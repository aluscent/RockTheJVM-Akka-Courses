package AkkaEssentials.Playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object DistributedWordCount {
  def main(args: Array[String]): Unit = {
    import WordCounterMaster._

    val system = ActorSystem("wordCounter")
    val master = system.actorOf(WordCounterMaster.props)

    def wordCount(dataset: String) =
      dataset
        .split("\\s|,|\\.|!|\\?|:|;|\\(|\\)|\"|'|-|\\+|\\|\\{|\\}|\\*|\\&|\\@|\\#|$|\\%|^|\\~").toList
        .filter(x => x.length > 1)
        .map(x => (x.toLowerCase(), 1))
        .groupBy(_._1)
        .map(x => x._1 -> x._2.length)

    def merge(result1: Map[String, Int], result2: Map[String, Int]) =
      (result1.toList ++ result2.toList).groupBy(_._1).map { case (k, v) => k -> v.map(_._2).sum }

    val file = getSource("src/main/scala/AkkaEssentials/sample.txt")
    val dataset = file.getLines().toList.mkString(" ")
    // println(dataset)
    val wordCountDataset = wordCount(dataset)
    println(wordCountDataset)
    file.close()

    master ! CreateChildren(5, wordCount)
    master ! StartJob(dataset)
    master ! GatherStats[Map[String, Int]](merge)
  }

  def getSource(path: String) = scala.io.Source.fromFile(path)

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    var funcCounter = 0

    override def receive: Receive = NoWorkerMode

    def NoWorkerMode: Receive = {
      case CreateChildren(count, function) =>
        context.become(MasterMode((1 to count).toList
          .map(x => context.actorOf(WordCounterWorker.props(function), s"worker-$funcCounter-$x"))))
        println(s"[Master] A number of $count workers have been created.")
      case StartJob(_) => println("[Master] [NoWorkerMode] You need to create workers first.")
      case GatherStats(_) => println("[Master] [NoWorkerMode] No worker nodes.")
      case _ => println("[Master] [NoWorkerMode] Invalid option.")
    }

    def MasterMode[A](workers: List[ActorRef]): Receive = {
      case CreateChildren(count, function) =>
        funcCounter += 1
        context.become(MasterMode((1 to count).toList
          .map(x => context.actorOf(WordCounterWorker.props(function), s"worker-$funcCounter-$x")) ++ workers))
        println(s"[Master] A number of $count workers have appended.")
      case StartJob(dataset) => workers.foreach(x => x ! StartJob(dataset))
      case GatherStats(merge) => workers.foreach(x => x ! GatherStats(merge))
      case Result(result, merge) => context.become(ResultMode(Result(result, merge)))
      case _ => println("[Master] [MasterMode] Invalid option.")
    }

    def ResultMode[A](result: Result[A]): Receive = {
      case Result(newResult: A, merge) => merge match {
        case merge: ((A, A) => A) =>
          val nextResult = merge(result.result, newResult)
          println(nextResult)
          context.become(ResultMode(Result(nextResult, merge)))
        case _ => println("[Master] [ResultMode] Function type not matching.")
      }
      case _ => println("[Master] [ResultMode] Invalid option.")
    }
  }

  class WordCounterWorker[A, B](function: A => B) extends Actor {

    import WordCounterMaster.{GatherStats, Retry, StartJob}

    override def receive: Receive = NotStarted

    def NotStarted: Receive = {
      case StartJob(dataset: A) =>
        context.become(DoneMode(function(dataset)))
      case _ => println("[Worker] [NotStarted] Invalid option.")
    }

    def DoneMode(result: B): Receive = {
      case StartJob(_) => println("[Worker] [DoneMode] Job has done.")
      case Retry() =>
        println("[Worker] [DoneMode] Resetting the worker.")
        context.become(NotStarted)
      case GatherStats(merge) =>
        println(s"[Worker] [DoneMode] sending results to master.")
        sender() ! WordCounterMaster.Result(result, merge)
      case _ => println("[Worker] [DoneMode] Invalid option.")
    }
  }

  object WordCounterMaster {
    def props = Props(new WordCounterMaster)

    trait Ops[A]

    case class CreateChildren[A, B](count: Int, function: A => B) extends Ops[A]

    case class StartJob[A](dataset: A) extends Ops[A]

    case class GatherStats[A <: Any](merge: (A, A) => A) extends Ops[A]

    case class Retry()

    case class Result[A <: Any](result: A, merge: (A, A) => A) extends Ops[A]
  }

  object WordCounterWorker {
    def props[A, B](function: A => B) = Props(new WordCounterWorker(function))
  }
}
