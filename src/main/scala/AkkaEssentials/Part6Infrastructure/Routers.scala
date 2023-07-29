package AkkaEssentials.Part6Infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object SimpleRouter {
  class Master extends Actor with ActorLogging {
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave-$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    val router: Router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = withRouter(router)

    def withRouter(newRouter: Router): Receive = {
      case message: String =>
        newRouter.route(message, sender())
      case Terminated(ref) =>
        val newRoutee = context.actorOf(Props[Slave])
        val changedRouter = router.removeRoutee(ref).addRoutee(newRoutee)
        context.watch(newRoutee)
        context.become(withRouter(changedRouter))
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String =>
        log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("simple")
    val master = system.actorOf(Props[Master])

    val ops = List("hello", "hi", "salam", "marhaba", "shalum", "dakh")
    ops.foreach(x => master !x)
  }
}


object PoolRouter {
  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String =>
        log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val ops = (1 to 10).toList.map(x => s"msg-$x")

    val system1 = ActorSystem("simplePool")
    val poolMaster1 = system1.actorOf(RoundRobinPool(5).props(Props[Slave]), "poolMaster1")
    // ops.foreach(x => poolMaster1 ! x)

    val system2 = ActorSystem("configPool", ConfigFactory.load().getConfig("routersDemo"))
    val poolMaster2 = system2.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
    ops.foreach(x => poolMaster2 ! x)
  }
}


object GroupRouter {
  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String =>
        log.info(message)
    }
  }

  def main(args: Array[String]): Unit = {
    val ops = (1 to 10).toList.map(x => s"msg-$x")

    val system1 = ActorSystem("pool1")
    val slaves1 = (1 to 5).toList.map(x => system1.actorOf(Props[Slave], s"slave-$x"))
    val slavePaths = slaves1.map(x => x.path.toString)
    val groupRouter = system1.actorOf(RoundRobinGroup(slavePaths).props())
    // ops.foreach(x => groupRouter ! x)

    val system2 = ActorSystem("pool2", ConfigFactory.load().getConfig("routersDemo"))
    val groupMaster2 = system2.actorOf(FromConfig.props(), "groupMaster2")
    ops.foreach(x => groupMaster2 ! x)
  }
}