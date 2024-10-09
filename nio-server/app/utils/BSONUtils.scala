package utils

import play.api.libs.json.Json
import reactivemongo.api.bson.BSONDocument


object BSONUtils {
  def stringify(doc: BSONDocument): String = {
    import reactivemongo.play.json.compat._
    import bson2json._
    Json.stringify(Json.toJson(doc))
  }
}
