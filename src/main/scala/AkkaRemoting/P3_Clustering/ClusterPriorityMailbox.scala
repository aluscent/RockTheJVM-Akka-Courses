package AkkaRemoting.P3_Clustering

import AkkaRemoting.P3_Clustering.Messages.CustomEvent
import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.MemberEvent
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

class ClusterPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox (
  PriorityGenerator {
    case _: MemberEvent => 0
    case _: CustomEvent => 1
    case _ => 5
  }
)
