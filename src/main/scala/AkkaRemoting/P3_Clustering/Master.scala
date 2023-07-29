package AkkaRemoting.P3_Clustering

import AkkaRemoting.P3_Clustering.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Random, Success}

class Master extends Actor with ActorLogging {

  implicit private val timeout = Timeout(25 seconds)
  private val cluster = Cluster(context.system)
  import context.dispatcher

  private var workers: Map[Member, ActorRef] = Map.empty
  private var pendingJoin: Map[Member, ActorRef] = Map.empty
  private var pendingRemoval: Map[Member, ActorRef] = Map.empty

  private var counter = 0

  override def receive: Receive = handleClusterEvents

  def handleClusterEvents: Receive = {

    case MemberJoined(member) if member.hasRole("worker") =>
      log.info(s"[SUBSCRIBER] New member joined: ${member.address}")
      context.actorSelection(s"${member.address}/user/worker")
        .resolveOne().map(actor => MemberJoining(member, actor)) onComplete {
        case Success(msg) => self ! msg
      }

    case MemberUp(member) if member.hasRole("worker") =>
      log.info(s"[SUBSCRIBER] New member up: ${member.address}")
      if (pendingRemoval.contains(member)) {
        log.info(s"[SUBSCRIBER] Member ${member.address} is pending removal, removing it from pending removal list")
        pendingJoin -= member
        pendingRemoval -= member
//        context.become(handleClusterEvents(pendingJoin - member, pendingRemoval - member))
      } else if (pendingJoin.contains(member)) context.actorSelection(s"${member.address}/user/worker")
          .resolveOne().map(actor => MemberWaking(member, actor)) onComplete {
        case Success(msg) => self ! msg
      }

    case MemberRemoved(member, previousStatus) if member.hasRole("worker")  =>
      log.info(s"[SUBSCRIBER] Member '${member.address}' removed from $previousStatus")
      workers = workers.filterNot(_._1 == member)
      pendingRemoval -= member
//      context.become(handleClusterEvents(pendingJoin, pendingRemoval - member))

    case UnreachableMember(member) if member.hasRole("worker")  =>
      log.info(s"[SUBSCRIBER] Member '${member.address}' is unreachable.")
      pendingRemoval += (member -> workers.filter(_._1 == member).head._2)
//      context.become(handleClusterEvents(pendingJoin, pendingRemoval + (member -> workers.filter(_._1 == member).head._2)))

    /**
     * These are custom messages that are sent to the master to handle the cluster events.
     */
    case MemberWaking(member: Member, actor: ActorRef) =>
      log.info(s"[SUBSCRIBER] Worker is joined and up: ${member.address}")
      workers += (member -> actor)
//      context.become(handleClusterEvents(pendingJoin, pendingRemoval))

    case MemberJoining(member: Member, actor: ActorRef) =>
      log.info(s"[SUBSCRIBER] Worker is about to join: ${member.address}")
      pendingJoin += (member -> actor)
//      context.become(handleClusterEvents(pendingJoin + (member -> actor), pendingRemoval))

    /**
     * These are messages to do operations in the workers.
     */
    case FileOperation(file, operation) =>
      val openedFile = scala.io.Source.fromFile(file)
      log.info("[SUBSCRIBER] Received file operation for file: " + file.split('/').last)
      openedFile.getLines()
        .foreach { line =>
          val actor = workers.selectRoundRobin
          log.info(s"[SUBSCRIBER] Sending line operation to worker $actor: '$line'")
          Thread.sleep(500)
          actor ! RunOperation(line, operation)
      }
      openedFile.close()

    case OperationResult(result: Int) =>
      log.info(s"[SUBSCRIBER] Operation new result: $result")

    case event: MemberEvent =>
      log.info(s"[SUBSCRIBER] Event of type '${event.getClass.getName}' has happened.")
  }

  override def preStart(): Unit = cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  implicit private class EnrichMap(map: Map[Member, ActorRef]) {
    def selectRandom: ActorRef = Random.shuffle(map.values).tail.head

    def selectRoundRobin: ActorRef = {
      val list = map.values.toList
      counter = (counter + 1) % list.length
      list(counter)
    }
  }
}
