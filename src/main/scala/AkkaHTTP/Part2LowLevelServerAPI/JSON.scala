package AkkaHTTP.Part2LowLevelServerAPI

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object JSON extends GuitarStoreJsonProtocol {
  implicit val system: ActorSystem = ActorSystem("json2")
  import system.dispatcher

  object GuitarDB {
    case class CreateGuitar(guitar: Guitar)
    case class GuitarCreated(id: Int)
    case class FindGuitar(id: Int)
    case object FindAll
    case class AddQuantity(id: Int, quantity: Int)
    case object GetQuantities
  }

  case class Guitar(name: String, model: String)
  class GuitarDB extends Actor with ActorLogging {
    import GuitarDB._

    override def receive: Receive = guitarful(Map[Int,Tuple2[Guitar,Int]](), 0)

    def guitarful(guitars: Map[Int,Tuple2[Guitar,Int]], currentId: Int): Receive = {
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

  implicit val timeout: Timeout = Timeout(3 seconds)
  import GuitarDB._

  def main(args: Array[String]): Unit = {
    val guitarActor = system.actorOf(Props[GuitarDB], "guitarActor")
    val fenderStratocaster = Guitar("Fender", "Stratocaster")
    val gibsonLesPaul = Guitar("Gibson", "Les Paul")

    guitarActor ! CreateGuitar(fenderStratocaster)
    guitarActor ! CreateGuitar(gibsonLesPaul)

    def requestHandler(): HttpRequest => Future[HttpResponse] = {
      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
        val query = uri.query()
        if (query.isEmpty) {
          val guitars: Future[List[Guitar]] = (guitarActor ? FindAll).mapTo[List[Guitar]]
          guitars map { guitar =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            )
          }
        } else {
          val guitar = (guitarActor ? FindGuitar(query.get("id").map(x => x.toInt).get)).mapTo[Option[Guitar]]
          guitar map {
            case None => HttpResponse(StatusCodes.NotFound)
            case Some(value) =>
              HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, value.toJson.prettyPrint))
            case _ => HttpResponse(StatusCodes.BadRequest)
          }
        }
      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
        val query = uri.query()
        if (query.isEmpty) {
          val guitars: Future[List[Tuple2[Guitar, Int]]] = (guitarActor ? GetQuantities).mapTo[List[Tuple2[Guitar, Int]]]
          guitars map { guitar =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                guitar.mkString(", ")
              )
            )
          }
        } else {
          guitarActor ! AddQuantity(query.get("id").map(x => x.toInt).get,query.get("quantity").map(x => x.toInt).get)
          Future(HttpResponse(StatusCodes.OK))
        }
      case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
        val strict = entity.toStrict(30 seconds)
        strict.flatMap { strictEntity =>
          val guitar = strictEntity.data.utf8String.parseJson.convertTo[Guitar]
          val guitarCreatedFuture = (guitarActor ? CreateGuitar(guitar)).mapTo[GuitarCreated]
          guitarCreatedFuture map { _ =>
            println("New guitar added.")
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "New guitar added."
              )
            )
          }
        }
      case HttpRequest(HttpMethods.GET, Uri.Path("/test"), _, _, _) =>
        Future { HttpResponse(
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "Welcome to test."
          )
        )}
      case request: HttpRequest =>
        request.discardEntityBytes()
        Future { HttpResponse (
          status = StatusCodes.NotFound
        )}
    }

    val server = Http().newServerAt("localhost", 12345).bind(requestHandler())
    server.onComplete{
      case Success(value) => println(s"Server is up: $value")
      case Failure(exception) => println(s"Server down. $exception")
    }
  }
}