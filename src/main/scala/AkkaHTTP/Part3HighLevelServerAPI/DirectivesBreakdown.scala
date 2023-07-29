package AkkaHTTP.Part3HighLevelServerAPI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._

import java.lang.Runnable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object DirectivesBreakdown {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def htmlBodyDecorator(text: String): String = s"""<html> <body> $text </body> </html>"""
  def pDecorator(text: String): String = s"""<p> $text </p>"""

  def formattedTime = s"${DateTime.now.hour}:${DateTime.now.minute}:${DateTime.now.second}"

  def main(args: Array[String]): Unit = {
    val simpleHttpMethodRoute = post {
      complete(StatusCodes.Forbidden)
    }

    val simplePathRoute = path("about") {
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        htmlBodyDecorator("About page.")
      ))
    }

    val complexPath = path("api" / "endpoint") {
      complete(StatusCodes.OK)
    } ~ path("api" / "endpoint" / IntNumber / IntNumber) { (itemNumber1,itemNumber2) =>
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        htmlBodyDecorator(pDecorator(s"Your number: $itemNumber1") + pDecorator(s"Your second number: $itemNumber2"))
      ))
    } ~ path("api" / "endpoint" / IntNumber) { (itemNumber1) =>
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        htmlBodyDecorator(pDecorator(s"Your number: $itemNumber1"))
      ))
    }

    val queriedPath = path("api" / "items") {
      parameter("id") { id =>
        complete(HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          htmlBodyDecorator(pDecorator(s"Item number: $id"))
        ))
      }
    }

    val extractedPath = path("api" / "req") {
      extractRequest { httpReq: HttpRequest =>
        complete(StatusCodes.OK)
      }
    }

    val server = Http().newServerAt("localhost", 12345).bind(complexPath)

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
