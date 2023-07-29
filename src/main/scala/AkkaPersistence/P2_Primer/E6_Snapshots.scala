package AkkaPersistence.P2_Primer

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.mutable

object E6_Snapshots {
  case class Message(sender: String, content: String)

  // commands
  case class ReceivedMessage(message: Message)

  case class SentMessage(message: Message)

  case object PrintTime

  // events
  case class ReceivedMessageRecord(id: String, message: Message)

  case class SentMessageRecord(id: String, message: Message)

  object Chat {
    def props(owner: String, contact: String, start: Int) = Props(new Chat(owner, contact, start))
  }

  class Chat(owner: String, contact: String, start: Int) extends PersistentActor with ActorLogging {
    val MAX_MESSAGES = 10

    val lastMessages = new mutable.Queue[Message]()

    override def receiveRecover: Receive = receiveCommandStateful(0, new mutable.Queue[Message]())
    //    {
    //      case ReceivedMessageRecord(id, message) =>
    //        log.info(s"[RECOVER] ReceivedMessageRecord {$id} $message")
    //      case SentMessageRecord(id, message) =>
    //        log.info(s"[RECOVER] SentMessageRecord {$id} $message")
    //      case SnapshotOffer(metadata, snapshot) =>
    //        log.info(s"[RECOVER] Recovered snapshot: $metadata")
    //        snapshot.asInstanceOf[mutable.Queue[Message]].foreach()
    //    }


    override def receiveCommand: Receive = receiveCommandStateful(0, new mutable.Queue[Message]())

    private def receiveCommandStateful(sequence: Int, lastMessages: mutable.Queue[Message]): Receive = {
      // COMMANDs
      case ReceivedMessage(message) =>
        val uuid = UUID.randomUUID().toString
        val q = lastMessages.mayReplaceMessage(message)
        log.info(s"[COMMAND] ReceivedMessage {$uuid} | sequence $sequence")
        persist(ReceivedMessageRecord(uuid, message)) { _ =>
          if (sequence % 10 == 0) saveSnapshot(lastMessages)
        }
        context.become(receiveCommandStateful(sequence + 1, q))
      case SentMessage(message) =>
        val uuid = UUID.randomUUID().toString
        val q = lastMessages.mayReplaceMessage(message)
        log.info(s"[COMMAND] SentMessage {$uuid} | sequence $sequence")
        persist(SentMessageRecord(uuid, message))(_ => ())
        context.become(receiveCommandStateful(sequence + 1, q))
      case PrintTime =>
        log.info(s"[COMMAND] TIME PASSED: " + (LocalDateTime.now().toLocalTime.toSecondOfDay - start))

      // RECOVERs
      case ReceivedMessageRecord(id, message) =>
        log.info(s"[RECOVER] ReceivedMessageRecord {$id} $message")
      case SentMessageRecord(id, message) =>
        log.info(s"[RECOVER] SentMessageRecord {$id} $message")
      case SnapshotOffer(metadata, snapshot) =>
        log.info(s"[RECOVER] Recovered snapshot: $metadata")
        snapshot.asInstanceOf[mutable.Queue[Message]].foreach(lastMessages.enqueue(_))
        context.become(receiveCommandStateful(sequence, lastMessages))
    }

    override def persistenceId: String = s"chat-$owner-$contact"

    implicit class enrichQueue(q: mutable.Queue[Message]) {
      def mayReplaceMessage(message: Message): mutable.Queue[Message] = {
        if (q.size > MAX_MESSAGES) q.dequeue()
        q.enqueue(message)
        q
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("WhatsApp")
    val start = LocalDateTime.now().toLocalTime.toSecondOfDay
    val chat = system.actorOf(Chat.props("Ali", "Sana", start))


    //    (102000 to 103000).toList foreach { i =>
    //      val message = Message("main method", s"This is message $i")
    //      chat ! ReceivedMessage(message)
    //      chat ! SentMessage(message)
    //    }
    chat ! PrintTime

  }
}
