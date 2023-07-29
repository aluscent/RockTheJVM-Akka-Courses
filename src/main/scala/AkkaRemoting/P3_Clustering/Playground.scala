package AkkaRemoting.P3_Clustering

object Playground {

  def main(args: Array[String]): Unit = {
    val openedFile = scala.io.Source
      .fromFile("/media/alitariverdy/New Volume/Job/CodeDev/IdeaProjects/RocktheJVM-Scala-Focus/src/main/resources/data/cars.json")

    openedFile.getLines().foreach(println)
  }
}
