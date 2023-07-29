package AkkaEssentials.Part4TestingActors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("testProb")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  // the entity to test the interaction between master and slave is called test probe
  "a master actor" should {
    import TestProbeSpec._

    "register slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck())

      val workLoadString = "this is work-load string"
      master ! Work(workLoadString)

      slave.expectMsg(SlaveWork(workLoadString, testActor))
      slave.reply(WorkCompleted(5, testActor))

      expectMsg(Report(5))
    }
  }
}

object TestProbeSpec {
  case class Register(actor: ActorRef)
  case class Work(text: String)
  case class WorkCompleted(totalCount: Int, requester: ActorRef)
  case class SlaveWork(text: String, requester: ActorRef)
  case class Report(totalCount: Int)
  case class RegistrationAck()

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slave) =>
        sender() ! RegistrationAck()
        context.become(Online(slave, 0))
      case _ =>
    }

    def Online(slave: ActorRef, wordCount: Int): Receive = {
      case Work(text) => slave ! SlaveWork(text, sender())
      case WorkCompleted(count, requester) =>
        val newTotalCount = wordCount + count
        requester ! Report(newTotalCount)
        context.become(Online(slave, wordCount))
    }
  }
}
