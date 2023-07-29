package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Intro extends App {
  // 1 - create an actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // 2 - create actors
  class WordCountActor extends Actor {
    override def receive: PartialFunction[Any, Unit] = {
      case msg: String => println(msg.split(" ").toList)
      case msg => println(s"i cant understand the message $msg")
    }
  }

  // 3 - instantiate the actor
  val actorWordCount1: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter_1")
  val actorWordCount2: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter_2")

  // 4 - communicate
  actorWordCount1 ! "Akka is really cool! I hope you join me."
  actorWordCount2 ! 2354


  // we can create our own actors.
  object Person {
    def props(name: String) = Props(new Person(name))
  }
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case msg: String => println(s"[Actor $name] Received your message: $msg")
      case msg => println(s"[Actor $name] No messages.")
    }
  }
  val personActorJohn = actorSystem.actorOf(Person.props("John"))
  personActorJohn ! "Hello to you from Mars!"
}
