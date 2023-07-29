package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  object SimpleActor {
    def prop = Props(new SimpleActor)
  }
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hello!" =>
        println(s"[${context.self}] received message 'Hello' from ${context.sender()}.")
        context.sender() ! "Hello there!"
      case msg: String =>
        Thread.sleep(500)
        println(s"[${context.self}] received message '$msg'.")
      case msg: Int =>
        Thread.sleep(500)
        context.self ! msg.toString
      case SayHi(ref) =>
        println(s"[${context.self}] received msg from $ref.")
        ref ! "Hello!"
      case phoneMessage(content, ref) => ref forward content // keeping the original sender of content
      case _ => println(s"[${context.self}] no messages.")
    }
  }

  val actorSystem = ActorSystem("actorCapabilitiesSystem")
  val simpleActor = actorSystem.actorOf(SimpleActor.prop, "firstSimpleActor")

  /* simpleActor ! "hello actor"
  simpleActor ! 4387 */

  // I - messages can be any type with these in mind:
  //  1 - messages must be immutable.
  //  2 - messages must be serializable.
  // in practice, use case classes and case objects.

  // II - actors have information about their context and themselves.
  //  they have a property 'context' to access these information.(e.g. context.self, ...)


  // III - actors can REPLY to messages:
  val alice = actorSystem.actorOf(SimpleActor.prop, "alice")
  val bob = actorSystem.actorOf(SimpleActor.prop, "bob")
  case class SayHi(ref: ActorRef)
  bob ! SayHi(alice)

  // IV - messages sent to null actors are received by FAKE actor 'deadLetters'

  // V - forwarding messages
  val cat = actorSystem.actorOf(SimpleActor.prop, "cat")
  case class phoneMessage(content: Any, ref: ActorRef)
  alice ! phoneMessage("Hello", cat)
}
