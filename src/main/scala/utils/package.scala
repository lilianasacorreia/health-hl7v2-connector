import ca.uhn.hl7v2.AcknowledgmentCode
import ca.uhn.hl7v2.model.AbstractMessage

package object utils {
  case class InboundMessage(
                             bundleId: String,
                             ackMsg: String,
                             msg: Option[String] = None,
                             triggerEvent: String,
                             actionCode: Option[String],
                             activityArea: Option[String],
                             sequentialNumber: String
                           )

  case class InternalErrorData(
                                exceptionId: String,
                                error: String,
                                exceptionAckMsg: String,
                                originalMsg: String,
                              )

  case class MessageData(
                          acknowledgmentCode: Option[AcknowledgmentCode],
                          messageEvent: String,
                          parsedData: AbstractMessage
                        )
}
