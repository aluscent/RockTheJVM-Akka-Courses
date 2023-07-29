package AkkaRemoting.P3_Clustering

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Address, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import com.typesafe.config.ConfigFactory

import java.io.File

object ChatDomain {
  case class ChatMessage(nickname: String, content: String)

  case class UserMessage(content: String)

  case class EnterRoom(member: Member, nickname: String)
}

class ChatActor(nickname: String, port: Int) extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)
  // initialize cluster object

  cluster.joinSeedNodes(Seq(
    Address("akka", "alitasl", "localhost", 12345),
    Address("akka", "alitasl", "localhost", 12346)
  ))

  // subscribe to cluster events preStart()
  override def preStart(): Unit = cluster
    .subscribe(self, InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  // unsubscribe self in preStop()
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = online(Map.empty)

  def online(clusterMembers: Map[Member, String]): Receive = {
    case MemberUp(member) =>
      // send a special "enter room" message to chatActor on new node
      println(member.address)
      val remoteChatActor = getChatActor(member.address)
      remoteChatActor ! ChatDomain.EnterRoom(member, nickname)
    //      log.info(s"[Member up] [$name]")
    case MemberRemoved(member, _) =>
      // remove actor from data structures
      log.info(s"[Member removed] [${member.address.toString}]")
      context.become(online(clusterMembers - member))
    case ChatDomain.EnterRoom(member, nickname) =>
      // add member to data structures
      val selectedActor = member -> nickname
      log.info(s"[Enter room] [$nickname]")
      context.become(online(clusterMembers + selectedActor))
    case ChatDomain.UserMessage(content) =>
      // broadcast content as ChatMessages to the rest of clusters
      log.info(clusterMembers.toString())
      clusterMembers
        .filterNot(_._2 == nickname)
        .foreach { case (member, n) => getChatActor(member.address) ! ChatDomain.ChatMessage(n, content) }
    case ChatDomain.ChatMessage(nickname, content) =>
      log.info(s"[Message received] [${nickname.toUpperCase()}] $content")
  }

  def getChatActor(address: Address): ActorSelection =
    context.actorSelection(address.toString + "/user/chatActor")
}

object ChatActor {
  def props(nickname: String, port: Int) = Props(new ChatActor(nickname, port))
}

class ChatApp(nickname: String, port: Int) extends App {
  private val configFile = new File("src/main/scala/AkkaRemoting/P3_Clustering/clusterChat.conf")
  val config = ConfigFactory.parseFile(configFile)
    .withFallback(ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port = $port""".stripMargin))
  val system: ActorSystem = ActorSystem("alitasl", config)
  private val chatActor: ActorRef = system.actorOf(ChatActor.props(nickname, port), s"chatActor")

  scala.io.Source.stdin.getLines() foreach { line =>
    chatActor ! ChatDomain.UserMessage(line)
  }
}

object Alice extends ChatApp("Alice", 12345)

object Bob extends ChatApp("Bob", 12346)

object Charlie extends ChatApp("Charlie", 12347)
