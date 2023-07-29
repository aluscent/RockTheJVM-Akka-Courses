package AkkaHTTP.Part2LowLevelServerAPI

import akka.actor.ActorSystem

import java.security.KeyStore

object LowLevelHTTPS {
  implicit val system = ActorSystem("lowLevelHttps")

  def main(args: Array[String]): Unit = {
    // define a key-store object
    val ks = KeyStore.getInstance("PKCS12")
    val ksFile = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
  }
}
