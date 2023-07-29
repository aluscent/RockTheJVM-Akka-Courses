package AkkaEssentials.Part5FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle {
  case object StartChild
  case object Fail
  case object FailChild

  class LifecycleActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("I am starting.")

    override def postStop(): Unit = log.info("I have stopped.")

    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifecycleActor], "child")
    }
  }


  class Parent extends Actor with ActorLogging {
    private val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild =>
        child ! Fail
    }
  }
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("I am starting.")
    override def postStop(): Unit = log.info("I have stopped.")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"Supervised actor restarting because of: ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info(s"Supervised actor restarted.")

    override def receive: Receive = {
      case Fail =>
        log.warning("Child will fail now.")
        throw new RuntimeException("I failed.") // because of this exception, the actor will be restarted.
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("actorLifeCycle")

    /*
    val parent = system.actorOf(Props[LifecycleActor], "parent")
    parent ! StartChild
    parent ! PoisonPill
     */

    val supervisor = system.actorOf(Props[Parent], "parent")
    supervisor ! FailChild
  }
}
