package AkkaStreams.P2_Primer

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MaterializingStreams {
  implicit val system = ActorSystem("materializingStreams")

  def main(args: Array[String]): Unit = {
    val source1 = Source(1 to 10)

    val flow1 = source1.to(Sink.foreach(println))
    //val simpleMaterializedValue = flow1.run()

    val sink1 = Sink.reduce[Int]((x,y) => x + y)
    val sumFuture = source1.runWith(sink1)
    sumFuture.onComplete {
      case Success(value) => println(f"Sum of all numbers is $value.")
      case Failure(exception) => println(s"No sum: $exception")
    }

    val sumFuture2 = source1.toMat(sink1)((x, y) => x)

    val future1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
    future1 onComplete {
      case Success(value) => println(f"value is $value.")
      case _ =>
    }


  }
}
