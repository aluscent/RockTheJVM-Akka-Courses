package AkkaStreams.P3_StreamsGraphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}

object Intro {
  implicit val system = ActorSystem("graphs")

  def main(args: Array[String]): Unit = {
    val input = Source(1 to 1000)
    val incrementer = Flow[Int] map {
      Thread.sleep(1000)
      _ + 1
    }
    val multiplier = Flow[Int] map {
      Thread.sleep(1200)
      _ * 2
    }
    val output = Sink.foreach(println)

    // step 1 - setting fundamentals
    val graph = RunnableGraph fromGraph {
      GraphDSL.create()({ implicit builder: GraphDSL.Builder[NotUsed] => // shape
        import GraphDSL.Implicits._

        // step 2 - add necessary components to the graph
        val broadcast = builder.add(Broadcast[Int](2)) // fan-out: one input and 2 outputs
        val zip = builder.add(Zip[Int, Int]) // fan-in operator

        // step 3 - tying the components
        input ~> broadcast
        broadcast.out(0) ~> incrementer ~> zip.in0
        broadcast.out(1) ~> multiplier ~> zip.in1
        zip.out ~> output

        // step 4 - close the shape
        ClosedShape
      }) // inert static graph
    } // runnable graph

    graph.run()
  }
}
