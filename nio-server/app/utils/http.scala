package utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParser

import scala.concurrent.ExecutionContext

object Import {
  import akka.stream.scaladsl.{Flow, Framing}
  val newLineSplit =
    Framing.delimiter(ByteString("\n"), 10000, allowTruncation = true)
  val toJson = Flow[ByteString] via newLineSplit map (_.utf8String) filterNot (_.isEmpty) map (l => (l, Json.parse(l)))

  def ndJson(implicit ec: ExecutionContext): BodyParser[Source[(String, JsValue), _]] =
    BodyParser { req =>
      Accumulator.source[ByteString].map(s => Right(s.via(toJson)))
    }
}

