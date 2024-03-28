package actor.session

import actor.session.Lobby.{Join, LobbyMessage, UserManagerGreeting}
import actor.session.UserManager.{CreateSession, SessionMessage, UserSession}
import io.circe.Json
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}
import io.circe.Encoder.*
import message.{IncomingMessage, OutgoingMessage}

import java.time.Instant

object UserManager {
  case class UserSession(userId: String, sessionId: String, actorRef: ActorRef[Message])

  type SessionMessage = IncomingMessage | OutgoingMessage

  case class CreateSession(userId: String, actorRef: ActorRef[Message]) extends IncomingMessage

  object CreateSession {
    private val PREFIX = "LOGIN-"
    private val userIdRegex = s"$PREFIX(\\w+)".r

    def parseUserId(text: String): Option[String] = {
      userIdRegex.findFirstMatchIn(text).map(matched =>
        matched.group(1)
      )
    }
  }

  def create(lobby: ActorRef[LobbyMessage]): Behavior[SessionMessage] = {
    Behaviors.setup(context =>
      lobby ! UserManagerGreeting(context.self)
      UserManager(lobby).live(Map.empty)
    )
  }

}

case class UserManager(lobby: ActorRef[LobbyMessage]) {

  def live(onlineUsers: Map[String, UserSession]): Behavior[SessionMessage] = Behaviors.receiveMessagePartial {
    case CreateSession(userId, ref) =>
      // todo : check and override existing session
      val sessionId = s"$userId#${Instant.now.getEpochSecond}"
      val newSession = UserSession(userId, sessionId, ref)
      println(s"new session created $sessionId")
      val json = Json.obj(
        "sessionId" -> Json.fromString(sessionId),
        "onlineCount" -> Json.fromInt(onlineUsers.size)
      )
      lobby ! Join(userId)
      ref ! TextMessage.apply(json.toString)
      live(onlineUsers + (userId -> newSession))

    case out: OutgoingMessage =>
      out.recipients.foreach { recipient =>
        println(s"Sending out message to $recipient")
        onlineUsers.get(recipient) match
          case None =>
            println(s"Error: $recipient not found")
          case Some(userSession) =>
            userSession.actorRef ! out.toWsMessage
      }
      Behaviors.same

  }


}



