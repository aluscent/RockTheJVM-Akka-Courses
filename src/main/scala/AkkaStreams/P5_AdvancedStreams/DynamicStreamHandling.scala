package AkkaStreams.P5_AdvancedStreams

import akka.actor.ActorSystem
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Keep, MergeHub, Sink, Source}

import scala.concurrent.duration._
import scala.language.postfixOps

object DynamicStreamHandling {
  implicit val system = ActorSystem("dynamicStreamHandling")
  import system.dispatcher

  def main(args: Array[String]): Unit = {
    /// 1 - Kill switch
    val killSwitchFlow = KillSwitches.single[Int]
    val simpleSource = Source(LazyList.from(1)).throttle(1, 1 seconds).log("Source 1 generating.")
    val simpleSink = Sink.ignore

    //val killSwitch = simpleSource.viaMat(killSwitchFlow)(Keep.right).toMat(simpleSink)(Keep.left).run()
    //system.scheduler.scheduleOnce(5 seconds) { killSwitch.shutdown() }

    val anotherSimpleSource = Source(LazyList.from(100)).throttle(1, 1 seconds).log("Source 2 generating.")
    val sharedKillSwitch = KillSwitches.shared("oneButtonToRullThemAll")

    //simpleSource.via(sharedKillSwitch.flow).runWith(simpleSink)
    //anotherSimpleSource.via(sharedKillSwitch.flow).runWith(simpleSink)

    //system.scheduler.scheduleOnce(9 seconds) { sharedKillSwitch.shutdown() }

    /// Merge Hub
    val dynamicMerge = MergeHub.source[Int]
    val matValue = dynamicMerge.to(Sink.foreach[Int](println)).run()
    Source(1 to 10).runWith(matValue)

    /// Broadcast Hub
    val dynamicBroadcast = BroadcastHub.sink[Int]
    val matValue2 = Source(1 to 20).runWith(dynamicBroadcast)
    matValue2.runWith(Sink.foreach(println))
  }
}
