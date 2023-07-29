package AkkaRemoting.P3_Clustering

import akka.actor.ActorRef
import akka.cluster.Member

object Messages {
  trait CustomEvent
  case class MemberJoining(member: Member, actor: ActorRef) extends CustomEvent
  case class MemberWaking(member: Member, actor: ActorRef) extends CustomEvent

  trait CustomOperation
  case class FileOperation(file: String, operation: String => Any) extends CustomOperation
  case class RunOperation[I, O](input: I, operation: I => O) extends CustomOperation
  case class OperationResult[T](result: T) extends CustomOperation
}
