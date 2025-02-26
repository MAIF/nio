package models

import play.api.libs.json.{Json, OFormat}
import reactivemongo.api.bson.BSONObjectID


case class User(
    _id: String = BSONObjectID.generate().stringify,
    userId: String, // can be siebel id or other
    orgKey: String,
    orgVersion: Int,
    latestConsentFactId: String
)

object User {
  implicit val formats: OFormat[User] = Json.format[User]
}
