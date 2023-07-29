package AkkaStreams.Playground

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}

object MergeBroadcastHub {
  implicit val system = ActorSystem("mergeHubBroadcastHub")

  def main(args: Array[String]): Unit = {
    val merge = MergeHub.source[Int]
    val broadcast = BroadcastHub.sink[Int]

    val source1 = Source(1 to 10)
    val source2 = Source(60 to 70)

    val sink1 = Sink.foreach[Int](x => println(s"Sink 1: $x"))
    val sink2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))

    val dynamicMergeBroadcast = merge.toMat(broadcast)(Keep.both)
    val (publisher, subscriber) = dynamicMergeBroadcast.run()
    source1.to(publisher).run()
    source2.to(publisher).run()
    subscriber.to(sink1).run()
    subscriber.to(sink2).run()
  }
}
