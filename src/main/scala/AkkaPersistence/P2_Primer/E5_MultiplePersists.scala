package AkkaPersistence.P2_Primer

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.{Date, UUID}
import scala.util.Random

object E5_MultiplePersists {

  /**
   * Diligent accountant - persisting 2 events:
   * 1. a tax record
   * 2. an invoice record
   */

  // commands
  case class Invoice(recipient: String, date: Date, amount: Int)

  // events
  case class TaxRecord(taxId: String, recordId: String, date: Date, totalAmount: Double)

  case class InvoiceRecord(id: String, invoice: Invoice)

  // actors
  class DiligentAccountant(taxId: String, taxAuthority: ActorRef)
    extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case event =>
        log.info(s"[DiligentAccountant] Recovered event: $event")
    }

    override def receiveCommand: Receive = {
      case invoice@Invoice(_, _, amount) =>
        persist(TaxRecord(
          UUID.randomUUID().toString,
          UUID.randomUUID().toString,
          new Date,
          (amount * 0.28).toInt.toDouble)
        ) { record =>
          taxAuthority ! record

          persist(s"The authority declares receiving tax record: $record") {
            taxAuthority ! _
          }
        }
        persist(InvoiceRecord(UUID.randomUUID().toString, invoice)) { invoice_ =>
          taxAuthority ! invoice_

          persist(s"The authority declares receiving invoice record: $invoice_") {
            taxAuthority ! _
          }
        }
    }

    override def persistenceId: String = "Diligent-Accountant-v0.2"
  }

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) =
      Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(s"[Authority] Got message: $message")
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("alitasl")
    val authority = system.actorOf(Props[TaxAuthority], "HMRC")
    val accountant = system.actorOf(DiligentAccountant.props(UUID.randomUUID().toString, authority))

    (1 to 10).toList.foreach { _ =>
      accountant ! Invoice(UUID.randomUUID().toString, new Date, Random.between(100, 2000) * 100)
    }
  }
}
