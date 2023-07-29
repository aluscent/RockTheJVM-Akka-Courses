package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.DirectivesBreakdown.{formattedTime, system}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object Websockets {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    // Messages: Text vs. Binary
    val textMsg = TextMessage(Source.single("hello via a test msg"))
    val binaryMsg = BinaryMessage(Source.single(CompactByteString("hello via a binary msg")))

    val homeFile = scala.io.Source
      .fromFile("D:\\Job\\CodeDev\\IdeaProjects\\Rock-the-JVM_Advanced-Scala\\src\\main\\resources\\HTML\\websockets.html")
    val homeHtml = homeFile.getLines()
    val page = homeHtml.mkString("\n")
    homeFile.close()

    def websocketFlow: Flow[Message,Message, _] = Flow[Message].map {
      case tm: TextMessage => TextMessage(Source.single(s"Server says back: ") ++ tm.textStream ++ Source.single("!"))
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        BinaryMessage(Source.single(CompactByteString("Server says back.")))
    }



    case class SocialPost(owner: String, content: String)

    val feed = Source(List(
      SocialPost("martin", "i like it!"),
      SocialPost("john", "scala is awesome"),
      SocialPost("shima", "hey babe!")
    ))

    val feedFlow: Flow[Message, Message, _] = Flow.fromSinkAndSource(
      Sink.foreach[Message](println),
      feed.throttle(1, 1 seconds)
        .map(post => TextMessage(s"User ${post.owner} says: ${post.content}"))
    )



    import akka.http.scaladsl.server.Directives._
    val websocketRoute = (pathEndOrSingleSlash & get) {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page))
    } ~ path("greeter") {
      handleWebSocketMessages(feedFlow)
    }

    val server = Http().newServerAt("localhost", 12345).bind(websocketRoute)

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
