package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.DirectivesBreakdown.formattedTime

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{FileIO, Sink}

import java.io.File
import scala.language.postfixOps
import scala.util.{Failure, Success}

object UploadingFiles {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    val pageFile = scala.io.Source
      .fromFile("src/main/resources/HTML/fileUpload.html")
    val html = pageFile.getLines().mkString("\n")
    pageFile.close()

    val filesRoute = {
      pathEndOrSingleSlash {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
      } ~
      (path("upload") & extractLog) { log =>
      // multipart/form data

      entity(as[Multipart.FormData]) { formData =>
        // handle file payload
        val partsSource = formData.parts
        val partsSink = Sink.foreach[Multipart.FormData.BodyPart](part =>
          if (part.name == "special") {
            // create a file
            val filename = "src/main/resources/Download/" + part.filename.getOrElse("tempFile")
            val file = new File(filename)

            log.info("Writing to file.")

            val fileContentSource = part.entity.dataBytes
            val fileContentSink = FileIO.toPath(file.toPath)
            fileContentSource.runWith(fileContentSink)
          }
        )

        val writeOperationFuture = partsSource.runWith(partsSink)
        onComplete(writeOperationFuture) {
          case Success(_) => complete("File uploaded.")
          case Failure(exception) => complete(s"Upload failed.  $exception")
        }
      }
    }
    }


    val server = Http().newServerAt("localhost", 12345).bind(filesRoute)

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
