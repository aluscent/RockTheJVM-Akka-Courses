package AkkaRemoting.P2_Remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorSystem, Identify, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object E5_RemoteActors {
  object RemoteActors_Local {
    case object State

    val localSystem = ActorSystem("LocalActors", ConfigFactory.load("AkkaRemoting/Part2Remoting/application.conf"))

    val localSimpleActor = localSystem.actorOf(Props[SimpleActor], "lo-si-1")

    def main(args: Array[String]): Unit = {

      /**
       * To send a message to remote actor on the next JVM:
       * Method 1: Actor selection
       * Method 2: Resolve actor selection to an actor-ref
       * Method 3: Actor identification via messages
       */

      // Method 1
      val remoteActorSelection = localSystem.actorSelection("akka://RemoteActors@localhost:2552/user/re-si-1")
      //    remoteActorSelection ! "hello to remote actor!"

      // Method 2
      import localSystem.dispatcher
      implicit val timeout = Timeout(2 second)
      //    val remoteActorRefFuture = remoteActorSelection.resolveOne()
      //    remoteActorRefFuture onComplete {
      //      case Success(actorRef) => actorRef ! "I've resolved you in a future!"
      //      case Failure(exception) => println(s"Failed due to: $exception")
      //    }

      // Method 3
      // - Actor resolver will ask for a actor selection from the local actor system.
      // - Actor resolver will send a Identify(value) to the actor selection.
      // - The remote actor will automatically respond with ActorIdentity(value, actorRef).
      // - Now the actor resolver can use the actorRef.
      class ActorResolver extends Actor with ActorLogging {
        override def preStart(): Unit = {
          val selected = context.actorSelection("akka://RemoteActors@localhost:2552/user/re-si-1")
          selected ! Identify(UUID.randomUUID().toString)
        }

        override def receive: Receive = {
          case ActorIdentity(id, Some(actor)) =>
            log.info(s"[ActorResolver] Identified actor $id.")
            actor ! "Thank you!"
          case State =>
            log.info("[ActorResolver] Actor up and running.")
        }
      }

      val resolver = localSystem.actorOf(Props[ActorResolver])
      resolver ! State
    }
  }

  object RemoteActors_Remote {
    val remoteSystem = ActorSystem("RemoteActors", ConfigFactory.load("AkkaRemoting/Part2Remoting/application.conf").getConfig("remoteSystem"))

    val remoteSimpleActor = remoteSystem.actorOf(Props[SimpleActor], "re-si-1")

    def main(args: Array[String]): Unit = {

    }
  }
}