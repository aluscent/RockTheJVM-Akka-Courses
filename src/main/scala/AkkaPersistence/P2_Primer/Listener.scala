package AkkaPersistence.P2_Primer

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.persistence.PersistentActor

import java.util.{Date, UUID}
import scala.language.postfixOps

object Listener {

  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoicePersisted(id: String, invoice: Invoice)

  class Accountant extends PersistentActor with ActorLogging {
    var totalAmount = 0

    override def persistenceId: String = "accountant"

    override def receiveCommand: Receive = {
      case invoice@Invoice(_, _, amount) =>
        log.info(s"Received invoice with amount: $amount")
        persist(InvoicePersisted(UUID.randomUUID().toString, invoice)) { e =>
          totalAmount += amount
          log.info(s"Invoice ID ${e.id} with recipient ${e.invoice.recipient} persisted. Total amount is $totalAmount.")
        }
    }

    override def receiveRecover: Receive = {
      case InvoicePersisted(id, invoice) =>
        totalAmount += invoice.amount
        log.info(s"Transaction $id recovered. Total amount: $totalAmount")
    }
  }

  def htmlBodyDecorator(text: String): String =
    s"""
       |<html> <body>
       |$text
       |</body> </html>
       |""".stripMargin

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("Persistence")
    val actor = system.actorOf(Props[Accountant], "test")

    import akka.http.scaladsl.server.Directives._
    val chainedRoute = (parameter(Symbol("recipient").as[String]) | path(Segment)) { recipient =>
      (parameter(Symbol("amount").as[String]) | path(Segment)) { amount =>
        actor ! Invoice(recipient, new Date, amount.toInt)
        complete(HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          htmlBodyDecorator(s"Your transaction was successful for $recipient.")
        ))
      }
    }

    Http().newServerAt("localhost", 12345).bind(chainedRoute)
  }
}
