package AkkaRemoting.P3_Clustering

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object E9_Clustering {

  def startCluster(ports: Int*): Unit = ports.foreach { port =>
    val config = ConfigFactory.parseString(s"akka.remote.artery.canonical.port = $port")
      .withFallback(ConfigFactory.load("AkkaRemoting/Part3Clustering/application.conf"))

    ActorSystem("clusterTest1", config) // All actor systems in a cluster must have the same name
      .actorOf(Props[Master], "subscriber")
  }

  def startCluster(count: Int): Unit = (1 to count).foreach(_ => startCluster(ports = 0))

  def main(args: Array[String]): Unit = {
    startCluster(2551, 2552)
//    startCluster(3)
  }
}