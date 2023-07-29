package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.Server.formattedTime
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat, jsonFormat3, listFormat}
import spray.json.{RootJsonFormat, enrichAny, enrichString}

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}

case class Player(nickname: String, characterClass: String, level: Int)

object GameAreaMap {
  case object GetAllPlayers
  case class GetPlayer(nickname: String)
  case class GetPlayerByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccessAck
}

class GameAreaMap extends Actor with ActorLogging {
  override def receive: Receive = Stateful(Map[String,Player]())

  import GameAreaMap._
  def Stateful(players: Map[String, Player]): Receive = {
    case GetAllPlayers =>
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      sender() ! players.get(nickname)

    case GetPlayerByClass(characterClass) =>
      sender() ! players.values.toList.filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      context.become(Stateful(players + (player.nickname -> player)))
      sender() ! OperationSuccessAck

    case RemovePlayer(player) =>
      context.become(Stateful(players - player.nickname))
      sender() ! OperationSuccessAck
  }
}

trait GameJsonConverters {
  implicit val playerFormat: RootJsonFormat[Player] = jsonFormat3(Player)
  implicit def playerHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)
}

object MarshallingJSON extends GameJsonConverters {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def GameHandler(map: ActorRef): Route = {
    import akka.http.scaladsl.server.Directives._
    import GameAreaMap._
    implicit val timeout: Timeout = Timeout(5 seconds)

    pathPrefix("api" / "players") {
      path("class" / Segment) { clazz =>
        complete(
          (map ? GetPlayerByClass(clazz)).mapTo[List[Player]]
            .map(list => list.toJson.prettyPrint)
        )
      } ~
      (parameter(Symbol("nickname").as[String]) | path(Segment)) { nickname =>
        complete(
          (map ? GetPlayer(nickname)).mapTo[Option[Player]]
            .map(_.get.toJson.prettyPrint)
        )
      } ~ get {
        complete(
          (map ? GetAllPlayers).mapTo[List[Player]]
            .map(list => list.toJson.prettyPrint)
        )
      } ~ (post & extractLog & pathEndOrSingleSlash & extractRequest) { (_, payload) =>
        val semiRawPlayer = payload.entity.toStrict(5 seconds)
          .map(player => player.data.utf8String.parseJson.convertTo[Player])

        onComplete(semiRawPlayer) {
          case Success(value) =>
            map ! AddPlayer(value)
            complete(s"Player added: ${value.nickname}")
          case Failure(exception) => failWith(exception)
        }
      } ~ (delete & extractLog & pathEndOrSingleSlash & extractRequest) { (_, payload) =>
        val semiRawPlayer = payload.entity.toStrict(5 seconds)
          .map(player => player.data.utf8String.parseJson.convertTo[Player])

        onComplete(semiRawPlayer) {
          case Success(value) =>
            map ! RemovePlayer(value)
            complete(s"Player removed: ${value.nickname}")
          case Failure(exception) => failWith(exception)
        }
      }
    }

  }

  def main(args: Array[String]): Unit = {
    val map = system.actorOf(Props[GameAreaMap], "map")
    val players = List(
      Player("jj", "hero", 70),
      Player("dani", "hero", 60)
    )

    import GameAreaMap.AddPlayer
    val gameHandler = GameHandler(map)
    players.foreach(player => map ! AddPlayer(player))

    val server = Http().newServerAt("localhost", 12345).bind(gameHandler)

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
