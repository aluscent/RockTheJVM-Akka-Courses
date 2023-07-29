package AkkaStreams.P4_TechniquesPatterns

import akka.actor.ActorSystem
import akka.stream.ActorAttributes
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.runtime.Nothing$
import scala.util.Random

object FaultTolerance {
  implicit val system = ActorSystem("faultTolerance")

  def main(args: Array[String]): Unit = {
    // 1 - Logging
    val faultySource = Source(1 to 10).map(n => if (n == 6) throw new RuntimeException else n)
    faultySource.log("Tracking elements.").to(Sink.ignore)//.run

    // 2 - Terminating stream
    faultySource.recover {
      case _: RuntimeException => Int.MinValue
    }.log("Terminating source.").to(Sink.ignore)//.run()

    // 3 - Recover with another stream
    faultySource.recoverWithRetries(3, {
      case _: RuntimeException => Source(45 to 49)
    }).log("Recovered.").to(Sink.ignore)//.run()

    // 4 - Backoff supervision
    RestartSource.onFailuresWithBackoff(
      2 seconds,
      1 minute,
      randomFactor = 0.1
    )(() => {
      val random = new Random()
      Source(1 to 10).map(x => if (random.between(0,10) == x) throw new RuntimeException else x)
    }).log("Restart.").to(Sink.ignore)//.run()

    // 5 - Supervision strategy
    faultySource.log("Supervision.").withAttributes(ActorAttributes.supervisionStrategy {
      /**
       * Resume, Stop, Restart
       */
      case _:RuntimeException => Resume
      case _ => Stop
    }).to(Sink.ignore).run()
  }
}
