package AkkaPersistence.P2_Primer

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object E8_PersistentAsync {
  case class DummyCommand(content: String)

  case class DummyEvent(content: String)

  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def persistenceId: String = "critical-stream-processor"

    override def receiveCommand: Receive = {
      case DummyCommand(content) =>
        eventAggregator ! s"Processing: $content"
        persistAsync(DummyEvent(content))(eventAggregator ! s"Processing events: " + _)
        val processedContent = content + " (processed)"
        persistAsync(DummyEvent(processedContent))(eventAggregator ! _)
    }

    override def receiveRecover: Receive = {
      case msg => log.info(s"[RECOVER] Received: $msg")
    }
  }

  object CriticalStreamProcessor {
    def props(eventAggregator: ActorRef) = Props(new CriticalStreamProcessor(eventAggregator))
  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg =>
        log.info(s"[AGGREGATOR] Received: $msg")
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("alitasl")
    val aggregator = system.actorOf(Props[EventAggregator], "agg-1")
    val processor = system.actorOf(CriticalStreamProcessor.props(aggregator), "csp-1")

    processor ! DummyCommand("process-1")
    processor ! DummyCommand("process-2")
  }
  /*
  persistAsync has the upper-hand performance-wise over persist.
  But it's bad when you need events ordering.
   */
}
