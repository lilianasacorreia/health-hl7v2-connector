package utils

import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.slf4j.Logger as SLFLogger

import scala.concurrent.{ExecutionContextExecutor, Future}

trait MessagingHelper {

  implicit def hl7System: ActorSystem

  implicit lazy val config: Config = hl7System.settings.config
  implicit val mat: Materializer = Materializer(hl7System)
  implicit val ec: ExecutionContextExecutor = hl7System.dispatcher
  implicit val scheduler: Scheduler = hl7System.scheduler

  def publishMessage(identifier: String, message: String, topic: String, producerSettings: ProducerSettings[String, String])(using logger: SLFLogger): Future[Done] = {
    Source(List(message))
      .map(value => new ProducerRecord[String, String](topic, null, identifier, value))
      .runWith(Producer.plainSink(producerSettings))
      .map { done =>
        logger.info(s"Successfully published message to topic '$topic' for Bundle ID '$identifier'")
        done
      }
  }
}
