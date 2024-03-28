import actor.Root
import org.apache.pekko.actor.typed.ActorSystem

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem(Root(), "game-server")
  }
}