package AkkaStreams.P4_TechniquesPatterns

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class Testing extends TestKit(ActorSystem("testingAkkaStreams")) with AnyWordSpecLike with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "a simple stream" should {
    "satisfy basic assertions" in {
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)

      val sumFuture = simpleSource.toMat(simpleSink)(Keep.right).run()
      val sum = Await.result(sumFuture, 2 seconds)
      assert(sum == 55)
    }

    "integrate with test actor via mat values" in {
      import akka.pattern._
      import system.dispatcher

      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)
      val probe = TestProbe()

      simpleSource.toMat(simpleSink)(Keep.right).run().pipeTo(probe.ref)

      probe.expectMsg(55)
    }

    "integrate with a test-actor-based sink" in {
      val simpleSource = Source(1 to 5)
      val simpleFlow = Flow[Int].scan[Int](0)(_ + _)
      val streamUnderTest = simpleSource.via(simpleFlow)

      val probe = TestProbe()
      val probeSink = Sink.actorRef(probe.ref, "completed", RuntimeException => "failed")

      streamUnderTest.to(probeSink).run()
      probe.expectMsgAllOf(0,1,3,6,10,15)
    }

    "integrate with streams testkit sink" in {
      val sourceUnderTest = Source(1 to 8).map(_ * 2)
      val testSink = TestSink.probe[Int]
      val testVal = sourceUnderTest.runWith(testSink)
      testVal.request(8).expectNextN(Seq(2,4,6,8,10,12,14,16)).expectComplete()
    }

    "integrate with streams testkit source" in {
      val sinkUnderTest = Sink.foreach[Int] {
        case 13 => throw new RuntimeException
        case _ =>
      }
      val testSource = TestSource.probe[Int]

      val testVal = testSource.toMat(sinkUnderTest)(Keep.both).run()
      val (testPublisher, testFuture) = testVal

      testPublisher.sendNext(1).sendComplete()
      testFuture onComplete {
        case Success(value) => println(s"Sink should throw error on 13. Value is: $value")
        case Failure(exception) => println(s"Flow is ok: $exception")
      }
    }

    "test flows with a test source and a test sink" in {
      val flowUnderTest = Flow[Int].map(_ * 2)

      val testSource = TestSource.probe[Int]
      val testSink = TestSink.probe[Int]

      val matValue = testSource.via(flowUnderTest).toMat(testSink)(Keep.both).run()
      val (testPublisher, testFuture) = matValue
      testPublisher.sendNext(4).sendNext(7).sendComplete()
      testFuture.request(2).expectNextN(Seq(8,14)).expectComplete()
    }
  }
}
