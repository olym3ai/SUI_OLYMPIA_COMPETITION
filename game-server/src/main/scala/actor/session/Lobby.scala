package actor.session

import actor.session.UserManager.SessionMessage
import message.OutgoingMessage
import message.OutgoingMessage.UserId
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}

object Lobby {
  
  sealed trait LobbyMessage
  
  case class Join(userId: String) extends LobbyMessage
  case class UserManagerGreeting(actorRef: ActorRef[SessionMessage]) extends LobbyMessage
  
  private case class NotifyUserJoin(users: List[User]) extends OutgoingMessage{
    override def toWsMessage: Message = TextMessage.Strict("HAHAHHA")

    override def recipients: List[UserId] = users.map(_.userId)
  }
  
  case class User(userId: String)
  
  case class Data(users: List[User]){
    def joined(userId: String): Data = {
      this.copy(users = this.users :+ User(userId))
    }
  }
  
  def apply(): Behavior[LobbyMessage] = {
     postStart()
  }

  private def live(data: Data, session: ActorRef[SessionMessage]): Behavior[LobbyMessage] = Behaviors.receiveMessagePartial {
    case Join(userId) =>
      println(s"User $userId joined Lobby")
      session ! NotifyUserJoin(data.users)
      live(data.joined(userId), session)
  }

  private def postStart(): Behavior[LobbyMessage] = Behaviors.receiveMessagePartial {
    case UserManagerGreeting(actorRef) =>
      println(s"Greeting from UserManager")
      live(Data(Nil), actorRef)
  }
}