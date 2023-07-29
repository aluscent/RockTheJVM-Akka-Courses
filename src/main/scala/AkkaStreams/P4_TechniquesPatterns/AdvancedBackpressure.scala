package AkkaStreams.P4_TechniquesPatterns

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

import java.util.Date
import scala.concurrent.duration._
import scala.language.postfixOps

object AdvancedBackpressure {
  implicit val system = ActorSystem("advBackpressure")

  case class PagerEvent(desciption: String, date: Date, nInstances: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  def sendEmail(notification: Notification) =
    println(s"dear ${notification.email}, you have an event: ${notification.pagerEvent.desciption}")

  // un-backpressurable source
  def sendEmailSlow(notification: Notification) = {
    Thread.sleep(1000)
    println(s"dear ${notification.email}, you have an event: ${notification.pagerEvent.desciption}")
  }

  def main(args: Array[String]): Unit = {
    // control backpressure in most simple way
    val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)

    val events = List(
      PagerEvent("service discovery failed.", new Date),
      PagerEvent("illegal elements.", new Date),
      PagerEvent("service down.", new Date),
      PagerEvent("number of HTTP500 spiked.", new Date),
      PagerEvent("service stopped responding.", new Date)
    )

    val eventSource = Source(events)
    val oncallEng = "daniel@rtj.com"
    val notificationSink = Flow[PagerEvent].map(event => Notification(oncallEng, event))
      .to(Sink.foreach[Notification](sendEmail))

    // This is the standard way
    //eventSource.to(notificationSink).run()

    val aggregationFlow = Flow[PagerEvent].conflate((e1, e2) =>
      PagerEvent(s"multiple events: ${e1.nInstances + e2.nInstances}", new Date, e1.nInstances + e2.nInstances)
    ).map(event => Notification(oncallEng, event))

    // slow consumer problem can be solved this way.
    //eventSource.via(aggregationFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()

    val slowEventSource = Source(events).throttle(1, 1 seconds)
    val extrapolationFlow = Flow[PagerEvent].extrapolate(event =>
      Iterator.iterate(PagerEvent(event.desciption + " (!)", event.date, event.nInstances))
      (e => PagerEvent(e.desciption + " (!)", e.date, e.nInstances))
    ).map(event => Notification(oncallEng, event)) // this flow meets unmet demand of sink
    slowEventSource.via(extrapolationFlow).to(Sink.foreach[Notification](sendEmail)).run()

    val expansionFlow = Flow[PagerEvent].expand(event =>
      Iterator.iterate(PagerEvent(event.desciption + " (!)", event.date, event.nInstances))
      (e => PagerEvent(e.desciption + " (!)", e.date, e.nInstances))
    ).map(event => Notification(oncallEng, event)) // this flow expands flow with no attention to demand
  }
}
