package AkkaStreams.P4_TechniquesPatterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object ActorIntegration {
  implicit val system = ActorSystem("actorIntegration")

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case string: String =>
        log.info(s"Received message $string")
        sender() ! s"[Received] $string"
      case int: Int =>
        log.info(s"Received integer $int")
        sender() ! 100000 + int
      case _ =>
    }
  }

  // Actor as destination
  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(x: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized.")
        sender() ! StreamAck
      case StreamComplete =>
        context.stop(self)
        log.info("String completed.")
      case StreamFail(ex) =>
        log.warning(s"Stream failed: $ex")
      case message =>
        log.info(s"Message received: $message")
        sender() ! StreamAck
    }
  }

  def main(args: Array[String]): Unit = {
    val actor = system.actorOf(Props[SimpleActor], "simpleActor")
    val numberSource = Source(1 to 10)
    implicit val timeout: Timeout = Timeout(2 seconds)
    val actorBasedFlow = Flow[Int].ask[Int](4)(actor)

    //numberSource.via(actorBasedFlow).to(Sink.foreach[Int](println))
    //numberSource.ask[Int](4)(actor).to(Sink.foreach[Int](println)).run()

    val actorPoweredSource = Source.actorRef[Int](10, OverflowStrategy.dropHead)
    val matActor = actorPoweredSource.to(Sink.foreach[Int](println)).run()
    //matActor ! 10
    // terminating
    //matActor ! akka.actor.Status.Success("complete")

    // Actor as a destination/sink
    //  1. An init message
    //  2. An acknowledge message to confirm reception
    //  3. A complete messsage
    //  4. Exception handler function

    val destinationActor = system.actorOf(Props[DestinationActor], "dest")
    val actorPoweredSink = Sink.actorRefWithBackpressure[Int](
      destinationActor,
      StreamInit,
      StreamComplete,
      StreamAck,
      throwable => StreamFail(throwable) // optional bc has a default value
    )

    Source(1 to 10).to(actorPoweredSink).run()
  }
}
