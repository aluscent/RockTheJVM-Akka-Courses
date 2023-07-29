package AkkaHTTP.Part3HighLevelServerAPI

import spray.json.RootJsonFormat
import AkkaHTTP.Part3HighLevelServerAPI.Exercise.Person
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat, jsonFormat2}

trait PersonJsonConverter {
  implicit def personFormat: RootJsonFormat[Person] = jsonFormat2(Person)
}
