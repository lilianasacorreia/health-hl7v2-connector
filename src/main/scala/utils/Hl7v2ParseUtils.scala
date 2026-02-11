package utils

import ca.uhn.hl7v2.model.v25.datatype.MSG
import ca.uhn.hl7v2.model.v25.message.{ACK, ADT_A05}
import ca.uhn.hl7v2.model.v25.segment.MSH
import ca.uhn.hl7v2.model.{GenericMessage, Message}
import ca.uhn.hl7v2.parser.{CanonicalModelClassFactory, PipeParser}
import ca.uhn.hl7v2.util.idgenerator.InMemoryIDGenerator
import ca.uhn.hl7v2.{AcknowledgmentCode, DefaultHapiContext, HL7Exception}
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import utils.BaseUtils.removeSpecialCharacters

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait Hl7v2ParseUtils {
  implicit def hl7System: ActorSystem

  lazy val exceptionId = UUID.randomUUID().toString.toLowerCase()
  private val mshString = "MSH"

  def parseMessage(data: String, parserVersion: String)(implicit config: Config): Either[InternalErrorData, InboundMessage] = {
    val parser: PipeParser = createParser(parserVersion)
    decode(parser, data)
  }

  def createParser(parserVersion: String): PipeParser = {
    val hapiContext = new DefaultHapiContext()
    hapiContext.getParserConfiguration.setIdGenerator(new InMemoryIDGenerator)
    hapiContext.setModelClassFactory(new CanonicalModelClassFactory(parserVersion))
    val parser: PipeParser = hapiContext.getPipeParser
    parser.getParserConfiguration.setAllowUnknownVersions(true)
    parser.getParserConfiguration.setValidating(true)
    parser
  }

  private def decode(parser: PipeParser, originalMsg: String)(implicit config: Config): Either[InternalErrorData, InboundMessage] = {
    Try(parser.parse(originalMsg)) match {
      case Success(m) =>
        m match
          case msg if msg.isInstanceOf[GenericMessage] => {
            val event = msg.get("MSH").asInstanceOf[MSH].getMsh9_MessageType.getTriggerEvent
            Left(InternalErrorData(exceptionId,
              "parseException",
              exceptionAck(parser, s"Unknown event ${event}", originalMsg),
              removeSpecialCharacters(originalMsg)
            ))
          }
          case ack: ca.uhn.hl7v2.model.v25.message.ACK => Right(newInbMsgForV25(ack, None))
          //case msg: Message if !enabledFeature(config, msg) => generateAck(originalMsg, parser, msg, AcknowledgmentCode.CR, UNSUPPORTED_MESSAGE_TYPE.getCode.toString)
          case msg: Message => generateAck(originalMsg, parser, msg, AcknowledgmentCode.CA, null)
          case _ => Left(InternalErrorData(exceptionId,
            "notSupported",
            exceptionAck(parser, "message format not supported", originalMsg),
            removeSpecialCharacters(originalMsg)
          ))

      case Failure(e) => Left(InternalErrorData(exceptionId,
        "parseException",
        exceptionAck(parser, e.getMessage, originalMsg),
        removeSpecialCharacters(originalMsg)
      ))
    }
  }

  private def generateAck(originalMsg: String, parser: PipeParser, m: Message, ackCode: AcknowledgmentCode, errorCode: String): Either[InternalErrorData, InboundMessage] = {

    val exception: HL7Exception = errorCode match {
      case code: String =>
        val exception = new HL7Exception("Message rejected. Event is disabled.")
        exception.setErrorCode(code.toInt)
        exception
      case _ => null
    }

    m.generateACK(ackCode, exception) match {
      case ack: ca.uhn.hl7v2.model.v25.message.ACK => Right(newInbMsgForV25(ack, Some(m.encode())))
      case _ => Left(InternalErrorData(exceptionId,
        "notSupported",
        exceptionAck(parser, "message format not supported", originalMsg),
        removeSpecialCharacters(originalMsg)
      ))
    }
  }

  private def exceptionAck(parser: PipeParser, errorString: String, originalMsg: String): String = {
    val tmpack: Array[String] = originalMsg.split("\r").filter((_: String).contains(mshString)).map((headerMsg: String) => {
      Try(parser.parse(headerMsg)) match {
        case Success(m) => m.generateACK(AcknowledgmentCode.CE, new HL7Exception(errorString)).encode()
        case Failure(e) => s"header parseException: ${e.getMessage}"
      }
    }
    )
    tmpack(0)
  }

  private def newInbMsgForV25(ack: ca.uhn.hl7v2.model.v25.message.ACK, message: Option[String]): InboundMessage = {
    InboundMessage(
      bundleId = ack.getMSA.getMessageControlID.getValue,
      ackMsg = ack.encode(),
      msg = message,
      triggerEvent = ack.getMSH.getMessageType.getTriggerEvent.getValue,
      actionCode = getOriginalMessageField(message, "EVN", 4),
      activityArea = getOriginalMessageField(message, "PV1", 2),
      sequentialNumber = ack.getMSA.getExpectedSequenceNumber.getValue
    )
  }

  private def getMsgType(msg: Message): String = {
    val msgType: MSG = msg.get(mshString).asInstanceOf[MSH].getMsh9_MessageType
    s"${msgType.getMsg1_MessageCode}^${msgType.getMsg2_TriggerEvent}"
  }

  private def getOriginalMessageField(originalMsg: Option[String], segment: String, fieldIndex: Int): Option[String] =
    originalMsg
      .flatMap(_.split("\r").find(_.startsWith(segment + "|")))
      .map(_.split('|'))
      .filter(_.isDefinedAt(fieldIndex))
      .map(_(fieldIndex))

  def parseAbstractMessage(message: String, parseVersion: String)(using config: Config): MessageData = {
    val parser: PipeParser = createParser(parseVersion)
    Try(parser.parse(message)) match {
      case Success(msg) =>
        msg match {
          case ack: ca.uhn.hl7v2.model.v25.message.ACK =>
            MessageData(Some(AcknowledgmentCode.valueOf(ack.getMSA.getAcknowledgmentCode.getValue)), getMsgType(msg), ack)
          case adta05: ADT_A05 => MessageData(None, getMsgType(msg), adta05)
          case ack: ACK =>
            throw new RuntimeException(s"Message format not supported $ack. ")
          case _ =>
            throw new RuntimeException(s"Message format not supported. ")
        }
      case Failure(e) =>
        throw new RuntimeException(s"Error on parse message ${removeSpecialCharacters(message)}. Ex.: ${e.getMessage}")
    }
  }
}
