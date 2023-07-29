package AkkaStreams.P3_StreamsGraphs

import akka.actor.ActorSystem
import akka.stream.{BidiShape, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows {
  implicit val system = ActorSystem("bidirectionalFlows")

  def main(args: Array[String]): Unit = {
    // An example for the use-cases of Bidirectional Flows: cryptography
    //  they use an algorithm named scissor-cypher
    def encrypt(n: Int)(string: String) = string.map(x => (x + n).toChar)

    def decrypt(n: Int)(string: String) = encrypt(-n)(string)

    // bidiflow
    val bidiStaticFlowGraph = GraphDSL.create() { implicit builder =>

      val encryptionFlow = builder.add(Flow[String].map(encrypt(3)))
      val decryptionFlow = builder.add(Flow[String].map(decrypt(3)))

      BidiShape fromFlows (encryptionFlow, decryptionFlow)
    }

    val unencryptedSource = Source("scala akka is awesome and I like it".split(" "))

    val cryptoGraph = RunnableGraph fromGraph {
      GraphDSL.create() { implicit builder =>
        val unencryptedSourceShape = builder.add(unencryptedSource)
        val bidi = builder.add(bidiStaticFlowGraph)
        val encryptedSink = builder.add(Sink.foreach[String](x => println(s"Encrypted value: $x")))
        val decryptedSink = builder.add(Sink.foreach[String](x => println(s"Decrypted value: $x")))
        val broadcast = builder.add(Broadcast[String](2))
        import GraphDSL.Implicits._
        unencryptedSourceShape ~> bidi.in1
        bidi.out1 ~> broadcast
        broadcast ~> encryptedSink
        broadcast ~> bidi.in2
        bidi.out2 ~> decryptedSink
        ClosedShape
      }
    }

    cryptoGraph.run()
  }
}
