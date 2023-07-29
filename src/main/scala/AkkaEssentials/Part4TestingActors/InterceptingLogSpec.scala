package AkkaEssentials.Part4TestingActors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogSpec extends TestKit(ActorSystem("interceptingLogs", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import InterceptingLogSpec._
  "A checkout flow" should {
    "correctly log" in {
      EventFilter.info(pattern = s"Order .*") intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout("test-item", "6534-3546-5487-9237")
      }
    }
  }
}

object InterceptingLogSpec {
  case class Checkout(item: String, card: String)
  case class AuthorizeCard(card: String)
  case class DispatchOrder(item: String)

  case object OrderConfirmed
  case object OrderCanceled

  object PaymentAccepted
  object PaymentDenied

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingPayment(item))
      case PaymentDenied =>
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if (card.startsWith("0")) sender() ! PaymentDenied
        else sender() ! PaymentAccepted
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    override def receive: Receive = {
      case DispatchOrder(item) =>
        log.info(s"Order with item $item has been dispatched.")
        sender() ! OrderConfirmed
    }
  }
}
