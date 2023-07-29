package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part2LowLevelServerAPI.JSON.Guitar
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object Server extends AkkaHTTP.Part2LowLevelServerAPI.GuitarStoreJsonProtocol {
  implicit val system: ActorSystem = ActorSystem()

  import system.dispatcher

  def htmlBodyDecorator(text: String): String = s"""<html> <body> $text </body> </html>"""

  def pDecorator(text: String): String = s"""<p> $text </p>"""

  def formattedTime = s"${DateTime.now.hour}:${DateTime.now.minute}:${DateTime.now.second}"

  object GuitarDB {
    case class CreateGuitar(guitar: Guitar)

    case class GuitarCreated(id: Int)

    case class FindGuitar(id: Int)

    case object FindAll

    case class AddQuantity(id: Int, quantity: Int)

    case object GetQuantities
  }

  //case class Guitar(name: String, model: String)

  class GuitarDB extends Actor with ActorLogging {

    import GuitarDB._

    override def receive: Receive = guitarful(Map[Int, (Guitar, Int)](), 0)

    def guitarful(guitars: Map[Int, (Guitar, Int)], currentId: Int): Receive = {
      case CreateGuitar(guitar) =>
        log.info(s"Adding guitar: ${guitar.name}")
        sender() ! GuitarCreated(currentId + 1)
        context.become(guitarful(guitars + (currentId + 1 -> (guitar -> 0)), currentId + 1))
      case GetQuantities =>
        log.info(s"Getting quantities.")
        sender() ! guitars.values.toList
      case AddQuantity(id, quantity) =>
        log.info(s"Adding to guitar quantity.")
        val oldRecord = guitars.get(id)
        if (oldRecord.isDefined) context.become(guitarful(guitars.updated(id, oldRecord.get._1 ->
          (oldRecord.get._2 + quantity)), currentId + 1))
      case FindAll =>
        log.info("Getting all guitars.")
        sender() ! guitars.values.toMap.keys.toList
      case FindGuitar(id) =>
        log.info(s"Searching for ID: $id")
        sender() ! Option(guitars(id)._1)
    }
  }

  def main(args: Array[String]): Unit = {
    val guitarActor = system.actorOf(Props[GuitarDB], "guitarActor")
    val fenderStratocaster = Guitar("Fender", "Stratocaster")
    val gibsonLesPaul = Guitar("Gibson", "Les Paul")

    import GuitarDB._
    guitarActor ! CreateGuitar(fenderStratocaster)
    guitarActor ! CreateGuitar(gibsonLesPaul)

    import akka.http.scaladsl.server.Directives._
    implicit val timeout: Timeout = Timeout(2 seconds)

    def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

    val betterApi = pathPrefix("api" / "guitar") {
      (path("inventory") & get) {
        parameter(Symbol("stock").as[Boolean]) { stock =>
          complete((guitarActor ? GetQuantities)
            .mapTo[List[Tuple2[Guitar, Int]]]
            .map(_.filter(g => if (stock) g._2 > 0 else g._2 == 0))
            .map(_.toJson.prettyPrint)
            .map(toHttpEntity)
          )
        }
      }
    } ~ (path(IntNumber) | parameter(Symbol("id").as[Int])) { id =>
      complete((guitarActor ? FindGuitar(id)).mapTo[Option[Guitar]]
        .map(_.toJson.prettyPrint)
        .map(toHttpEntity)
      )
    } ~ get {
      complete((guitarActor ? FindAll).mapTo[List[Guitar]]
        .map(_.toJson.prettyPrint)
        .map(toHttpEntity)
      )
    }

    val server = Http().newServerAt("localhost", 12345).bind(betterApi)

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
