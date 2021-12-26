package it.unibo.alchemist.loader.`export`.extractors

import it.unibo.alchemist.loader.`export`.Extractor
import it.unibo.alchemist.model.interfaces.{Environment, Reaction, Time}

import java.util
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

class ExportWithProblem extends Extractor[Int] {
  val scalaNames = (1 to 5).map(i => s"temperature-$i")
  private val rest = scalaNames.tail.map(name => name -> 0).toList
  override def getColumnNames: util.List[String] = scalaNames.asJava

  override def extractData[T](
    environment: Environment[T, _],
    reaction: Reaction[T],
    time: Time,
    l: Long
  ): util.Map[String, Int] = {
    val result = (scalaNames.head -> 10) :: rest
    Map(result: _*).asJava
  }
}
