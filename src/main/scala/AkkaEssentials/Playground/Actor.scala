package AkkaEssentials.Playground

import AkkaEssentials.Playground.BankActor.ATM.props
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object CounterActor extends App {
  val actorSystem = ActorSystem("counterSystem")

  object Counter {
    def props = Props(new Counter)
  }
  class Counter extends Actor {
    var counter = 0
    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[$self] The counter is on: $counter")
    }
  }

  trait Ops
  case class Increment() extends Ops
  case class Decrement() extends Ops
  case class Print() extends Ops

  val actor = actorSystem.actorOf(Counter.props, "counterActor")
  val listOps = List(Increment, Increment, Decrement, Increment, Print)
  listOps.foreach(x => actor ! x)
}


object BankActor {
  val actorSystem = ActorSystem("bankAccountSystem")

  object BankAccount {
    def props = Props(new BankAccount)
  }
  class BankAccount extends Actor {
    var remainder = 0
    var history = List[Ops]()
    override def receive: Receive = {
      case Deposit(amount) =>
        history = Deposit(amount) :: history
        remainder += amount
        sender() ! Success
      case Withdraw(amount) if remainder >= amount =>
        history = Withdraw(amount) :: history
        remainder -= amount
        sender() ! Success
      case Withdraw(amount) if remainder < amount =>
        sender() ! Failure
      case Statement() =>
        println(history)
        sender() ! Success
      case _ =>
        println(s"[Bank] Invalid!")
        sender() ! Failure
    }
  }

  object ATM {
    def props(bank: ActorRef) = Props(new ATM(bank))
  }
  class ATM(bank: ActorRef) extends Actor {
    override def receive: Receive = {
      case ops: Ops => bank ! ops
      case status: Status => println(status)
      case _ => println("[ATM] Invalid!")
    }
  }

  trait Ops
  case class Deposit(amount: Int) extends Ops {
    override def toString: String = s"Deposit $amount"
  }
  case class Withdraw(amount: Int) extends Ops {
    override def toString: String = s"Withdraw $amount"
  }
  case class Statement() extends Ops {
    override def toString: String = "Statement"
  }

  trait Status
  case class Success() extends Status {
    override def toString: String = "Success"
  }
  case class Failure() extends Status {
    override def toString: String = "Failure"
  }


  def main(args: Array[String]): Unit = {
    val bank = actorSystem.actorOf(BankAccount.props, "bank")
    val aTM = actorSystem.actorOf(ATM.props(bank), "ATM")
    val listOps = List[Ops](Deposit(500), Deposit(200), Withdraw(300), Deposit(50), Statement())
    //  listOps.foreach(x => atm ! x)
    aTM ! Deposit(500)
  }
}