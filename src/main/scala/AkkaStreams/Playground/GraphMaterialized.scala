package AkkaStreams.Playground

import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object GraphMaterialized {
  implicit val system = ActorSystem("graphMaterialized")

  def EnhancedFlow[A,B](flow: Flow[A,B,_]): Flow[A,B,Future[Int]] = {
    val counter = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow fromGraph {
      GraphDSL.createGraph(counter) { implicit builder => counterShape =>
        val broadcast = builder.add(Broadcast[B](2))
        val flowShape = builder.add(flow)
        import GraphDSL.Implicits._
        flowShape ~> broadcast ~> counterShape
        FlowShape(flowShape.in, broadcast.out(1))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val source1 = Source(43 to 90)

    val special = source1.viaMat(EnhancedFlow(Flow[Int].map(x => x + 3)))(Keep.right)
      .toMat(Sink.foreach(println))(Keep.left).run()

    special.onComplete {
      case Success(value) => println(s"The count is: $value.")
    }
  }
}
