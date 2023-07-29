package AkkaHTTP.Part3HighLevelServerAPI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives

object Intro {
  implicit val system: ActorSystem = ActorSystem("highlevelApi")
  import system.dispatcher

  def htmlBodyDecorator(text: String): String =
    s"""
       |<html> <body>
       |$text
       |</body> </html>
       |""".stripMargin

  def main(args: Array[String]): Unit = {
    // Directives
    import akka.http.scaladsl.server.Directives._
    val simpleRoute =  // this is a Route
      path("home") { // directive - filters the requests
      complete(StatusCodes.OK) // directive - tells how to respond to each request
    }

    val homeGetRoute = path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

    val chainedRoute = path("home") {
      get {
        complete(StatusCodes.OK)
      } ~ // if you forget tilda, code will work, but Scala compiler enforces the last expression inside block
        post {
          complete(HttpEntity (
            ContentTypes.`text/html(UTF-8)`,
            htmlBodyDecorator("You can get list of guitars here.")
          ))
        }
    } ~ path("guitar") {
      get {
        complete(StatusCodes.Forbidden)
      }
    }

    Http().newServerAt("localhost", 12345).bind(chainedRoute)
  }
}
