package AkkaStreams.P3_StreamsGraphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Balance, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs {
  implicit val system = ActorSystem("openGraphs")

  def main(args: Array[String]): Unit = {
    val source1 = Source(1 to 15)
    val source2 = Source(32 to 50)

    val sourceGraph = Source fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        val concat = builder.add(Concat[Int](2))
        import GraphDSL.Implicits._
        source1 ~> concat
        source2 ~> concat
        SourceShape(concat.out)
      }
    }

    //sourceGraph.runWith(Sink.foreach(println))

    val sink1 = Sink.foreach[Int](x => println(s"Sink 1: $x."))
    val sink2 = Sink.foreach[Int](x => println(s"Sink 2: $x."))

    val sinkGraph = Sink fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        val balance = builder.add(Balance[Int](2))
        import GraphDSL.Implicits._
        balance ~> sink1
        balance ~> sink2
        SinkShape(balance.in)
      }
    }

    Source(1 to 100).runWith(sinkGraph)

    val incrementer = Flow[Int] map (_ + 1)
    val multiplier = Flow[Int] map (_ * 2)

    val shapeGraph = Flow fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        val inc = builder.add(incrementer)
        val mul = builder.add(multiplier)
        import GraphDSL.Implicits._
        inc ~> mul
        FlowShape(inc.in, mul.out)
      } // static graph
    } // component

    def sourceSink[A,B](sink: Sink[A,_], source: Source[B,_]): Flow[A,B,_] = Flow fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        val in = builder.add(source)
        val out = builder.add(sink)
        FlowShape(out.in, in.out)
      }
    }
    val f = Flow.fromSinkAndSourceCoupled(Sink.foreach(println), Source(1 to 10))
  }
}
