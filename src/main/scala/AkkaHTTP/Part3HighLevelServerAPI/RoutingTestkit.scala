package AkkaHTTP.Part3HighLevelServerAPI

import AkkaHTTP.Part3HighLevelServerAPI.RoutingTestkit.books
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{MethodRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat, jsonFormat3}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny, enrichString}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

case class Book(id: Int, name: String, author: String)

trait BookJsonProtocol extends DefaultJsonProtocol {
  implicit val bookFormat: RootJsonFormat[Book] = jsonFormat3(Book)
}

class RoutingTestkit extends AnyWordSpec with Matchers with ScalatestRouteTest
  with BookJsonProtocol with SprayJsonSupport {
  import RoutingTestkit.libraryRoute

  "a digital library backend" should {
    "return all library books" in {
      Get("/api/book") ~> libraryRoute ~> check {
        // assertions
        status shouldBe StatusCodes.OK

        entityAs[List[Book]] shouldBe books
      }
    }

    "return a book by hitting query param endpoint" in {
      Get("/api/book?id=2") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK

        responseAs[Option[Book]] shouldBe Some(books(1))
      }
    }

    "return a book by hitting int endpoint" in {
      Get("/api/book/2") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK

        val strictEntity = Await.result(response.entity.toStrict(3 seconds), 4 seconds)
        strictEntity.contentType shouldBe ContentTypes.`application/json`
        val book = strictEntity.data.utf8String.parseJson.convertTo[Option[Book]]
        book shouldBe Some(books(1))
      }
    }

    "add a book into list" in {
      val newBook = Book(5, "the War of Art", "Steven Pressfield")
      Post("/api/book", newBook) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK

        assert(books.contains(newBook))
      }
    }

    "not accept other method" in {
      Delete("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty

        val methodRejection = rejections.collect {
          case rejection: MethodRejection => rejection
        }

        methodRejection.length shouldBe 2
      }
    }
  }
}

object RoutingTestkit extends BookJsonProtocol with SprayJsonSupport {

  var books: List[Book] = List(
    Book(1, "to Kill a Mocking Bird", "Harper Lee"),
    Book(2, "the Lord of the Rings", "JRR Tolkien"),
    Book(3, "Game of Thrones", "GRR Marting")
  )

  import akka.http.scaladsl.server.Directives._
  val libraryRoute: Route = pathPrefix("api" / "book") {
    get {
      (path(IntNumber) | parameter(Symbol("id").as[Int])) { id =>
        complete(books.find(_.id == id))
      } ~ pathEndOrSingleSlash {
        complete(books)
      }
    } ~ post {
      entity(as[Book]) { book =>
        books = books :+ book
        complete(StatusCodes.OK)
      } ~ complete(StatusCodes.BadRequest)
    }
  }
}
