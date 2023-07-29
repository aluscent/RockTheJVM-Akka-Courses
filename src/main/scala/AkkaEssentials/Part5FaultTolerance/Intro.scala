package AkkaEssentials.Part5FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object Intro {

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"[PARENT] starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"[PARENT] stopping child $name")
        val childHandle = children.get(name)
        childHandle.foreach(x => context.stop(x))
        context.become(withChildren(children.filter(x => x._1 != name)))
      case Stop =>
        log.info("[PARENT] stopping parent")
        context.stop(self)
      case message: String =>
        log.info("[PARENT] " + message)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info("[CHILD] " + message.toString)
    }
  }

  def main(args: Array[String]): Unit = {
    import Parent._

    val system = ActorSystem("introFaultTolerance")
    val parent = system.actorOf(Props[Parent], "parent")

    /* // solution 1: using context.stop
    parent ! StartChild("firstChild")
    val child = system.actorSelection("/user/parent/firstChild")
    child ! "hi new child!"

    parent ! StopChild("firstChild") // The child won't stop immediately. It takes a little bit of time. In this time, it can receive messages.

    parent ! StartChild("secondChild")
    val secondChild = system.actorSelection("/user/parent/secondChild")
    secondChild ! "hi secondChild"

    parent ! Stop

    for (i <- 1 to 100) {
      parent ! s"[$i] ***"
      secondChild ! s"[$i] -----"
    }
     */

    /* // solution 2: using signals
    val looseActor = system.actorOf(Props[Child])
    looseActor ! "hey loose actor!"
    looseActor ! PoisonPill
    looseActor ! "are still there?"

    val killedActor = system.actorOf(Props[Child])
    killedActor ! "hey actor [to be killed]!"
    killedActor ! Kill
    killedActor ! "are still there?"
     */

    // solution 3: death watch
    class Watcher extends Actor with ActorLogging {
      override def receive: Receive = {
        case StartChild(name) =>
          val child = context.actorOf(Props[Child], name)
          log.info(s"[WATCH] Started and watching child $name.")
          context.watch(child)
        case Terminated(ref) =>
          log.info(s"[WATCH] The child $ref been killed.")
      }
    }

    val watcher = system.actorOf(Props[Watcher], "watcher")
    watcher ! StartChild("firstWatchedChild")
    val watchedChild = system.actorSelection("/user/watcher/firstWatchedChild")
    Thread.sleep(500)
    watchedChild ! PoisonPill
  }
}
