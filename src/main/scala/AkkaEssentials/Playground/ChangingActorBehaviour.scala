package AkkaEssentials.Playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.util.Random

object Counter {

  object Counter {
    case class Increment()
    case class Decrement()
    case class Print()

    def props = Props(new Counter)
  }
  class Counter extends Actor {

    import Counter._

    override def receive: Receive = Zero

    def Zero: Receive = {
      case Increment => context.become(NonZero(1), false)
      case Decrement =>
      case Print => println("[Actor] State Zero.")
    }

    def NonZero(value: Int): Receive = {
      case Increment => context.become(NonZero(value + 1), false)
      case Decrement => context.unbecome()
      case Print => println(s"[Actor] State: $value.")
    }
  }

  def main(args: Array[String]): Unit = {
    import Counter.{Print, Increment, Decrement}

    val system = ActorSystem("Counter")
    val counter = system.actorOf(Counter.props)

    val opsList = List(Increment, Increment, Decrement, Print, Decrement, Decrement, Print)
    opsList.foreach(x => counter ! x)
  }
}


object VotingSystem {
  object Candidate {
    trait Candide {def toString: String}
    case class Trump() extends Candide {override def toString: String = "Trump"}
    case class Biden() extends Candide {override def toString: String = "Biden"}
    case class Clinton() extends Candide {override def toString: String = "Clinton"}
    case class Obama() extends Candide {override def toString: String = "Obama"}
  }

  import Candidate._
  val candidates: List[Candide] = List(Trump(), Biden(), Obama(), Clinton())

  object Citizen {
    def citizenProps(ref: ActorRef) = Props(new Citizen(ref))
  }
  class Citizen(ref: ActorRef) extends Actor {
    import Candidate._

    override def receive: Receive = {
      case Trump() => ref ! Trump()
      case candide: Candide => ref ! candide
      case _ => println("[Citizen] Invalid Option.")
    }
  }

  object Aggregator {
    def aggregatorProps = Props(new Aggregator)
    case class Print()
  }
  class Aggregator extends Actor {
    import Candidate._
    import Aggregator._

    override def receive: Receive = votes(0, 0, 0, 0)

    def votes(trump: Int, biden: Int, obama: Int, clinton: Int): Receive = {
      case Trump() => context.become(votes(trump + 1, biden, obama, clinton))
      case Biden() => context.become(votes(trump, biden + 1, obama, clinton))
      case Obama() => context.become(votes(trump, biden, obama + 1, clinton))
      case Clinton() => context.become(votes(trump, biden, obama, clinton + 1))
      case Print() =>
        println(s"Sum of votes: ${trump + biden + obama + clinton}")
        println(s"Votes: Trump: $trump\t Biden: $biden\t Obama: $obama\t Clinton: $clinton")
        val currentVotes = Map[String, Int]("Trump" -> trump, "Biden" -> biden, "Obama" -> obama, "Clinton" -> clinton)
        println(currentVotes)
      case _ => println("[Aggregator] Invalid Option.")
    }
  }

  def main(args: Array[String]): Unit = {
    import Aggregator._
    import Citizen._
    val system = ActorSystem("voting")

    val aggregatorCount = 5
    val aggregators: List[ActorRef] = (1 to aggregatorCount).toList
      .map(x => system.actorOf(aggregatorProps, s"aggregator_$x"))

    val citizenCount = 1000
    val citizens: List[ActorRef] = (1 until citizenCount).toList
      .map(x => system.actorOf(citizenProps(aggregators(((x.toFloat / citizenCount) * aggregatorCount).toInt)), s"citizen_$x"))

    val random = new Random()
    citizens.foreach(x => x ! candidates(random.nextInt(candidates.length)))

    Thread.sleep(1000)
    aggregators.foreach(x => x ! Print())
  }
}