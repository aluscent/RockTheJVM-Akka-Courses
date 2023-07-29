package AkkaStreams.Playground

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Flow, GraphDSL, MergePreferred, RunnableGraph, Source, Zip}

object GraphCycle {
  implicit val system = ActorSystem("graphCycle")

  def main(args: Array[String]): Unit = {
    val source1: Source[(Int,Int),NotUsed] = Source[(Int,Int)](Seq((1,1)))
    val adder = Flow[(Int,Int)].map(x => {
      println(s"Value: ${x._1 + x._2}")
      Thread.sleep(100)
      (x._1 + x._2, x._1)
    })

    val Fibonacci = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val source1Shape = builder.add(source1)
      val merge = builder.add(MergePreferred[(Int,Int)](1))
      val adderShape = builder.add(adder)

      source1Shape ~> merge.in(0)
      merge.out ~> adderShape
      adderShape ~> merge.preferred

      ClosedShape
    }

    RunnableGraph.fromGraph(Fibonacci).run()
  }
}
