package AkkaRemoting.P3_Clustering

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import java.io.File

case class SimpleTask(contents: String)

case object StartWork

class MasterWithRouter extends Actor with ActorLogging {
  val router = context.actorOf(FromConfig.props(Props[Routee]), "CAR")

  override def receive: Receive = {
    case StartWork =>
      (1 to 100) foreach { id =>
        router ! SimpleTask(s"[${self.path}] simpleTask-$id")
      }
  }
}

class Routee extends Actor with ActorLogging {
  override def receive: Receive = {
    case SimpleTask(contents) =>
      log.info(s"[${self.path}] processing: $contents")
  }
}

object RouteesApp extends App {
  def startRouteeNode(port: Int) = {
    val conf = new File("src/main/scala/AkkaRemoting/P3_Clustering/clusterAwareRouters.conf")
    val config = ConfigFactory.parseFile(conf)
      .withFallback(ConfigFactory.parseString(
        s"""akka.remote.artery.canonical.port=$port""".stripMargin))

    val system = ActorSystem("alitasl", config)
    system.actorOf(Props[Routee], "worker")
  }

  startRouteeNode(12345)
  startRouteeNode(12346)
}

object MasterWithRouteesApp extends App {
  def startMasterNode() = {
    val conf = new File("src/main/scala/AkkaRemoting/P3_Clustering/clusterAwareRouters.conf")
    val config = ConfigFactory.parseFile(conf).getConfig("masterWithGroupRouterApp")
      .withFallback(ConfigFactory.parseFile(conf))

    val system = ActorSystem("alitasl", config)
    val masterActor = system.actorOf(Props[MasterWithRouter], "master")

    Thread.sleep(10000)
    masterActor ! StartWork
  }

  startMasterNode()
}
