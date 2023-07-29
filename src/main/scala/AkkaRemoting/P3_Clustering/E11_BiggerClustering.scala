package AkkaRemoting.P3_Clustering

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import Messages.FileOperation

object E11_BiggerClustering {

  def configNode(port: Int, role: String) = s"""
      |akka {
      |  remote.artery.canonical {
      |      port = $port
      |      hostname = localhost
      |  }
      |  cluster.roles = [$role]
      |}
      """.stripMargin

  def createNode(port: Int, role: String, props: Props) = {
    val config = ConfigFactory
      .parseString(configNode(port, role))
      .withFallback(ConfigFactory
      .load("AkkaRemoting/Part3Clustering/application.conf"))

    ActorSystem("clusterTest1", config)
      .actorOf(props, role)
  }

  def main(args: Array[String]): Unit = {
    val master = createNode(2551, "master", Props[Master])
    (2552 to 2554).foreach(createNode(_, "worker", Props[Worker]))

    Thread.sleep(15000)
    master ! FileOperation(
      "/media/alitariverdy/New Volume/Job/CodeDev/IdeaProjects/RocktheJVM-Scala-Focus/src/main/resources/data/cars.json",
      _.count(_ == '5')
    )

    Thread.sleep(1000)
    createNode(2555, "worker", Props[Worker])
  }
}
