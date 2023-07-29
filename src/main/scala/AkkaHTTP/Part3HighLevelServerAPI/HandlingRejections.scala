package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.Server.formattedTime
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, Rejection, RejectionHandler}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object HandlingRejections {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    // Rejection handlers
    val badReqHandler1: RejectionHandler = { rejection: Seq[Rejection] =>
      println(s"Encountered rejection.  $rejection")
      Some(complete(StatusCodes.BadRequest))
    }
    val badReqHandler2: RejectionHandler = { rejection: Seq[Rejection] =>
      println(s"Encountered rejection.  $rejection")
      Some(complete(StatusCodes.Forbidden))
    }

    val simpleRoute = handleRejections(badReqHandler1) {
      path("api" / "endpoint") {
        get {
          complete(StatusCodes.OK)
        } ~ post {
          handleRejections(badReqHandler2) {
            parameter(Symbol("name")) { _ =>
              complete(StatusCodes.BadGateway)
            }
          }
        }
      }
    }

    implicit val customRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
      .handle {
        case MissingQueryParamRejection(parameterName) =>
          println(s"Missing param: $parameterName")
          complete(s"Missing param: $parameterName")
        case MethodRejection(supported) =>
          println(s"Rejected method: $supported")
          complete(s"Rejected method: $supported")
      }
      .result()

    val simplePath = path("api" / "endpoint") {
      get {
        complete(StatusCodes.OK)
      } ~ parameter(Symbol("id").as[Int]) { _ =>
        complete(StatusCodes.OK)
      }
    }


    /**
     * Test run area
     */
    val server = Http().newServerAt("localhost", 12345).bind(simplePath)

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
