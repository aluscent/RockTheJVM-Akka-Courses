package AkkaStreams.P3_StreamsGraphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.ZipWith
import akka.stream.{ClosedShape, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}

import scala.util.Random

object ComplexOpenGraphs {
  implicit val system = ActorSystem("complexOpenGraphs")

  def main(args: Array[String]): Unit = {
    val random = new Random()
    val source1 = Source(for (_ <- 1 to 20) yield random.between(25,55))
    val source2 = Source(for (_ <- 1 to 20) yield random.between(25,55))
    val source3 = Source(for (_ <- 1 to 20) yield random.between(25,55))

    val graph = Source fromGraph {
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        val in1 = builder.add(source1)
        val in2 = builder.add(source2)
        val in3 = builder.add(source3)
        import GraphDSL.Implicits._
        val zip1 = builder.add(ZipWith.create[Int,Int,Int]((x, y) => Math.max(x, y)))
        val zip2 = builder.add(ZipWith.create[Int,Int,Int]((x, y) => Math.max(x, y)))

        in1 ~> zip1.in0
        in2 ~> zip1.in1
        zip1.out ~> zip2.in0
        in3 ~> zip2.in1
        SourceShape(zip2.out)
      }
    }

    graph.runWith(Sink.foreach(println))
  }
}
