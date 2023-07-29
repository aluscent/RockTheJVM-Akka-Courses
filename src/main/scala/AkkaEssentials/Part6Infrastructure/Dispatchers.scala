package AkkaEssentials.Part6Infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.util.Random

object Dispatchers {
  class Dispatcher extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message: String =>
        count += 1
        log.info(s"message count is: $count")
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("dispatchers")
    val counters = (1 to 10).toList.map(x =>
      system.actorOf(Props[Dispatcher].withDispatcher("my-dispatcher"), s"counter_${x}"))

    val r = new Random()
    (1 to 1000).toList.foreach(x => counters(r.nextInt(10)) ! s"msg $x")

    val dispatcher2 = system.actorOf(Props[Dispatcher], "dispatcher2")
  }
}
