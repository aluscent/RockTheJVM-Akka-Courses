package AkkaStreams.P5_AdvancedStreams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object Substreams {
  implicit val system = ActorSystem("substreams")
  import system.dispatcher

  def main(args: Array[String]): Unit = {
    // 1 - grouping streams using a function
    val source1 = Source("akka is awesome because it is scala".split(" "))
    val groups = source1.groupBy(32, word => word.toLowerCase.charAt(0))

    groups.to(Sink.fold(0)((count,word) => {
      val c = count + 1
      println(s"just received '$word', total count is $c.")
      c
    }))//.run()

    // 2 - merge substreams back
    val source2 = Source("akka is awesome*scala is wonderful*this is amazing*learning from rock the jvm".split('*'))
    val groups2 = source2.groupBy(2, sen => sen.length % 2).map(_.length)
      .mergeSubstreamsWithParallelism(2).toMat(Sink.reduce[Int](_ + _))(Keep.right).run()

    groups2 onComplete {
      case Success(value) => println(s"Total char count: $value.")
      case Failure(exception) =>
    }

    // 3 - splitting streams into substreams, on a condition
    val source3 = Source("akka is awesome*scala is wonderful*this is amazing*learning from rock the jvm".split('*'))
      .splitWhen(ch => ch == " ")


    // 4 - flattening
    val source4 = Source(1 to 14)
    val multiSource = source4.flatMapConcat(x => Source(x to (2 * x))).runWith(Sink.foreach(println))
    val multiMerge = source4.flatMapMerge(2, x => Source(x to (2 * x))).runWith(Sink.foreach(println))
  }
}
