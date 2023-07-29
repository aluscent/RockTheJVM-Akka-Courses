package AkkaPersistence.P2_Primer

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.{Date, UUID}
import scala.util.Random

object E2_PersistentActors {

  case class Invoice(recipient: String, date: Date, amount: Int) {
    def toInvoicePersisted(id: String): InvoicePersisted = InvoicePersisted(id, this)
  }

  case class InvoiceBulk(invoices: List[Invoice])

  case class InvoicePersisted(id: String, invoice: Invoice)


  // Custom shutdown message
  case object Shutdown

  class Accountant extends PersistentActor with ActorLogging {
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant"

    /**
     * The normal receive method.
     */
    override def receiveCommand: Receive = {
      case invoice@Invoice(_, _, amount) =>
        /*
         * when get a command, you do the following:
         * 1) you create an event to persist into the store
         * 2) you pass in a callback that will get triggered once the event is written
         * 3) we update the actor state when the event is persisted
         */
        log.info(s"Received invoice with amount: $amount")
        persist(InvoicePersisted(UUID.randomUUID().toString, invoice)) { e =>
          totalAmount += amount
          log.info(s"Invoice ID ${e.id} with recipient ${e.invoice.recipient} persisted. Total amount is $totalAmount.")
        }

      // persisting multiple events
      case InvoiceBulk(invoices) =>
        /*
         * 1) create events
         * 2) persist all the events
         * 3) update actor state per persist
         */
        val invoiceIds = invoices.indices.toList.map(_ => UUID.randomUUID().toString)
        val events = invoices.zip(invoiceIds).map { case (invoice: Invoice, id: String) =>
          invoice.toInvoicePersisted(id)
        }
        persistAll(events) { event =>
          totalAmount += event.invoice.amount
          log.info(s"[InvoiceBulk] ${event.id} with recipient ${event.invoice.recipient} persisted. Total amount: $totalAmount.")
        }

      case Shutdown =>
        log.info("Shutting down...")
        context.stop(self)
    }

    /**
     * The handler that will be called on recovery.
     * Best practice is to follow the logic of receiveCommand.
     */
    override def receiveRecover: Receive = {
      case InvoicePersisted(id, invoice) =>
        totalAmount += invoice.amount
        log.info(s"Transaction $id recovered. Total amount: $totalAmount")
    }

    /**
     * Thrown when an error occurs during persisting events into DB. The actor stops.
     *
     * Better to restart actor after a while.
     */
    override protected def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Unable to persist event $event\nREASON: $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /**
     * Happens when then "journal" encounters an error persisting events. The actor resumes.
     */
    override protected def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.info(s"Persisting event $event rejected.\nREASON: $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Persistence")
    val actor = system.actorOf(Props[Accountant], "test")

    //    (20 to 30).foreach(i => actor ! Invoice(f"res-$i", new Date, i * 100))

    // This method will cause the actor to fail immediately. It is recommended to make your own shutdown method.
    // actor ! PoisonPill

    val bulk = for (i <- 15 to 20) yield Invoice(s"res-$i", new Date, Random.between(1000, 2000))
    actor ! InvoiceBulk(bulk.toList)
  }
}
