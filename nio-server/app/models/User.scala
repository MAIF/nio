package models

import play.api.libs.json.Json
import reactivemongo.api.bson.BSONObjectID
import scala.collection.Seq

case class User(
    _id: String = BSONObjectID.generate().stringify,
    userId: String, // can be siebel id or other
    orgKey: String,
    orgVersion: Int,
    latestConsentFactId: String
)

object User {
  implicit val formats = Json.format[User]
}
