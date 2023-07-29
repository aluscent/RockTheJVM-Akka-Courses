package AkkaHTTP.Part4ClientAPI

import AkkaHTTP.Part4ClientAPI.PaymentSystem.requestFormat
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.{Sink, Source}
import spray.json.enrichAny

import java.util.UUID
import scala.util.{Failure, Random, Success}

object HostLevelAPI {
  implicit val system = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {

    val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

    Source(1 to 10).map(i => (HttpRequest(),i))
      .via(poolFlow)
      .map {
        case (Success(response), value) =>
          response.discardEntityBytes()
          s"Request $value received: $response"
        case (Failure(exception), value) =>
          s"Request $value failed: $exception"
      }
      //.runWith(Sink.foreach[String](println))



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
      HttpRequest(HttpMethods.POST, Uri("/api/payment"), entity = HttpEntity(ContentTypes.`application/json`, req.toJson.prettyPrint)),
      UUID.randomUUID().toString
    ))

    Source(serverRequests)
      .via(Http().cachedHostConnectionPool[String]("localhost", 12345))
      .runForeach {
        case (Success(response@HttpResponse(StatusCodes.Forbidden, _, _, _)), value) =>
          println(s"Order $value was not allowed to proceed: $response")
        case (Success(response), value) =>
          println(s"Order $value returned: $response")
        case (Failure(exception), value) =>
          println(s"Order $value failed: $exception")
      }
  }
}
