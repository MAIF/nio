package models

import controllers.ReadableEntity
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax.unlift
import reactivemongo.bson.BSONObjectID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import utils.DateUtils

import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class DoneBy(userId: String, role: String)
object DoneBy {
  implicit val doneByFormats = Json.format[DoneBy]
}

case class Consent(key: String, label: String, checked: Boolean) {
  def asXml = {
    <consent>
      <key>{key}</key>
      <label>{label}</label>
      <checked>{checked}</checked>
    </consent>
  }
}
object Consent {
  implicit val consentFormats = Json.format[Consent]

  def fromXml(xml: Elem) = {
    val key = (xml \ "key").head.text
    val label = (xml \ "label").head.text
    val checked = (xml \ "checked").head.text.toBoolean
    Consent(key, label, checked)
  }
}

case class ConsentGroup(key: String, label: String, consents: Seq[Consent]) {
  def asXml = {
    <consentGroup>
      <key>{key}</key>
      <label>{label}</label>
      <consents>{consents.map(_.asXml)}</consents>
    </consentGroup>
  }
}
object ConsentGroup {
  implicit val consentGroupFormats = Json.format[ConsentGroup]

  def fromXml(xml: Elem) = {
    val key = (xml \ "key").head.text
    val label = (xml \ "label").head.text
    val consentsXml = (xml \ "consents").head
    val consents = consentsXml.child.collect {
      case e: Elem => Consent.fromXml(e)
    }
    ConsentGroup(key, label, consents)
  }
}

// A user will have multiple consent facts
case class ConsentFact(_id: String = BSONObjectID.generate().stringify,
                       userId: String,
                       doneBy: DoneBy,
                       version: Int,
                       groups: Seq[ConsentGroup],
                       lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
                       orgKey: Option[String] = None,
                       metaData: Option[Map[String, String]] = None)
    extends ModelTransformAs {
  def asJson = {
    Json.obj(
      "userId" -> userId,
      "doneBy" -> Json.toJson(doneBy),
      "version" -> version,
      "groups" -> Json.toJson(groups),
      "lastUpdate" -> lastUpdate.toString(DateUtils.utcDateFormatter),
      "orgKey" -> JsString(orgKey.getOrElse("")),
      "metaData" -> JsObject(
        metaData
          .map(_.map { case (k, v) => (k, JsString(v)) }.toSeq)
          .getOrElse(Seq[(String, JsValue)]()))
    )
  }

  def asXml = {
    <consentFact>
      <userId>{userId}</userId>
      <doneBy>
        <userId>{doneBy.userId}</userId>
        <role>{doneBy.role}</role>
      </doneBy>
      <version>{version}</version>
      <groups>{groups.map(_.asXml)}</groups>
      <lastUpdate>{lastUpdate.toString(DateUtils.utcDateFormatter)}</lastUpdate>
      <orgKey>{orgKey.getOrElse("")}</orgKey>
      {metaData.map{md => <metaData>{md.map{ e => <metaDataEntry key={e._1} value={e._2}/>}}</metaData>}.getOrElse{<metaData/>}}
    </consentFact>
  }
}
object ConsentFact extends ReadableEntity[ConsentFact] {
  def newWithoutIdAndLastUpdate(userId: String,
                                doneBy: DoneBy,
                                version: Int,
                                groups: Seq[ConsentGroup],
                                orgKey: Option[String] = None,
                                metaData: Option[Map[String, String]] = None) =
    ConsentFact(
      _id = BSONObjectID.generate().stringify,
      userId = userId,
      doneBy = doneBy,
      version = version,
      groups = groups,
      lastUpdate = DateTime.now(DateTimeZone.UTC),
      orgKey = orgKey,
      metaData = metaData
    )

  val consentFactReadsWithoutIdAndLastUpdate: Reads[ConsentFact] = (
    (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]]
  )(ConsentFact.newWithoutIdAndLastUpdate _)

  val consentFactReads: Reads[ConsentFact] = (
    (__ \ "_id").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "lastUpdate").read[DateTime](DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]]
  )(ConsentFact.apply _)

  val consentFactWrites: Writes[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "lastUpdate").write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]]
  )(unlift(ConsentFact.unapply))

  val consentFactFormats = Format(consentFactReads, consentFactWrites)

  def template(orgVerNum: Int, groups: Seq[ConsentGroup], orgKey: String) =
    ConsentFact(_id = null,
                userId = "fill",
                doneBy = DoneBy("fill", "fill"),
                version = orgVerNum,
                groups = groups,
                lastUpdate = DateTime.now(DateTimeZone.UTC),
                orgKey = Some(orgKey))

  def fromXml(xml: Elem) = {
    Try {
      val userId = (xml \ "userId").head.text
      val doneByUserId = (xml \ "doneBy" \ "userId").head.text
      val doneByRole = (xml \ "doneBy" \ "role").head.text
      val version = (xml \ "version").head.text.toInt
      val groupsXml = (xml \ "groups").head
      val groups = groupsXml.child.collect {
        case e: Elem => ConsentGroup.fromXml(e)
      }
      val metaData = (xml \ "metaData").headOption.map(md =>
        md.child.collect {
          case e: Elem => (e \ "@key").text -> (e \ "@value").text
        }.toMap)

      ConsentFact(
        _id = BSONObjectID.generate().stringify,
        userId = userId,
        doneBy = DoneBy(doneByUserId, doneByRole),
        version = version,
        lastUpdate = DateTime.now(DateTimeZone.UTC),
        groups = groups,
        metaData = metaData
      )
    } match {
      case Success(value) => Right(value)
      case Failure(throwable) => {
        Left(throwable.getMessage)
      }
    }
  }

  def fromJson(json: JsValue) = {
    json.validate[ConsentFact](
      ConsentFact.consentFactReadsWithoutIdAndLastUpdate) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }

  def addOrgKey(consentFact: ConsentFact, orgKey: String): ConsentFact = {
    consentFact.copy(orgKey = Some(orgKey))
  }
}
