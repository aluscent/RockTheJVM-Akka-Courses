package AkkaHTTP.Part4ClientAPI

import AkkaHTTP.Part3HighLevelServerAPI.DirectivesBreakdown.formattedTime
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat, jsonFormat1, jsonFormat3}
import spray.json.RootJsonFormat

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

case class Account(name: String)
case class CreditCard(serial: Int, code: String, account: Account)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard, receiverAccount: Account, amount: Int)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJson {
  implicit val accountFormat: RootJsonFormat[Account] = jsonFormat1(Account)
  implicit val cardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)

  import PaymentSystemDomain.PaymentRequest
  implicit val requestFormat: RootJsonFormat[PaymentRequest] = jsonFormat3(PaymentRequest)
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._

  override def receive: Receive = {
    case PaymentRequest(CreditCard(serial, _, Account(name)), Account(receiver), amount) =>
      log.info(s"Account $name sending $amount dollars to $receiver.")
      if (serial < 170000) sender() ! PaymentRejected
      else sender() ! PaymentAccepted
  }
}

object PaymentSystem extends PaymentJson with SprayJsonSupport {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {
    import akka.http.scaladsl.server.Directives._
    import PaymentSystemDomain._

    val paymentValidator = system.actorOf(Props[PaymentValidator], "validator")
    implicit val timeout: Timeout = Timeout(2 seconds)

    val paymentRoute = path("api" / "payment") {
      post {
        entity(as[PaymentRequest]) { req =>
          val validation = (paymentValidator ? req).map {
            case PaymentRejected => StatusCodes.Forbidden
            case PaymentAccepted => StatusCodes.OK
            case _ => StatusCodes.BadRequest
          }

          complete(validation)
        }
      }
    }


    val server = Http().newServerAt("localhost", 12345).bind(paymentRoute)

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
