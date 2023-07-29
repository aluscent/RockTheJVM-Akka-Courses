package AkkaStreams.Playground

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.concurrent.{Future, Promise}
import scala.util.Success

object CustomOperator {
  implicit val system = ActorSystem()
  import system.dispatcher

  class FilterFlow[T](predicate: T => Boolean) extends GraphStage[FlowShape[T,T]] {
    val inPort = Inlet[T]("Inlet")
    val outPort = Outlet[T]("Outlet")

    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      try {
        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            val next = grab(inPort)
            if (predicate(next)) push(outPort, next)
            else pull(inPort)
          }
        })

        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)
        })
      } catch {
        case exception: Throwable => failStage(exception)
      }
    }
  }

  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T,T], Future[Int]] {
    val inPort = Inlet[T]("Inlet")
    val outPort = Outlet[T]("Outlet")
    var init = 0

    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]
      val future = promise.future
      val logic = new GraphStageLogic(shape) {
        var counter = 0

        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)

          override def onDownstreamFinish(cause: Throwable): Unit = {
            promise.success(counter)
            super.onDownstreamFinish(cause)
          }
        })

        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            val next = grab(inPort)
            counter += 1
            push(outPort, next)
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        })
      }

      (logic, future)
    }
  }

  def main(args: Array[String]): Unit = {
    val filterFlow = Flow.fromGraph(new FilterFlow[Int](_ > 10))
    //Source(5 to 15).via(filterFlow).runWith(Sink.foreach(println))

    val counterFlow = Flow.fromGraph(new CounterFlow[Int])
    val promise = Source(1 to 10).viaMat(counterFlow)(Keep.right).to(Sink.foreach(println)).run()
    promise onComplete {
      case Success(value) => println(s"Count is $value")
    }
  }
}
