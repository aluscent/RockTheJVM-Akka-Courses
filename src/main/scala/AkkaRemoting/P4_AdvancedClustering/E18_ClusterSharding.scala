package AkkaRemoting.P4_AdvancedClustering

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import com.typesafe.config.ConfigFactory

import java.io.File
import java.util.{Date, UUID}
import scala.util.Random

/** Here I am developing the oyster cards system of London metro
 */

case class OysterCard(id: String, amount: Double)

case class EntryAttempt(oysterCard: OysterCard, date: Date)

case object EntryAccepted

case class EntryRejected(reason: String)

class Turnstile(validator: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case o: OysterCard => validator ! EntryAttempt(o, new Date)
    case EntryAccepted => log.info("Please enter.")
    case EntryRejected(reason) => log.info(s"Stop! $reason")
  }
}

object Turnstile {
  def props(validator: ActorRef) = Props(new Turnstile(validator))
}

class OysterCardValidator extends Actor with ActorLogging {
  override def preStart(): Unit = {
    super.preStart()
    log.info("Validator started.")
  }

  override def receive: Receive = {
    case EntryAttempt(OysterCard(id, amount), _) =>
      log.info(s"Validating oyster card $id.")
      if (amount > 2.5) sender() ! EntryAccepted
      else sender() ! EntryRejected(s"Please top up.\nID $id")
  }
}

/**
 * Sharding settings below
 */
object TurnstileSettings {
  val numberOfShards = 10 // use 10x times the number of nodes in cluster
  val numberOfEntities = 100 // 10x times the number of shards

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case attempt@EntryAttempt(OysterCard(id, _), _) =>
      val entityId = id.hashCode.abs % numberOfEntities
      (entityId.toString, attempt)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntryAttempt(OysterCard(id, _), _) =>
      val shardId = id.hashCode.abs % numberOfShards
      shardId.toString
  }
}

/**
 * Cluster nodes
 */
class TubeStation(port: Int, numberOfTurnstiles: Int) extends App {
  val conf = new File("src/main/scala/AkkaRemoting/P4_AdvancedClustering/clusterSharding.conf")
  val config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port = $port""")
    .withFallback(ConfigFactory.parseFile(conf))

  val system = ActorSystem("alitasl", config)

  // Setting up cluster sharding

  val validatorShardRegionRef: ActorRef = ClusterSharding(system).start(
    typeName = "OysterCardValidator",
    entityProps = Props[OysterCardValidator],
    settings = ClusterShardingSettings(system),
    extractEntityId = TurnstileSettings.extractEntityId,
    extractShardId = TurnstileSettings.extractShardId
  )

  val turnstiles = (1 to numberOfTurnstiles)
    .toList.map(_ => system.actorOf(Turnstile.props(validatorShardRegionRef)))

  Thread.sleep(10000)

  for (_ <- 1 to 1000) {
    val randomTurnstileIndex = Random.nextInt(numberOfTurnstiles)
    val randomTurnstile = turnstiles(randomTurnstileIndex)
    Thread.sleep(Random.nextInt(50))
    randomTurnstile ! OysterCard(UUID.randomUUID().toString, Random.nextDouble() * 20)
  }
}

object PiccadillyCircus extends TubeStation(12345, 3)

object CharingCross extends TubeStation(12355, 5)

object WestMinster extends TubeStation(12365, 8)
