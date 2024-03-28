package actor

import actor.session.UserManager.{CreateSession, SessionMessage}
import actor.session.{Lobby, UserManager}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import org.apache.pekko.http.scaladsl.model.{AttributeKeys, HttpRequest, HttpResponse, Uri}
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import org.apache.pekko.stream.typed.scaladsl.ActorSource

import scala.concurrent.{ExecutionContextExecutor, Future}

object Root {

  private def requestHandler(userManagerRef: ActorRef[SessionMessage])(using actorSystem: ActorSystem[_], ec: ExecutionContextExecutor): HttpRequest => HttpResponse = {
    {
      case req@HttpRequest(GET, Uri.Path("/game"), _, _, _) =>
        req.attribute(AttributeKeys.webSocketUpgrade) match {
          case Some(upgrade) =>
            lazy val (actorRef, publisher) = ActorSource.actorRef[Message](
                completionMatcher = {
                  case b: BinaryMessage =>
                },
                bufferSize = 200,
                failureMatcher = PartialFunction.empty,
                overflowStrategy = OverflowStrategy.fail)
              .toMat(Sink.asPublisher[Message](fanout = false))(Keep.both).run()

            val inSink = Sink.foreach[Message] {
              case TextMessage.Strict(text) =>
                CreateSession.parseUserId(text) match
                  case None =>
                    println(s"Received message $text")
                  case Some(userId) =>
                    userManagerRef ! CreateSession(userId, actorRef)
              case b: BinaryMessage =>
                println(s"Received binary messages: $b")

            }
            upgrade.handleMessagesWithSinkSource(inSink, Source.fromPublisher(publisher))
          case None => HttpResponse(400, entity = "Not a valid websocket request!")
        }
    }
  }

  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      val lobby = context.spawn(Lobby(), "lobby")
      val userManager = context.spawn(UserManager.create(lobby), "user-manager")

      val rh = requestHandler(userManager)
      val serverSource = Http().newServerAt("localhost", 8080).connectionSource()
      val bindingFuture: Future[Http.ServerBinding] =
        serverSource.to(Sink.foreach { connection =>
          println("Accepted new connection from " + connection.remoteAddress)
          connection.handleWithSyncHandler(rh)
        }).run()

      Behaviors.receiveSignal{
        case (_, PostStop) =>
          println("PostStop Root")
          bindingFuture
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => system.terminate()) // and shutdown when done
          Behaviors.same
      }
    }
  }


}
