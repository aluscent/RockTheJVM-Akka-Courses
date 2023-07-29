package AkkaEssentials.Part7AkkaPatterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashingMessages {
  case object Close
  case object Open
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash{
    override def receive: Receive = Closed

    def Closed: Receive = {
      case Open =>
        log.info("Opening resource.")
        unstashAll()
        context.become(Opened(""))
      case message =>
        log.info("Stashing message.")
        stash()
    }

    def Opened(state: String): Receive = {
      case Close =>
        log.info("Closing resource.")
        unstashAll()
        context.become(Closed)
      case Read =>
        log.info(s"Reading data: $state")
      case Write(data) =>
        log.info("Writing data.")
        context.become(Opened(data))
      case message: String =>
        log.info("Stashing message.")
        stash()
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("stashing")
    val resource = system.actorOf(Props[ResourceActor])

    resource ! Write("hello")
    resource ! Read
    resource ! Read
    Thread.sleep(1000)
    resource ! Open
    resource ! Write("hi")
    resource ! Read
    Thread.sleep(1000)
    resource ! Close
    Thread.sleep(1000)
    resource ! Open
  }
}
