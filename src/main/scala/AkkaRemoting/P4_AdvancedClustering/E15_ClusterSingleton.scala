package AkkaRemoting.P4_AdvancedClustering

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.ConfigFactory

import java.io.File
import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random


/**
 * A small payment system - it is centralized:
 * - well-defined transaction ordering
 * - interacting with legacy system
 */

case class Order(items: List[String], total: Double)

case class Transaction(orderId: Int, txnId: String, amount: Double)

class PaymentSystem extends Actor with ActorLogging {
  override def receive: Receive = {
    case t: Transaction =>
      log.info(s"Validating transaction $t")
  }
}

class PaymentSystemNode(port: Int) extends App {
  val conf = new File("src/main/scala/AkkaRemoting/P4_AdvancedClustering/clusterSingleton.conf")
  val config = ConfigFactory.parseFile(conf)
    .withFallback(ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port"""))

  val system = ActorSystem("alitasl", config)
  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props[PaymentSystem],
    terminationMessage = PoisonPill,
    ClusterSingletonManagerSettings(system)
  ), "paymentSystem")
}

object Node1 extends PaymentSystemNode(12345)

object Node2 extends PaymentSystemNode(12346)

object Node3 extends PaymentSystemNode(12347)

class OnlineShopCheckout(paymentSystem: ActorRef) extends Actor with ActorLogging {
  var orderId = 0

  override def receive: Receive = {
    case Order(_, total) =>
      log.info(s"Received order $orderId with amount $total")
      val txn = Transaction(
        orderId = orderId,
        txnId = UUID.randomUUID().toString,
        amount = total)
      paymentSystem ! txn
      orderId += 1
  }
}

object OnlineShopCheckout {
  def props(paymentSystem: ActorRef) = Props(new OnlineShopCheckout(paymentSystem))
}

class PaymentSystemClient extends App {
  val conf = new File("src/main/scala/AkkaRemoting/P4_AdvancedClustering/clusterSingleton.conf")
  val config = ConfigFactory.parseFile(conf)
    .withFallback(ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=0"""))

  val system = ActorSystem("alitasl", config)

  val proxy = system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/paymentSystem",
      settings = ClusterSingletonProxySettings(system)
    ), "paymentSystemProxy")

  val onlineShopCheckout = system.actorOf(OnlineShopCheckout.props(proxy))

  implicit val ec = system.dispatcher

  system.scheduler.scheduleAtFixedRate(5 seconds, 1 seconds)(() => {
    val randomOrder = Order(List(), Random.nextDouble() * 100)
    onlineShopCheckout ! randomOrder
  })
}

object initiator extends PaymentSystemClient
