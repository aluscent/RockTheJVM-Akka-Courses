package AkkaStreams.P3_StreamsGraphs

import akka.actor.ActorSystem
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterialized {
  implicit val system = ActorSystem("graphMaterialized")

  def main(args: Array[String]): Unit = {
    val wordSource = Source("akka is Awesome and rock the JVM".split(" "))
    val printer = Sink.foreach[String](x => if (x == x.toLowerCase) println(x))
    val shortCounter = Sink.fold[Int,String](0)((count,string) => if (string.length >= 4) count + 1 else count)
    val counter = Sink.fold[Int,String](0)((count, _) => count + 1)

    val graph: Sink[String, Future[Int]] = Sink fromGraph{
      GraphDSL.createGraph(shortCounter, counter) ((shortCounterMatVal, counterMatVal) => shortCounterMatVal)
      { implicit builder => (shortCounterShape, counterShape) =>
        val broadcast = builder.add(Broadcast[String](3))
        import GraphDSL.Implicits._
        broadcast ~> printer
        broadcast ~> shortCounterShape
        broadcast ~> counterShape
        SinkShape(broadcast.in)
      }
    }

    wordSource.runWith(graph).onComplete {
      case Success(value) => println(s"Count of words: $value.")
      case Failure(exception) => println(s"Count failed: $exception")
    }
  }
}
