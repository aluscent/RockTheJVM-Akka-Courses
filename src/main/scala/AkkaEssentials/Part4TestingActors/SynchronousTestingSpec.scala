package AkkaEssentials.Part4TestingActors

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll{
  implicit val system: ActorSystem = ActorSystem("syncTest")

  override protected def afterAll(): Unit = system.terminate()

  import SynchronousTestingSpec._
  "a counter" should {
    "increase counter synced" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! "tick"

      assert(counter.underlyingActor.cntr == 1)
    }

    "increase counter at call of receive" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive("tick")
    }

    "work on the calling thread" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, "read")
      probe.expectMsg(Duration.Zero, 0)
    }
  }
}

object SynchronousTestingSpec {
  class Counter extends Actor {
    var cntr = 0
    override def receive: Receive = {
      case "tick" => cntr += 1
      case "read" => sender() ! cntr
    }
  }
}