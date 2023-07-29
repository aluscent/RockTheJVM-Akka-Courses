package AkkaEssentials.Part4TestingActors

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import java.util.Random
import scala.language.postfixOps

object TimedAssertionSpec {
  case class WorkResult(result: String)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender() ! WorkResult("done")
      case "workSeq" =>
        val r = new Random()
        for (_ <- 1 to 12) Thread.sleep(r.nextInt(50))
        sender() ! WorkResult("work done.")
    }
  }
}

class TimedAssertionSpec extends TestKit(ActorSystem("timedAssertions", ConfigFactory.load().getConfig("specialTimedAssertionConfiguration")))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import TimedAssertionSpec._
  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply in a timely manner" in {
      within(500 millis, 1000 millis) {
        workerActor ! "work"

        expectMsg(WorkResult("done"))
      }
    }

    "reply with a valid work" in {
      within(1 second) {
        workerActor ! "workSeq"

        val results: Seq[String] = receiveWhile(2 second, 500 millis, 12) {
          case WorkResult(result) => result
        }

        assert(results.length > 5)
      }
    }

    "reply to a test probe" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(testActor, "work")
        expectMsg(WorkResult("done"))
      }
    }
  }
}
