package AkkaRemoting.P3_Clustering

import akka.actor.{ActorSystem, Address, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory

object ManualRegistration {

  val system = ActorSystem("clusterTest1", ConfigFactory
    .load("AkkaRemoting/Part3Clustering/application.conf")
    .getConfig("manualRegistration"))

  val cluster = Cluster(system)
  cluster.joinSeedNodes(Seq(
    Address("akka", "clusterTest1","localhost",2551),
    Address("akka", "clusterTest1","localhost",2552)
  ))

  def main(args: Array[String]): Unit = {
    system.actorOf(Props[Master], "subscriber")
  }
}
