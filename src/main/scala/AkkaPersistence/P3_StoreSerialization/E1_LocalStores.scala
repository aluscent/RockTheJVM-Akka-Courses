package AkkaPersistence.P3_StoreSerialization

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

object E1_LocalStores {
  case object Print

  case object Snapshot

  class SimplePersistentActor extends PersistentActor with ActorLogging {
    var num = 0

    override def receiveRecover: Receive = {
      case SnapshotOffer(_, snapshot: Int) =>
        log.info(s"Recovered snapshot: $snapshot")
        num = snapshot
      case msg =>
        log.info(s"Recovered: $msg")
        num += 1
    }

    override def receiveCommand: Receive = {
      case Print =>
        log.info(s"Persisted $num messages.")
      case Snapshot =>
        saveSnapshot(num)
      case msg =>
        persist(msg) { m =>
          log.info(s"Persisting: $m")
          num += 1
        }
      case SaveSnapshotSuccess =>
        log.info(s"Snapshot successfully saved.")
      case SaveSnapshotFailure(_, cause) =>
        log.info(s"Snapshot saving failed: $cause")
    }

    override def persistenceId: String = "simple-persistent-actor"
  }

  def main(args: Array[String]): Unit = {

  }
}
