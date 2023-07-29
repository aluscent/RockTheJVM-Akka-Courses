package AkkaEssentials.Part3Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviour {
  object Mom {
    case class MomStart(kid: ActorRef)

    trait FoodType
    case class Vegetable() extends FoodType
    case class Milk() extends FoodType

    case class Food(food: FoodType)
    case class Ask(message: String)

    def props = Props(new Mom)
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kid) =>
        kid ! Food(Vegetable())
        kid ! Ask("Are you happy?")
      case KidReject =>
        sender() ! Food(Milk())
        sender() ! Ask("Are you happy?")
      case KidAccept =>
        println("Thanks God!")
    }
  }

  object FussyKid {
    trait Mood
    case class Sad() extends Mood
    case class Happy() extends Mood

    case object KidAccept
    case object KidReject

    def props = Props(new FussyKid)
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    var state: Mood = Happy()
    override def receive: Receive = receiveHappy

    def receiveHappy: Receive = {
      case Food(Vegetable()) => context.unbecome()
      case Food(Milk()) => context.become(receiveHappy, false)
      case Ask(_) => sender() ! KidAccept
    }
    def receiveSad: Receive = {
      case Food(Vegetable()) => context.become(receiveSad, false)
      case Food(Milk()) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("actorChangingSystem")
    val mom = actorSystem.actorOf(Mom.props, "mom")
    val kid = actorSystem.actorOf(FussyKid.props, "kid")

    mom ! Mom.MomStart(kid)
  }
}
