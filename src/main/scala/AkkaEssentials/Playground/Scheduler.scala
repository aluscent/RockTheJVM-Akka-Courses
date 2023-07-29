package AkkaEssentials.Playground

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object Scheduler {
  class SimpleActor extends Actor with ActorLogging {
    var schedule = timeWindow()

    def timeWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }(context.dispatcher)
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info("Restarting actor.")

    override def postStop(): Unit = log.info("Stopping actor.")

    override def receive: Receive = {
      case "timeout" =>
        log.info("No message delivered.")
        context.stop(self)
      case message: String =>
        log.info(s"Message delivered: $message")
        schedule.cancel()
        schedule = timeWindow()
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("schedulerExercise")
    val simpleActor = system.actorOf(Props[SimpleActor])

    val routine = system.scheduler.scheduleOnce(600 millis){
      simpleActor ! "hello"
    }(system.dispatcher)
  }
}
