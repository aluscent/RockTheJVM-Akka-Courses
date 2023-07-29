package AkkaStreams.P2_Primer

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion {
  implicit val system = ActorSystem("operatorFusion")

  def main(args: Array[String]): Unit = {
    val simpleSource = Source(1 to 1000)
    val simpleFlow = Flow[Int].map(_ + 1)
    val simpleFlow2 = Flow[Int].map(_ * 10)
    val simpleSink = Sink.foreach[Int](println)

    //  This stream runs on the same component
    //simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
    //  This is operator/component fusion

    val longFlow = Flow[Int] map {
      Thread.sleep(1500)
      _ + 1
    }
    //simpleSource.via(longFlow).to(simpleSink).run() // here the process takes much longer

    simpleSource.via(longFlow).async.to(simpleSink).run()
  }
}
