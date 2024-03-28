package message

import message.OutgoingMessage.UserId
import org.apache.pekko.http.scaladsl.model.ws

trait IncomingMessage


object IncomingMessage {
 
 def parse(text: String): Option[IncomingMessage] = {
   ???
 }
 
}

object OutgoingMessage  {
  type UserId = String
}

trait OutgoingMessage {
   def recipients: List[UserId]
   def toWsMessage: ws.Message
}