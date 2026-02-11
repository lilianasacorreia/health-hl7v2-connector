package operations

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.v25.message.ADT_A05
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.util.Timeout
import org.hl7.fhir.r5.model.Bundle
import org.slf4j.Logger as SLFLogger
import utils.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class PatientDemographicsOperations(using fhirCtx: FhirContext, val hl7System: ActorSystem, val logger: SLFLogger)
  extends Hl7v2ParseUtils
    with MessagingHelper
    with Hl7v2ToFhirUtils
    with BaseUtils {

  val parserVersion: String = hl7System.settings.config.getString("hl7.parser.version")
  val sinkHl7v25InboundTopic: String = hl7System.settings.config.getString("hl7v2Message.inboundFhirTransactions.topic")
  val sinkHl7v25InboundExceptionTopic: String = hl7System.settings.config.getString("hl7v2Message.inboundFhirTransactions.exceptions.topic")
  val bootstrapServers: String = hl7System.settings.config.getString("kafka.bootstrapservers")
  val producerSettings: ProducerSettings[String, String] = ProducerSettings(hl7System, new StringSerializer,
    new StringSerializer).withBootstrapServers(bootstrapServers)

  given timeout: Timeout = Timeout(5.seconds)

  def handleMessage(data: InboundMessage): Unit = {
    data.triggerEvent match {
      case "A28" =>
        logger.info("Handle New Patient")
        handleA28Message(data)
      case _ => logger.info(s"Going to ignore message type ${data.triggerEvent}")
    }
  }

  private def handleA28Message(data: InboundMessage): Unit = {
    Try {
      val message = parseAbstractMessage(data.msg.get, parserVersion)
      val parsedData = message.parsedData.asInstanceOf[ADT_A05]
      handleA28InboundMessageToFhir(parsedData)
    } match {
      case Success(_) =>
        logger.info(s"[${data.bundleId}] - Successfully processed ADT_A28.")
      case Failure(e) =>
        logger.error(s"[${data.bundleId}] - Failed processing ADT_A28. ${e.getMessage}")
    }
  }

  private def handleA28InboundMessageToFhir(data: ADT_A05): Unit = {
    val msgId: String = data.getMSH.getMessageControlID.toString
    logger.info(s"[$msgId] - Handling ADT^A28 inbound message with id")
    val bundlePatientNew: Bundle = handlePatientNew(data)
    val bundleString: String = parseFhirToString(bundlePatientNew)
    logger.info(s"[$msgId] - Going to publish a message for Bundle ID $msgId.")
    publishMessage(msgId, bundleString, sinkHl7v25InboundTopic, producerSettings)
  }
}
