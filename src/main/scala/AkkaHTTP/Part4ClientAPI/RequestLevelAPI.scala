package AkkaHTTP.Part4ClientAPI

import AkkaHTTP.Part4ClientAPI.PaymentSystem.requestFormat
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.scaladsl.Source
import spray.json.enrichAny

import java.util.UUID
import scala.util.{Failure, Random, Success}

object RequestLevelAPI {
  implicit val system = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    val responseFuture = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))
    responseFuture.onComplete {
      case Success(value) =>
        value.discardEntityBytes()
        println(s"Success: $value")
      case Failure(exception) => println(s"Failure: $exception")
    }



    import AkkaHTTP.Part4ClientAPI.PaymentSystemDomain.PaymentRequest
    val john = Account("John")
    val reza = Account("Reza")
    val mahdi = Account("Mahdi")

    val random = new Random()
    var temp = 456
    val cards = List(john,reza,mahdi).map(acc => {
      temp += 1
      CreditCard(random.between(124456, 234578), temp.toString, acc)
    })

    val paymentRequests = List(
      PaymentRequest(cards(0), mahdi, 200),
      PaymentRequest(cards(1), mahdi, 120),
      PaymentRequest(cards(2), john, 500)
    )

    val serverRequests = paymentRequests.map(req => (
      HttpRequest(HttpMethods.POST, Uri("http://localhost:12345/api/payment"), entity = HttpEntity(ContentTypes.`application/json`, req.toJson.prettyPrint))
    ))

    Source(serverRequests)
      .mapAsync(2)(Http().singleRequest(_))
      .runForeach(println)
  }
}
