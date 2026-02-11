import ca.uhn.fhir.context.FhirContext
import com.typesafe.config.Config
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.event.slf4j.Logger
import org.apache.pekko.stream.Materializer

object Server extends App:
  given system: ActorSystem = ActorSystem("Server")

  given materializer: Materializer = Materializer(system)

  given config: Config = system.settings.config

  given fhirCtx: FhirContext = FhirContext.forR5()

  val log = Logger("Server")
  val port = config.getInt("tcp.port")
  val host = config.getString("tcp.host")
  val inboundServer: ActorRef = system.actorOf(InboundServer.props(host,port), "HL7V2Server")

  log.info(s"✅ HL7v2 MLLP Server listening on TCP port $port")
