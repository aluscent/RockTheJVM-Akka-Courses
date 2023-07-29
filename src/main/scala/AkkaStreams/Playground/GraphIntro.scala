package AkkaStreams.Playground

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Balance, Broadcast, GraphDSL, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.duration._
import scala.language.postfixOps

object GraphIntro1 {
  implicit val system = ActorSystem("graphIntro1")

  def main(args: Array[String]): Unit = {
    val input = Source(1 to 100)
    val output1 = Sink.foreach[Int](x => println(s"Sink 1: $x"))
    val output2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))

    val graph = RunnableGraph fromGraph({
      GraphDSL.create()({ implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Int](2))

        input ~> broadcast ~> output1
        broadcast ~> output2

        ClosedShape
      })
    })

    graph.run()
  }
}


object GraphIntro2 {
  implicit val system = ActorSystem("graphIntro2")

  def main(args: Array[String]): Unit = {
    val input1 = Source(100 to 200).throttle(10, 4 second)
    val input2 = Source(200 to 400).throttle(20, 4 second)
    val output1 = Sink.foreach[Int](x => println(s"Sink 1: $x"))
    val output2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))

    val graph = RunnableGraph fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val merge = builder.add(Merge[Int](2))
        val balance = builder.add(Balance[Int](2))

        input1 ~> merge
        input2 ~> merge
        merge ~> balance
        balance ~> output1
        balance ~> output2

        ClosedShape
      }
    }

    graph.run()
  }
}