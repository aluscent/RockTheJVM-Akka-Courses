package AkkaStreams.P3_StreamsGraphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object GraphCycles {
  implicit val system = ActorSystem("graphCycles")

  def main(args: Array[String]): Unit = {
    val source1 = Source(1 to 10)

    val accelerator = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val sourceShape = builder.add(source1)
      val merge = builder.add(Merge[Int](2))
      val incrementor = builder.add(Flow[Int].map(x => {
        println(s"Accelerating $x")
        x + 1
      }))

      sourceShape ~> merge ~> incrementor
      incrementor ~> merge

      ClosedShape
    }

    //RunnableGraph.fromGraph(accelerator).run() // a graph cycle deadlock

    /**
     * Solutions to the problem "graph cycle deadlock"
     *  1. MergePreferred
     *  2. Buffers
     */

    val acceleratorMergePreferred = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val sourceShape = builder.add(source1)
      val merge = builder.add(MergePreferred[Int](1))
      val incrementor = builder.add(Flow[Int].map(x => {
        println(s"Accelerating $x")
        Thread.sleep(100)
        x
      }))

      sourceShape ~> merge ~> incrementor
      incrementor ~> merge

      ClosedShape
    }

    //RunnableGraph.fromGraph(acceleratorMergePreferred).run()

    val acceleratorBuffered = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val sourceShape = builder.add(source1)
      val merge = builder.add(Merge[Int](2))
      val incrementor = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropTail).map(x => {
        println(s"Accelerating $x")
        Thread.sleep(100)
        x
      }))

      sourceShape ~> merge ~> incrementor
      incrementor ~> merge

      ClosedShape
    }

    RunnableGraph.fromGraph(acceleratorBuffered).run()
  }
}
