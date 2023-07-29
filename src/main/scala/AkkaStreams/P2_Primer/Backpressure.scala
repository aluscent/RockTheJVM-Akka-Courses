package AkkaStreams.P2_Primer

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._
import scala.language.postfixOps

object Backpressure {
  def Sigmoid(value: Double) = ((1 / (1 + math.exp(-value))) * 1000).toInt

  implicit val system = ActorSystem("backpressure")

  def main(args: Array[String]): Unit = {
    val fastSource = Source(1 to 100)
    val slowSink = Sink foreach[Int] { x =>
      Thread.sleep(100)
      println(s"Sink $x.\t        @ ${java.time.LocalTime.now()}")
    }
    val fastFlow = Flow[Int] map { x =>
      println(s"New flow value: $x.   @ ${java.time.LocalTime.now()}")
      x
    }

    //fastSource.to(slowSink).run()
    //fastSource.async.to(slowSink).run()
    //fastSource.async.via(fastFlow).async.to(slowSink).run()

    // reactions to backpressure:
    //  1. slowdown upstream if possible
    //  2. buffer elements as long as possible
    //  3. drop the elements from buffer if overflows

    val bufferedFlow = fastFlow.buffer(5, OverflowStrategy.dropTail)
    //fastSource.async.via(bufferedFlow).async.to(slowSink).run()

    fastSource.throttle(6, 1 second).async.via(bufferedFlow).async.to(slowSink).run()
  }
}
