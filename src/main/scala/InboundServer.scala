import ca.uhn.fhir.context.FhirContext
import handlers.InboundHandler
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.apache.pekko.io.{IO, Tcp}

import java.net.InetSocketAddress
import java.util.UUID

class InboundServer(host: String, port: Int)(using fhirCtx: FhirContext) extends Actor:

  import Tcp.*
  import context.system

  private val log = Logging(system, classOf[InboundServer])

  IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))

  def receive: Receive = {
    case b@Bound(localAddress) =>
      log.debug("received connection in interface{}", localAddress.toString)
      context.parent ! b

    case CommandFailed(_: Bind) =>
      context.stop(self)

    case Connected(remote: InetSocketAddress, _) =>
      val handler = context.actorOf(
        InboundHandler.props(sender(), remote)(using fhirCtx),
        remote.toString.replace("/", "").replace(".", "-").replace(":", "-").concat("-").concat(UUID.randomUUID().toString.toLowerCase)
      )
      log.debug(s"received connection from $remote, create handler ${handler.toString}")
      sender() ! Register(handler, keepOpenOnPeerClosed = false)
  }

object InboundServer:
  def props(host: String, port: Int)(using fhirCtx: FhirContext): Props =
    Props(new InboundServer(host, port))
    