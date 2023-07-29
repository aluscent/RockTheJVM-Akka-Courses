package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActor {
  object Parent {
    case class CreateChild(name: String)
    case class ExecuteChild(command: Child.Command)
    def props = Props(new Parent)
  }
  class Parent extends Actor {
    import Parent._

    override def receive: Receive = NoChild

    def NoChild: Receive = {
      case CreateChild(name) => context.become(Children(List(context.actorOf(Child.props(name), name))))
      case ExecuteChild(command) => println("No child to execute this command.")
      case _ => println("[Parent] Invalid.")
    }

    def Children(children: List[ActorRef]): Receive = {
      case CreateChild(name) => context.become(Children(context.actorOf(Child.props(name), name) :: children))
      case ExecuteChild(command) => children.foreach(x => x ! command)
      case _ => println("[Parent] Invalid.")
    }
  }

  object Child {
    trait Command
    case class Increment() extends Command
    case class Decrement() extends Command
    case class Print() extends Command

    def props(name: String) = Props(new Child(name))
  }
  class Child(val name: String) extends Actor {
    import Child._

    override def receive: Receive = Zero

    def Zero: Receive = {
      case Increment() => context.become(NonZero(1))
      case Decrement() => context.become(Zero)
      case Print() => println(s"[$name] Counter is 0.")
      case _ => println(s"[$name] Invalid.")
    }

    def NonZero(counter: Int): Receive = {
      case Increment() => context.become(NonZero(counter + 1))
      case Decrement() => if (counter > 1) context.become(NonZero(counter + 1))
        else context.become(Zero)
      case Print() => println(s"[$name]Counter is $counter.")
      case _ => println(s"[$name] Invalid.")
    }
  }

  def main(args: Array[String]): Unit = {
    import Child.{Increment, Decrement, Print}
    import Parent.{CreateChild, ExecuteChild}

    val system = ActorSystem("childActors")
    val parent = system.actorOf(Parent.props, "parent")

    val ops = List(CreateChild("firstCounter"), ExecuteChild(Increment()), ExecuteChild(Increment()), ExecuteChild(Print()),
      CreateChild("secondCounter"), ExecuteChild(Increment()), ExecuteChild(Print()), CreateChild("thirdCounter"),
      ExecuteChild(Increment()), ExecuteChild(Increment()), ExecuteChild(Increment()), ExecuteChild(Print()),
      ExecuteChild(Decrement()), ExecuteChild(Decrement()), ExecuteChild(Print()))
    ops.foreach(x => parent ! x)


    // you can create actor hierarchy: parent -> child -> grandchild

    // Guardian actors(Top-level):
    //  0 / = root guardian
    //  1 /system = system guardian
    //  2 /user = user level guardian


    // Actor selection:
    val childSelection = system.actorSelection("/user/parent/secondCounter")
    childSelection ! Print()

    // NEVER PASS MUTABLE ACTOR STATE OR THIS REFERENCE TO CHILD ACTORS!
    //  has the danger of breaking actor encapsulation.
  }
}
