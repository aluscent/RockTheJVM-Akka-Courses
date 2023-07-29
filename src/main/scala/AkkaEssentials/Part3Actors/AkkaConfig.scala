package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaConfig {
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val path = "D:\\Job\\CodeDev\\IdeaProjects\\Rock-the-JVM_Advanced-Scala\\src\\main\\scala\\AkkaEssentials\\Actors\\AkkaConfig.conf"

    val file = new java.io.File(path)

    val config = ConfigFactory
      .parseFile(file)

    val system = ActorSystem("AkkaConfig", ConfigFactory.load(config))
    val actor = system.actorOf(Props[ActorWithLogging])

    actor ! "this is a message from user."
  }
}
