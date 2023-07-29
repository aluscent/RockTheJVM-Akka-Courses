package AkkaHTTP.Part2LowLevelServerAPI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, ResponseEntity, StatusCodes, Uri}
import akka.stream.scaladsl.{Flow, Keep, Sink}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Intro {
  implicit val system: ActorSystem = ActorSystem("lowLevelAPI")
  import system.dispatcher

  case class HtmlElement(text: String = "") {
    override def toString: String = text
  }

  def htmlBodyDecorator(element: HtmlElement): HtmlElement =
    HtmlElement(s"""
      |<html>
      |<body>
      |${element.text}
      |</body>
      |</html>
    """.stripMargin)

  def divDecorator(attrib: String = "", element: HtmlElement): HtmlElement =
    HtmlElement(s"""
       |<div $attrib>
       |${element.text}
       |</div>
       |""".stripMargin)

  def reqToResFuture(failure: String, pathToRes: Map[Uri.Path,HtmlElement]): HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri: Uri, _, _, _) =>
      Future(HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          pathToRes(uri.path).text
        )
      ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          failure
        )
      ))
  }

  def reqToResFlow(failure: String, pathToRes: Map[Uri.Path,HtmlElement]): Flow[HttpRequest,HttpResponse,_] = Flow[HttpRequest] map {
    case HttpRequest(HttpMethods.GET, uri: Uri, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          pathToRes(uri.path).text
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          failure
        )
      )
  }

  def main(args: Array[String]): Unit = {
    val serverSource = Http().newServerAt("localhost", 12345).connectionSource()

    val connectionSink = Sink.foreach[IncomingConnection] { connection =>
      println(s"Incoming connection: ${connection.remoteAddress}")
    }

//    val serverBinding = serverSource.toMat(connectionSink)(Keep.left).run()
//    serverBinding onComplete {
//      case Success(value) =>
//        println("Server binding successful.")
//        Thread.sleep(10000)
//        value.unbind()
//      case Failure(exception) => println(s"Server binding failed: $exception")
//    }

    val root = Uri.Path("/")
    val home = Uri.Path("/home")
    val search = Uri.Path("/search")
    val uriToMessage = Map(
      root -> htmlBodyDecorator(HtmlElement("Hello from Akka HTTP!")),
      home -> htmlBodyDecorator(HtmlElement("Welcome to home page."))
    )

    val requestHandler = reqToResFuture(htmlBodyDecorator(HtmlElement("Resource not found.")).text, uriToMessage)

    val httpSinkConnectionHandler = Sink.foreach[IncomingConnection](conn => conn.handleWithAsyncHandler(requestHandler))
//    val asyncServerBinding = Http().newServerAt("localhost", 12346).connectionSource()
//      .toMat(httpSinkConnectionHandler)(Keep.left).run()
//    asyncServerBinding onComplete {
//      case Success(value) =>
//        println("Server binding successful.")
//        Thread.sleep(10000)
//        value.unbind()
//      case Failure(exception) => println(s"Server binding failed: $exception")
//    }

    val flowRequestHandler = reqToResFlow(htmlBodyDecorator(HtmlElement("Resource not found.")).text, uriToMessage)
//    val flowServerBinding = Http().newServerAt("localhost",12347).bindFlow(flowRequestHandler)
//    flowServerBinding onComplete {
//      case Success(value) =>
//        println("Server binding successful.")
//      case Failure(exception) => println(s"Server binding failed: $exception")
//    }

    val reqToResFlow2: Flow[HttpRequest,HttpResponse,_] = Flow[HttpRequest] map {
      case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
        HttpResponse(status = StatusCodes.Found, headers = List(Location("https://google.com")))
    }

    val flowServerBinding2 = Http().newServerAt("localhost",12348).bindFlow(reqToResFlow2)
    flowServerBinding2 flatMap (binding => binding.unbind()) onComplete(_ => system.terminate())
  }
}
