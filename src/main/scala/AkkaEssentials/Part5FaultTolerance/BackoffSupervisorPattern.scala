package AkkaEssentials.Part5FaultTolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import scala.concurrent.duration._
import java.io.File
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern {
  case object ReadFile

  class FileBasedPersistent extends Actor with ActorLogging {
    var source: Source = null

    override def preStart(): Unit = log.info("I am starting.")

    override def postStop(): Unit = log.info("I have stopped.")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"Supervised actor restarting because of: ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info(s"Supervised actor restarted.")

    override def receive: Receive = {
      case ReadFile =>
        if (source == null) source = Source.fromFile(new File("src/main/scala/AkkaEssentials/sample1.txt"))
        log.info("File read. length: " + source.length)
    }
  }


  def main(args: Array[String]): Unit = {
    val system = ActorSystem("backoff")
    val persistentActor = system.actorOf(Props[FileBasedPersistent], "simplePersistent")

    // persistentActor ! ReadFile

    val simpleSupervisorProps = BackoffSupervisor.props {
      BackoffOpts.onFailure(Props[FileBasedPersistent], "simpleBackoff", 3 seconds, 10 seconds, 0.1)
    }
    val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")

    // simpleBackoffSupervisor ! ReadFile

    val stopSupervisorProps = BackoffSupervisor.props {
      BackoffOpts.onStop(Props[FileBasedPersistent], "stopBackoff", 3 seconds, 10 seconds, 0.1)
        .withSupervisorStrategy(OneForOneStrategy() {
          case _ => Stop
        })
    }
    val stopBackoffSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")

    stopBackoffSupervisor ! ReadFile

    // if the actor encounters an exception, the back-off will restart it.
    // for a file access, it can retry until the file becomes available.
    // it also has a random seed, so all the actors will retry with time-differences and won't bring down the database.
  }
}
