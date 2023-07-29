package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.Server.formattedTime
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object HandlingExceptions {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    val simpleRoute = path("api" / "test") {
      get {
        throw new RuntimeException("No GET method!")
      } ~ post {
        parameter(Symbol("id")) { id =>
          if (id.length > 3) throw new RuntimeException("ID cannot be found!")
          else complete(StatusCodes.OK)
        }
      }
    }

    implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
      case re: RuntimeException =>
        complete(StatusCodes.NotFound, re.getMessage)
      case iae: IllegalArgumentException =>
        complete(StatusCodes.Forbidden, iae.getMessage)
    }




    /**
     * Test run area
     */
    val server = Http().newServerAt("localhost", 12345).bind(simpleRoute)

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
