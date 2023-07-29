package AkkaRemoting.P3_Clustering

import akka.actor.{Actor, ActorLogging}
import Messages._

class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case RunOperation(input: Any, operation: (Any => Any)) =>
      log.info(s"Worker ${self.path} (dispatcher: " +
        s"${context.system.settings.config.getLong("akka.remote.artery.canonical.port")}) received: \n\t${input.toString}")
      sender() ! OperationResult(operation(input))

    case _ =>
      log.error("Unhandled message!")
      throw new RuntimeException("Unhandled message!")
  }
}
