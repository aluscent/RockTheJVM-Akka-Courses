package AkkaHTTP.Playground

object Implicits {
  implicit class Printer[T <: Product](cc: T) {
      def print() = (0 until cc.productElementNames.length).foldRight("") { (i: Int, initial: String) =>
        initial + s"${cc.productElementName(i)} is ${cc.productElement(i)}\n".stripMargin
    }
  }

  def main(args: Array[String]): Unit = {
    case class Pitch(firstName: String, lastName: String, age: Int)

    val sara = Pitch("sara", "salemi", 19)

    println(sara.print())
  }
}
