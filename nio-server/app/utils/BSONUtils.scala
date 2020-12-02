package utils

import play.api.libs.json.Json
import reactivemongo.api.bson.{
  BSONArray,
  BSONBoolean,
  BSONDateTime,
  BSONDecimal,
  BSONDocument,
  BSONDouble,
  BSONElement,
  BSONInteger,
  BSONLong,
  BSONMaxKey,
  BSONMinKey,
  BSONNull,
  BSONObjectID,
  BSONString,
  BSONTimestamp,
  BSONUndefined
}

import scala.util.{Failure, Success, Try}

object BSONUtils {
  def stringify(doc: BSONDocument): String = {
    import reactivemongo.play.json.compat._
    import bson2json._
    Json.stringify(Json.toJson(doc))
  }
}
