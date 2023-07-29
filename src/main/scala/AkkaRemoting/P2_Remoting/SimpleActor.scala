package AkkaRemoting.P2_Remoting

import akka.actor.{Actor, ActorLogging}

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m => log.info(s"[Actor] Received $m from ${sender()}")
  }
}
