package AkkaHTTP.Part3HighLevelServerAPI

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object Exercise extends PersonJsonConverter {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def toHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)
  def formattedTime = s"${DateTime.now.hour}:${DateTime.now.minute}:${DateTime.now.second}"

  case class Person(PIN: Int, name: String)

  object Database {
    case object GetAllPeople
    case class AddPerson(person: Person)
    case class GetPerson(id: Int)
  }

  class Database extends Actor with ActorLogging {
    override def receive: Receive = Stateful(Map[Int,Person](), 0)

    import Database._
    def Stateful(people: Map[Int,Person], currentId: Int): Receive = {
      case GetAllPeople =>
        sender() ! people.values.toList
      case GetPerson(id) =>
        sender() ! people.get(id)
      case AddPerson(person) =>
        context.become(Stateful(people + (currentId -> person), currentId + 1))
      case _ =>
    }
  }

  def main(args: Array[String]): Unit = {
    val db = system.actorOf(Props[Database], "db")
    implicit val timeout: Timeout = Timeout(2 seconds)

    import Database._
    val people = List(
      Person(121, "john"),
      Person(122, "simin"),
      Person(123, "alex")
    )
    people.foreach(db ! AddPerson(_))

    val GovernmentAPI = pathPrefix("api" / "people") {
      (path(IntNumber) | parameter(Symbol("pin").as[Int])) { id =>
        complete((db ? GetPerson(id))
          .mapTo[Option[Person]]
          .map(_.get.toJson.prettyPrint)
          .map(toHttpEntity)
        )
      } ~
      (post & extractLog & pathEndOrSingleSlash & extractRequest) { (_, payload) =>
        val rawPerson = payload.entity.toStrict(5 seconds)
          .map(person => person.data.utf8String.parseJson.convertTo[Person])

        onComplete(rawPerson) {
          case Success(value) =>
            db ! AddPerson(value)
            complete(StatusCodes.OK)
        }
      } ~
      get {
        complete((db ? GetAllPeople)
          .mapTo[List[Person]]
          .map(_.map(_.toJson.prettyPrint).mkString(", "))
          .map(toHttpEntity)
        )
      }
    }

    val server = Http().newServerAt("localhost", 12345).bind(GovernmentAPI)

    server onComplete {
      case Success(value) =>
        println(s"New server started: $value   @ $formattedTime")
        system.scheduler.scheduleAtFixedRate(5 seconds, 5 seconds) {
          new Runnable {
            def run(): Unit = println(s"Live server. $formattedTime")
          }
        }
    }
  }
}
