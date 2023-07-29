package AkkaEssentials.Part5FaultTolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("supervisionSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{

  override protected def afterAll(): Unit = super.afterAll()

  import SupervisionSpec._
  "A supervisor" should {
    "stop actor in case of string doesn't start with capital letter" in {
      val supervisor1 = system.actorOf(Props[Supervisor], "supervisor1")
      supervisor1 ! Props[FussyWordCounter]
      val child1 = expectMsgType[ActorRef]
      watch(child1)

      child1 ! "i love Akka."
      child1 ! Report
      supervisor1 ! PoisonPill
    }

    "resume its child in case of a minor fault" in {
      val supervisor2 = system.actorOf(Props[Supervisor], "supervisor2")
      supervisor2 ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "I love Akka and I love Akka and I love Akka."
      child2 ! Report
      supervisor2 ! PoisonPill
    }

    "restart child in case of zero-length string" in {
      val supervisor3 = system.actorOf(Props[Supervisor], "supervisor3")
      supervisor3 ! Props[FussyWordCounter]
      val child3 = expectMsgType[ActorRef]

      child3 ! ""
      child3 ! Report
      supervisor3 ! PoisonPill
    }

    "escalate exception from child to supervisor" in {
      val supervisor4 = system.actorOf(Props[Supervisor], "supervisor4")
      supervisor4 ! Props[FussyWordCounter]
      val child4 = expectMsgType[ActorRef]

      watch(child4)
      child4 ! 23
      supervisor4 ! PoisonPill
    }
  }
}

object SupervisionSpec {
  case object Report

  class Supervisor extends Actor {
    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: RuntimeException => Resume
      case _: IllegalArgumentException => Stop
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val child = context.actorOf(props)
        sender() ! child
    }
  }

  class FussyWordCounter extends Actor with ActorLogging{
    var words = 0

    override def preStart(): Unit = log.info("FussyWordCounter starting.")

    override def postStop(): Unit = log.info("FussyWordCounter stopped.")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info("FussyWordCounter restarting.")

    override def postRestart(reason: Throwable): Unit =
      log.info("FussyWordCounter restarted.")

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence is empty.")
      case sentence: String =>
        if (sentence.length > 10) throw new RuntimeException("Sentence is too big.")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("Sentence doesn't start uppercase.")
        else words += sentence.split(' ').length
      case _ => throw new Exception("Option not valid.")
    }
  }
}