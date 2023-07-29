package AkkaHTTP.Part4ClientAPI

import AkkaHTTP.Part4ClientAPI.PaymentSystem.requestFormat
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json.enrichAny

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

object ConnectionLevelAPI {
  implicit val system = ActorSystem()
  import system.dispatcher

  def oneOffRequest(connFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]])(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(connFlow).runWith(Sink.head)

  def main(args: Array[String]): Unit = {
    val connectionFlow = Http().outgoingConnection("www.google.com")
    def testOneOff(request: HttpRequest) = oneOffRequest(connectionFlow)(request)

    testOneOff(HttpRequest()) onComplete {
      case Success(value) => println(s"Got successful response: $value")
      case Failure(exception) => println(s"Request failed: $exception")
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
      PaymentRequest(cards.head, mahdi, 200),
      PaymentRequest(cards(1), mahdi, 120),
      PaymentRequest(cards(2), john, 500)
    )

    val serverRequests = paymentRequests.map(req => {
      HttpRequest(HttpMethods.POST, Uri("/api/payment"), entity = HttpEntity(ContentTypes.`application/json`, req.toJson.prettyPrint))
    })

    Source(serverRequests).via(Http().outgoingConnection("localhost", 12345)).runWith(Sink.foreach(println))
  }
}
