package AkkaStreams.P4_TechniquesPatterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.Timeout
import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object ExternalServiceIntegration {
  implicit val system = ActorSystem("externalServiceIntegration")
  implicit val dispatcher = system.dispatchers.lookup("dedicated")
  implicit val timeout: Timeout = Timeout.durationToTimeout(2 second)

  case class PagerEvent(application: String, description: String, date: Date)

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("john", "daniel")
    private val emails = Map("john" -> "john@some.com", "daniel" -> "daniel@fella.com")


    private def ProcessEvent(pagerEvent: PagerEvent) = {
      val engineerIndex: Int = ((pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length).toInt

      val engineer = engineers(engineerIndex)
      val engineerEmail = emails(engineer)

      // page the engineer
      log.info(s"Sending the engineer $engineer an email with high priority: $pagerEvent")
      Thread.sleep(1000)

      // return the email
      engineerEmail
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! ProcessEvent(pagerEvent)
    }
  }

  // simple external service
  def genericService[A,B](element: A): Future[B] = ???

  def main(args: Array[String]): Unit = {
    val eventSource = Source(List(
      PagerEvent("AkkaInfra", "Broke", new Date),
      PagerEvent("DataPipeline", "IllegalElements", new Date),
      PagerEvent("AkkaInfra", "ServiceStopped", new Date),
      PagerEvent("Frontend", "ButtonUnfuntioned", new Date)
    ))

    val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
    val infraEvents = eventSource.filter(x => x.application == "AkkaInfra")
    //val pagedEngineersEmailed = infraEvents.mapAsync(1)(event => PagerService.ProcessEvent(event))
    val altPagedEngineersEmailed = infraEvents.mapAsync(2)(event => (pagerActor ? event).mapTo[String])
    val pagedEmailsSink = Sink.foreach[String](x => println(s"Notification sent for: $x"))

    //pagedEngineersEmailed.to(pagedEmailsSink).run()
    altPagedEngineersEmailed.to(pagedEmailsSink).run()
  }
}
