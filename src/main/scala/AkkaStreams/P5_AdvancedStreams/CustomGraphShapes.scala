package AkkaStreams.P5_AdvancedStreams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Graph, Inlet, Outlet, Shape}

import scala.concurrent.duration._
import scala.language.postfixOps

object CustomGraphShapes {
  implicit val system = ActorSystem("customGraphShapes")

  // simple balance 2 by 3
  case class Balance2x3 (
    in0: Inlet[Int],
    in1: Inlet[Int],
    out0: Outlet[Int],
    out1: Outlet[Int],
    out2: Outlet[Int],
  ) extends Shape {
    // Inlet[T], Outlet[T]
    override def inlets: Seq[Inlet[_]] = Seq(in0, in1)

    override def outlets: Seq[Outlet[_]] = Seq(out0, out1, out2)

    override def deepCopy(): Shape = Balance2x3(
      in0.carbonCopy(),
      in1.carbonCopy(),
      out0.carbonCopy(),
      out1.carbonCopy(),
      out2.carbonCopy()
    )
  }

  case class BalanceMxN[T] (override val inlets: Seq[Inlet[T]], override val outlets: Seq[Outlet[T]]) extends Shape {
    override def deepCopy(): Shape = BalanceMxN(inlets.map(_.carbonCopy()), outlets.map(_.carbonCopy()))
  }

  object BalanceMxN {
    def apply[T](inputCount: Int, outputCount: Int): Graph[BalanceMxN[T], NotUsed] =
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val merger = builder.add(Merge[T](inputCount))
        val balancer = builder.add(Balance[T](outputCount))

        merger ~> balancer

        BalanceMxN(merger.inlets, balancer.outlets)
      }
  }

  def main(args: Array[String]): Unit = {

    val balance2x3 = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](3))

      merge ~> balance

      Balance2x3(merge.in(0), merge.in(1), balance.out(0), balance.out(1), balance.out(2))
    }

    val source1 = Source(1 to 10)
    val source2 = Source(60 to 70)

    val balance2x3Graph = RunnableGraph fromGraph {
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val so1 = builder.add(source1.throttle(1, 1 seconds))
        val so2 = builder.add(source2.throttle(1, 1 seconds))
        val si1 = builder.add(Sink.foreach[Int](x => println(s"Sink 1: $x")))
        val si2 = builder.add(Sink.foreach[Int](x => println(s"Sink 2: $x")))
        val si3 = builder.add(Sink.foreach[Int](x => println(s"Sink 3: $x")))
        val bal = builder.add(balance2x3)

        so1 ~> bal.in0
        so2 ~> bal.in1
        bal.out0 ~> si1
        bal.out1 ~> si2
        bal.out2 ~> si3

        ClosedShape
      }
    }

    //balance2x3Graph.run()


  }
}
