package AkkaPersistence.P2_Primer

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.pattern.ask
import akka.persistence.PersistentActor
import Directory.WebpageToolkit._
import akka.http.scaladsl.server.StandardRoute

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object E4_Exercise {

  case class Vote(citizen: Int, candidate: String)
  case class VotePersisted(id: String, vote: Vote)

  trait Result
  case object VoteRejected extends Result
  case object VoteAccepted extends Result

  def addVote(vote: String, votes: Map[String, Int]): Map[String, Int] = votes + (vote -> (votes.getOrElse(vote, 0) + 1))

  def formattedTime: String = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date)

  class votingMachine extends PersistentActor with ActorLogging {
    var totalAmount: Map[String, Int] = Map[String, Int]()
    var votedCitizens: Set[Int] = Set[Int]()

    override def persistenceId: String = "votingMachine"

    override def receiveCommand: Receive = {
      case vote@Vote(citizen, candidate) =>
        if (votedCitizens.contains(citizen)) {
          sender() ! VoteRejected
        } else {
          persist(VotePersisted(UUID.randomUUID().toString, vote)) { event =>
            log.info(s"Persisted: ${event.vote}")
            sender() ! VoteAccepted
            totalAmount = addVote(candidate, totalAmount)
            votedCitizens = votedCitizens + citizen
          }
        }
    }

    override def receiveRecover: Receive = {
      case VotePersisted(_, vote) =>
        log.info(s"Recovered: $vote")
        totalAmount = addVote(vote.candidate, totalAmount)
        votedCitizens = votedCitizens + vote.citizen
    }
  }

  def defaultPage(candidates: List[String]) = buildHtml(
    "<title>Voting Machine</title>",
    buildForm(List(
      buildInput("number", "citizenPID"),
      buildRadio(
        "candidateName",
        candidates
      )
    ))
  )

  def htmlBodyDecorator(text: String): String = buildHtml(body = text)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("Persistence")
    val actor = system.actorOf(Props[votingMachine], "test")
    implicit val timeout: akka.util.Timeout = akka.util.Timeout(5000 millis)
    import akka.http.scaladsl.server.Directives._
    import system.dispatcher
    val candidates = List("Bob", "Alice", "Charlie")

    val chainedRoute = (parameter(Symbol("citizenPID").as[String]) | path(Segment)) { citizen =>
      (parameter(Symbol("candidateName").as[String]) | path(Segment)) { candidateName =>
        val startTime = System.currentTimeMillis()
        Await.result(actor ? Vote(citizen.toInt, candidateName), 2500 millis) match {
          case VoteAccepted if candidates.contains(candidateName) =>
            val totalTime = System.currentTimeMillis() - startTime
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlBodyDecorator(s"Vote for $candidateName accepted @ $totalTime ms")))
          case VoteRejected =>
            val totalTime = System.currentTimeMillis() - startTime
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlBodyDecorator(s"Vote for $candidateName rejected @ $totalTime ms")))
          case _ =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlBodyDecorator(s"Error @ $formattedTime")))
        }
      }
    } ~ get {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, defaultPage(candidates)))
    }

    Http().newServerAt("localhost", 12345).bind(chainedRoute)
  }
}
