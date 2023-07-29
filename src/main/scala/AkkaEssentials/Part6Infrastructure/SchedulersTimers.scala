package AkkaEssentials.Part6Infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object Schedulers {
  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message: String => log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("scheduler")
    val simpleActor = system.actorOf(Props[SimpleActor])

    system.log.info("Scheduling reminder.")

    system.scheduler.scheduleOnce(2 second) {
      simpleActor ! "reminder"
    }(system.dispatcher)

    val rountine = system.scheduler.scheduleAtFixedRate(2 second, 5 second) {
      new Runnable {
        override def run(): Unit = simpleActor !"routine"
      }
    }(system.dispatcher)

    system.scheduler.scheduleOnce(13 second){
      rountine.cancel()
    }(system.dispatcher)
  }
}


object Timers {
  case object TimerKey
  case object Print
  case object Reminder
  case object Stop

  class SingleTimerActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Print, 2 second)

    override def receive: Receive = {
      case message: String => log.info(message)
      case Print => log.info("Printing single message.")
    }
  }

  class PeriodicTimerActor extends Actor with ActorLogging with Timers {
    timers.startTimerAtFixedRate(TimerKey, Print, 3 second)

    override def receive: Receive = {
      case message: String => log.info(message)
      case Print => log.info("Printing routine message.")
    }
  }

  class TimerActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Print, 2 second)

    override def receive: Receive = {
      case message: String => log.info(message)
      case Print =>
        log.info("Printing single message.")
        timers.startTimerAtFixedRate(TimerKey, Reminder, 1 second) // the timer with the same key overrides last running timer.
      case Reminder =>
        log.info("Printing periodic message.")
      case Stop =>
        log.info("Stopping timer.")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("timer")

    // val singleTimerActor = system.actorOf(Props[SingleTimerActor])
    // singleTimerActor ! "start reminder."

    // val periodicTimerActor = system.actorOf(Props[PeriodicTimerActor])
    // periodicTimerActor ! "start routine"

    val timerActor = system.actorOf(Props[TimerActor])
    timerActor ! "start timer"
  }
}