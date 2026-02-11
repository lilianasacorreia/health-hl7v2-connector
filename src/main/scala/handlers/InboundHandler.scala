package handlers

import ca.uhn.fhir.context.FhirContext
import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.io.Tcp
import org.apache.pekko.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import utils.{InboundMessage, InternalErrorData}

import java.net.InetSocketAddress
import java.util.UUID

/**
 * An actor responsible for handling inbound HL7 messages.
 *
 * @param connection The actor reference for the connection
 * @param remote     The remote address
 *
 *                   https://www.rapidtables.com/code/text/ascii-table.html
 *                   https://www.hl7.org/documentcenter/public/wg/inm/mllp_transport_specification.PDF
 *                   \u000B -> 0x0B -> 11	0B	00001011	&#11;	VT	Vertical Tab
 *                   \u001C -> 0x1C -> 28	1C	00011100	&#28;	FS	File Separator
 *                   \r     -> 0x0D -> 13	0D	00001101	&#13;	CR	Carriage Return
 */
class InboundHandler(
                      connection: ActorRef,
                      remote: InetSocketAddress,
                    )(using fhirCtx: FhirContext)
  extends Actor
    with HL7v2MessageHandler {

  import Tcp.*

  //TODO: validate use of implicit def in scala 3
  given hl7System: ActorSystem = context.system

  given logger: Logger = LoggerFactory.getLogger("InboundHandler")

  given ctx: FhirContext = fhirCtx

  lazy val auditUUID: String = UUID.randomUUID().toString.toLowerCase()

  val maxStored = 100000000L
  val highWatermark: Long = maxStored * 5 / 10
  val lowWatermark: Long = maxStored * 3 / 10
  var storage = Vector.empty[ByteString]
  var stored = 0L
  var transferred = 0L
  var closing = false

  /**
   * sign death pact: this actor terminates when connection breaks
   * */

  context.watch(connection)
  var suspended = false

  def receive: Receive = {
    case Received(data) =>
      logger.info(s"Received ${data.size} bytes")
      data.headOption match {
        case None =>
          logger.warn(s"empty payload from $remote")
        case Some(b) if b != 0x0B.toByte =>
          logger.error(s"Header NOK from $remote (no SB 0x0B)")
          closing = true
          context.stop(self)
        case Some(_) =>
          //TODO: add log message confidential level
          val cleanData = data.filterNot(b => b == 0x0B.toByte || b == 0x1C.toByte)
          val message = s"Message received:\n${cleanData.utf8String.replace("\r", "\n")}"
          logger.info(message)
          buffer(cleanData)
          ackIfCompleted(data)
      }

    case PeerClosed =>
      context.stop(self)
  }

  private def buffer(cleanData: ByteString): Unit = {
    storage :+= cleanData
    stored += cleanData.size
    if (stored > maxStored) {
      logger.warn(s"drop connection to [$remote] (buffer overrun)")
      context.stop(self)
    } else if (stored > highWatermark) {
      logger.debug(s"suspending reading")
      connection ! SuspendReading
      suspended = true
    }
  }

  private def ackIfCompleted(data: ByteString): Unit = {
    if (data.contains(0x1C)) {
      val message = replaceEscapedHexChars(ByteString(storage.flatten.toArray).utf8String, "ISO-8859-1")
      val isAckMsg = isAckMessage(message)
      val hl7Message = parseMessage(message, parserVersion)
      lazy val exceptionId = UUID.randomUUID().toString.toLowerCase()

      hl7Message match {
        case Left(errorData@InternalErrorData(exceptionId, "parseException", _, _)) =>
          val error = errorData.exceptionId
          logger.info(s"Message with error: ${errorData.exceptionAckMsg}")
          logger.error(s"Exception $error parsing $message")
          publishAndSendExternalAckMessage(errorData.exceptionId, errorData.exceptionAckMsg, isAckMsg)
        case Left(errorData@InternalErrorData(exceptionId, "notSupported", _, _)) =>
          val error = errorData.error
          logger.error(s"Exception $error parsing $message")
          publishAndSendExternalAckMessage(errorData.exceptionId, errorData.originalMsg, isAckMsg)
        case Right(data) =>
          handleRighDataReceived(isAckMsg, data)
      }
    }
  }

  private def handleRighDataReceived(isAckMessage: Boolean, data: InboundMessage): Unit = {
    logger.info(s"Received message with ID: ${data.bundleId} with ${storage.flatten.toArray.length} bytes")
    if (isAckMessage) {
      data.triggerEvent match {
        case evt =>
          logger.warn(s"Not supported event: $evt. ACK Message = $data")
          closing = true
          context.stop(self)
      }
    } else {
      publishMessage(data.bundleId, data.msg.get, requestInTopic, producerSettings)
      publishAndSendExternalAckMessage(data.bundleId, data.ackMsg, isAckMessage)
      logger.info(s"Going to handle message type ${data.triggerEvent}")
      handle(data)
    }
  }

  private def publishAndSendExternalAckMessage(
                                                id: String,
                                                ack: String,
                                                isAckMessage: Boolean
                                              ): Unit = {
    if (!isAckMessage) {
      publishMessage(id, ack, requestInTopic, producerSettings)
      logger.info(s"Message ack:\n${ack.replace("\r", "\n")}")
      connection ! Write(ByteString('\u000B' + ack + '\u001C' + '\r'), Ack)
      context.become({
        case Received(data) =>
          buffer(data)
          ackIfCompleted(data)
        case Ack =>
          acknowledge()
          closing = true
          context.stop(self)
        case PeerClosed => closing = true
      }, discardOld = false)
    }
  }

  private def acknowledge(): Unit = {
    require(storage.nonEmpty, "storage was empty")
    val size = storage(0).size
    stored -= size
    transferred += size
    storage = storage.drop(1)
    if (suspended && stored < lowWatermark) {
      logger.debug("resuming reading")
      connection ! ResumeReading
      suspended = false
    }
    if (storage.isEmpty) {
      if (closing) context.stop(self)
      else context.unbecome()
    } else connection ! Write(storage(0), Ack)
  }

  case object Ack extends Event

}

object InboundHandler:
  def props(conn: ActorRef, remote: InetSocketAddress)(using FhirContext): Props =
    Props(new InboundHandler(conn, remote))
