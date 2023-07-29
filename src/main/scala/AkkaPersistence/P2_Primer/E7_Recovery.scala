package AkkaPersistence.P2_Primer

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, Recovery}

object E7_Recovery {

  case class Command(payload: String)

  case class Event(payload: String)

  class RecoveryActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "recovery-actor"

    override def receiveRecover: Receive = {
      case Event(payload) if payload.contains("Command 31") =>
        throw new RuntimeException("Command 31 is not allowed")
      case Event(payload) =>
        log.info(s"Recovered event: $payload")
    }

    override def receiveCommand: Receive = {
      case Command(payload) =>
        persist(Event(payload)) { event =>
          log.info(s"Persisted $event")
        }
    }

    override protected def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.info(s"Failure of type '${cause.getMessage}'\nDuring recovery of event: $event")
      super.onRecoveryFailure(cause, event)
    }

    //    override def recovery: Recovery = Recovery(toSequenceNr = 20)
    //    override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)
    override def recovery: Recovery = Recovery()
  }

  def main(args: Array[String]): Unit = {
    val system = akka.actor.ActorSystem("recovery-actor-system")
    val recoveryActor = system.actorOf(akka.actor.Props[RecoveryActor], "recovery-actor")

    /** Stashing commands
     * All commands sent during recovery are stashed.
     */
    //    (1 to 1000).foreach(i => recoveryActor ! Command(s"Command $i"))
  }
}
