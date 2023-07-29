package AkkaEssentials.Part7AkkaPatterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("askSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import AskSpec._
}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)

  case object AuthFailure
  case object AuthSuccess

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        if (kv.contains(key)) {
          log.info(s"Reading key '$key': ${kv(key)}")
          sender() ! kv(key)
        }
        else log.info("(key,value) pair does not exist.")
      case Write(key, value) =>
        log.info(s"Mapping value '$value' to key '$key'")
        if (!kv.contains(key)) context.become(online(kv + (key -> value)))
    }
  }

  class AuthManager extends Actor with ActorLogging {
    private val authDb = context.actorOf(Props[KVActor])

    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = {
      case RegisterUser(username, password) =>
        log.info(s"Adding user '$username' to (key,value) store.")
        authDb ! Write(username, password)
      case Authenticate(username, password) =>
        val originalSender = sender()
        val extractedPassword = authDb ? Read(username)
        extractedPassword.onComplete { // NEVER call methods of actor or access mutable states in 'onComplete'.
          case Success(value) =>
            if (password == value) {
              log.info("User authenticated.")
              originalSender ! AuthSuccess
            }
            else {
              log.info("Password is wrong.")
              originalSender ! AuthFailure
            }
          case Failure(exception) => originalSender ! AuthFailure
        }


    }
  }

  def main(args: Array[String]): Unit = {

  }
}
