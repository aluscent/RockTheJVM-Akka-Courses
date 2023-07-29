package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object LoggingActors {
  class SimpleActorExplicitLogger extends Actor {
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message: String => logger.info(message)
    }
  }

  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("loggingActor")
    val actor = system.actorOf(Props[SimpleActorExplicitLogger])

    actor ! "hello to yall!"
  }
}
