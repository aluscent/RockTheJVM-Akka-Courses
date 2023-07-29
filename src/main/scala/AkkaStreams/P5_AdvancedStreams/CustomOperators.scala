package AkkaStreams.P5_AdvancedStreams

import akka.actor.ActorSystem
import akka.stream.{Attributes, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable
import scala.util.Random

object CustomOperators {
  implicit val system = ActorSystem()

  class RandomNumberGenerator(max: Int) extends GraphStage[SourceShape[Int]] {
    val outPort: Outlet[Int] = Outlet[Int]("randomGenerator")
    val random = new Random()

    override def shape: SourceShape[Int] = SourceShape(outPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      // implement logic
      setHandler(outPort, new OutHandler {
        override def onPull(): Unit = {
          // emit a new element
          val next = random.nextInt(max)
          // push it out of out port
          push(outPort, next)
        }
      })
    }
  }

  class Batcher[T](size: Int) extends GraphStage[SinkShape[T]] {
    val inPort = Inlet[T]("batchGenerator")
    val batch = new mutable.Queue[T]()

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = pull(inPort)

      setHandler(inPort, new InHandler {
        override def onPush(): Unit = {
          // used when upstream wants to send me an element
          val next = grab(inPort)
          batch.enqueue(next)
          if (batch.size >= size) {
            println(s"Batch flushing: ${batch.dequeueAll(_ => true) mkString(", ")}")
          }
          pull(inPort) // send demand upstream
        }

        override def onUpstreamFinish(): Unit = {
          if (batch.nonEmpty) println(s"Batch flushing: ${batch.dequeueAll(_ => true) mkString(", ")}")
        }
      })
    }

    override def shape: SinkShape[T] = SinkShape(inPort)
  }

  def main(args: Array[String]): Unit = {
    // 1 - create custom source with random numbers until cancelled
    val randomGenerator = Source.fromGraph(new RandomNumberGenerator(100))
    //randomGenerator.runWith(Sink.foreach(println))

    // 2 - a custom sink to print elements in batches of given size
    val batchGenerator = Sink.fromGraph(new Batcher[Int](10))
    Source(1 to 100).runWith(batchGenerator)
  }
}
