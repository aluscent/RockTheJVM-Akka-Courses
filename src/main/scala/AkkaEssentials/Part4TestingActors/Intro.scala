package AkkaEssentials.Part4TestingActors

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration._
import scala.language.postfixOps

class IntroSpec extends TestKit(ActorSystem("introSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  // Basic setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "the thing thats being tested" should {    // this is a test-suit
    "do this" in {    // these are tests
      true // testing scenario
    }
  }

  "A simple actor" should {
    "send back the message I sent" in {
      val actorTest = system.actorOf(Props[IntroSpec.SimplestActor])

      val message = "hello"
      actorTest ! message

      expectMsg(message)
    }
  }

  "A no response actor" should {
    "send back the message I sent" in {
      val actorTest = system.actorOf(Props[IntroSpec.NoResponseActor])

      val message = "hello"
      actorTest ! message

      expectNoMessage(3 second)
    }
  }
}

object IntroSpec {
  class SimplestActor extends Actor {
    override def receive: Receive = {
      case message: String => sender() ! message
    }
  }

  class NoResponseActor extends Actor {
    override def receive: Receive = {
      case message: String =>
    }
  }
}
