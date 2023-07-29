package AkkaStreams.P2_Primer

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FirstPrinciples {
  implicit val system = ActorSystem("streams")

  // Source
  val source = Source(1 to 10)
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(LazyList.from(1))
  val futureSource = Source.future(Future(42))
  val namesSource = Source("Spark is the best thing that ever happened and will ever happen until everything that can ever happened happened"
    .split(" ").toList)

  // Process
  val flow = Flow[Int].map(x => x + 1)
  val takerFlow = Flow[Int].take(5) // takes 5 elements and closes stream
  val dropFlow = Flow[Int].drop(5)
  val filterFlow = Flow[Int].filter(_ > 100)
  val takeFirst5Chars = Flow[String].map(x => x.substring(0,3))
  val capitalFlow = Flow[String].map(x => x.toUpperCase)
  val checkLengthFlow = Flow[String].filter(_.length > 3)

  // Sink
  val printSink = Sink.foreach(println)
  val blackhole = Sink.ignore
  val headSink = Sink.head // retrieves head and closes stream
  val foldSink = Sink.fold[Int, Int](0)((x, y) => x + y)

  def main(args: Array[String]): Unit = {
    val graph = source.to(printSink)
    //graph.run()

    val sourceWithFlow = source.via(flow)

    val flowToSink = flow.to(printSink)

    val graph2 = source.via(flow).to(printSink)
    //graph2.run()

    val source2 = Source(List(1,2,3)).map(x => x * 2)
    //source2.runForeach(println)

    namesSource.via(checkLengthFlow).via(takeFirst5Chars).via(capitalFlow).to(printSink).run()
  }
}