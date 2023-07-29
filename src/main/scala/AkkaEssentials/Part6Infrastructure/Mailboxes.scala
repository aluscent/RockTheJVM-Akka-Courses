package AkkaEssentials.Part6Infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes {
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(message)
      case ManagementTicket => log.info("Management Ticket.")
    }
  }

  class SupportTicket(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(PriorityGenerator {
    case message: String if message.startsWith("[P0]") => 0
    case message: String if message.startsWith("[P1]") => 1
    case message: String if message.startsWith("[P2]") => 2
    case _ => 3
  })

  case object ManagementTicket extends ControlMessage

  def main(args: Array[String]): Unit = {

    // Scenario #1: custom priority mailbox
    val system_1 = ActorSystem("mailboxDemo", ConfigFactory.load().getConfig("mailboxDemo"))
    val supportTicket = system_1.actorOf(Props[SimpleActor].withDispatcher("support-ticket-pr"), "supportTicket")
    supportTicket ! PoisonPill
    supportTicket ! "goodnight"
    supportTicket ! "[P1] hello"
    supportTicket ! "[P0] hi"
    supportTicket ! "[P2] bye"

    // Scenario #2: control-aware mailbox
    //  mark important messages as control messages
    // method 1:
    val controlAwareMailbox = system_1.actorOf(Props[SimpleActor].withMailbox("control-mailbox"), "controlAwareMailbox")
    controlAwareMailbox ! "goodnight"
    controlAwareMailbox ! "[P1] hello"
    controlAwareMailbox ! ManagementTicket
    // method 2:
    val altControlAwareMailbox = system_1.actorOf(Props[SimpleActor], "altControlAwareMailbox")
    altControlAwareMailbox ! "goodnight"
    altControlAwareMailbox ! "[P1] hello"
    altControlAwareMailbox ! ManagementTicket
  }
}
