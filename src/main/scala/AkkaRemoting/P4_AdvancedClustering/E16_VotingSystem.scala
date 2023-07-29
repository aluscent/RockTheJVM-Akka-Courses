package AkkaRemoting.P4_AdvancedClustering

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, ReceiveTimeout}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.ConfigFactory

import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/** Here we implement a voting system with multiple voting stations and aggregators
 */

case class Person(id: Int, age: Int)

case object Person {
  def generate(): Person = Person(Random.between(1111111, 9999999), Random.between(12, 80))

  def generateBulk(n: Int): List[Person] = (1 to n).toList.map(_ => generate())
}

case class Vote(person: Person, candidate: String)

case object VoteAccepted

case class VoteRejected(reason: String)

case object GetVotes

class VotingAggregator extends Actor with ActorLogging {
  val candidates: Set[String] = Set("Martin", "Roland", "Jonas")

  context.setReceiveTimeout(60 seconds)

  override def receive: Receive = online(Set.empty, Map.empty)

  private def online(peopleVoted: Set[Person], polls: Map[String, Int]): Receive = {
    case Vote(person, candidate) =>
      if (peopleVoted.contains(person)) sender() ! VoteRejected("Person has voted.")
      else if (person.age < 18) sender() ! VoteRejected("Person illegal age.")
      else if (!candidates.contains(candidate)) sender() ! VoteRejected("No such candidate.")
      else {
        sender() ! VoteAccepted
        context.become(online(
          peopleVoted + person,
          polls.updated(candidate, polls.getOrElse(candidate, 0) + 1)
        ))
      }
    case GetVotes =>
      sender() ! s"Results to this moment: \n\t" + polls.mkString("\n\t")
    case ReceiveTimeout =>
      log.info(s"Vote closed.\nFinal results: \n\t" + polls.mkString("\n\t"))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(offline(polls))
  }

  private def offline(finalPolls: Map[String, Int]): Receive = {
    case _: Vote =>
      log.info("Poll closed.")
      sender() ! VoteRejected("Poll closed.")
    case GetVotes =>
      sender() ! s"Final results are: " + finalPolls.mkString("\n\t")
  }
}

class VotingStation(aggregator: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case v: Vote => aggregator ! v
    case VoteAccepted => log.info("Vote accepted.")
    case VoteRejected(reason) => log.info(s"Vote rejected because: $reason")
    case GetVotes => aggregator ! GetVotes
    case s: String => log.info(s"[MESSAGE] $s")
  }
}

object VotingStation {
  def props(aggregator: ActorRef) = Props(new VotingStation(aggregator))
}

class ElectionSystem(port: Int) extends App {
  val conf = new File("src/main/scala/AkkaRemoting/P4_AdvancedClustering/votingCluster.conf")
  val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.parseFile(conf))

  val system = ActorSystem("alitasl", config)

  system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props[VotingAggregator],
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)
    ), "aggregator")
}

object aggregator1 extends ElectionSystem(12345)

object aggregator2 extends ElectionSystem(12346)

object aggregator3 extends ElectionSystem(12347)

class VotingMachine extends App {
  val conf = new File("src/main/scala/AkkaRemoting/P4_AdvancedClustering/votingCluster.conf")
  val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=0""")
    .withFallback(ConfigFactory.parseFile(conf))

  val system = ActorSystem("alitasl", config)

  val proxy = system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/aggregator",
      settings = ClusterSingletonProxySettings(system)
    ), "aggregatorProxy")

  val newVotingMachine = system.actorOf(VotingStation.props(proxy))

  scala.io.Source.stdin.getLines() foreach {
    case "getVotes" => newVotingMachine ! GetVotes
    case s: String => newVotingMachine ! Vote(Person.generate(), s)
  }
}

object machine1 extends VotingMachine

object machine2 extends VotingMachine

object machine3 extends VotingMachine

/**
 * In this solution, if we lose the actor doing the singleton job, the state will be lost.
 * In order to overcome this obstacle, we need to use Akka Persistence.
 */
