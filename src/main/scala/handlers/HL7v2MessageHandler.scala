package handlers

import ca.uhn.fhir.context.FhirContext
import operations.OperationRegistry
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.util.Timeout
import org.slf4j.Logger as SLFLogger
import utils.{BaseUtils, Hl7v2ParseUtils, InboundMessage, MessagingHelper}

import scala.concurrent.duration.DurationInt

trait HL7v2MessageHandler
  extends Hl7v2ParseUtils
    with MessagingHelper
    with BaseUtils {

  implicit def hl7System: ActorSystem

  implicit def ctx: FhirContext

  implicit def logger: SLFLogger

  given parserVersion: String = hl7System.settings.config.getString("hl7.parser.version")

  given timeout: Timeout = Timeout(5.seconds)

  val sinkInboundFhirTransactionTopic: String = hl7System.settings.config.getString("hl7v2Message.inboundFhirTransactions.topic")
  val requestInTopic: String = hl7System.settings.config.getString("hl7v2Message.requestIn.topic")
  val bootstrapServers: String = hl7System.settings.config.getString("kafka.bootstrapservers")
  val producerSettings: ProducerSettings[String, String] = ProducerSettings(hl7System, new StringSerializer,
    new StringSerializer).withBootstrapServers(bootstrapServers)

  def handle(data: InboundMessage): Unit = {
    val operationRegistry = new OperationRegistry()
    data.activityArea match {
      case _ =>
        operationRegistry.patientDemographicsOperations.handleMessage(data)
    }
  }
}
