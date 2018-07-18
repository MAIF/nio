package models

import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import utils.DateUtils
import utils.Result.AppErrors
import utils.Result.AppErrors._

import scala.xml.{Elem, NodeSeq}

case class Metadata(key: String, value: String)
object Metadata {

  implicit val xmlRead: XMLRead[Metadata] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "@key").validate[String](Some(s"${path.convert()}@key")),
        (node \ "@value").validate[String](Some(s"${path.convert()}@value"))
      ).mapN(Metadata.apply)
}

case class DoneBy(userId: String, role: String)

object DoneBy {
  implicit val doneByFormats = Json.format[DoneBy]

  implicit val xmlRead: XMLRead[DoneBy] = {
    import AppErrors._
    import cats.data.Validated._
    import cats.implicits._
    import libs.xml.implicits._
    import libs.xml.syntax._
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "userId").validate[String](Some(s"${path.convert()}userId")),
        (node \ "role").validate[String](Some(s"${path.convert()}role"))
      ).mapN { DoneBy.apply }
  }
}

case class Consent(key: String, label: String, checked: Boolean) {
  def asXml = <consent>
      <key>
        {key}
      </key>
      <label>
        {label}
      </label>
      <checked>
        {checked}
      </checked>
    </consent>.clean()
}

object Consent {
  implicit val consentFormats = Json.format[Consent]

  implicit val xmlRead: XMLRead[Consent] =
    (xml: NodeSeq, path: Option[String]) => {
      (
        (xml \ "key").validate[String](Some(s"${path.convert()}key")),
        (xml \ "label").validate[String](Some(s"${path.convert()}label")),
        (xml \ "checked").validate[Boolean](Some(s"${path.convert()}checked"))
      ).mapN(Consent.apply)
    }

}

case class ConsentGroup(key: String, label: String, consents: Seq[Consent]) {
  def asXml = <consentGroup>
      <key>
        {key}
      </key>
      <label>
        {label}
      </label>
      <consents>
        {consents.map(_.asXml)}
      </consents>
    </consentGroup>.clean()
}

object ConsentGroup {
  implicit val consentGroupFormats = Json.format[ConsentGroup]

  import Consent._

  implicit val xmlRead: XMLRead[ConsentGroup] =
    (xml: NodeSeq, path: Option[String]) => {
      (
        (xml \ "key").validate[String](Some(s"${path.convert()}key")),
        (xml \ "label").validate[String](Some(s"${path.convert()}label")),
        (xml \ "consents").validate[Seq[Consent]](
          Some(s"${path.convert()}consents"))
      ).mapN(ConsentGroup.apply)
    }

}

// A user will have multiple consent facts
case class ConsentFact(_id: String = BSONObjectID.generate().stringify,
                       userId: String,
                       doneBy: DoneBy,
                       version: Int,
                       groups: Seq[ConsentGroup],
                       lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
                       lastUpdateSystem: DateTime =
                         DateTime.now(DateTimeZone.UTC),
                       orgKey: Option[String] = None,
                       metaData: Option[Map[String, String]] = None)
    extends ModelTransformAs {

  def asJson = ConsentFact.consentFactWritesWithoutId.writes(this)

  def asXml: Elem = <consentFact>
      <userId>
        {userId}
      </userId>
      <doneBy>
        <userId>
          {doneBy.userId}
        </userId>
        <role>
          {doneBy.role}
        </role>
      </doneBy>
      <version>
        {version}
      </version>
      <groups>
        {groups.map(_.asXml)}
      </groups>
      <lastUpdate>
        {lastUpdate.toString(DateUtils.utcDateFormatter)}
      </lastUpdate>
      <orgKey>
        {orgKey.getOrElse("")}
      </orgKey>{if (metaData.isDefined) {
      metaData.map { md =>
        <metaData>
          {md.map { e => <metaDataEntry key={e._1} value={e._2}/> }}
        </metaData>
      }
    }.get}
    </consentFact>.clean()
}

object ConsentFact extends ReadableEntity[ConsentFact] {
  def newWithoutIdAndLastUpdate(userId: String,
                                doneBy: DoneBy,
                                version: Int,
                                groups: Seq[ConsentGroup],
                                lastUpdate: DateTime =
                                  DateTime.now(DateTimeZone.UTC),
                                orgKey: Option[String] = None,
                                metaData: Option[Map[String, String]] = None) =
    ConsentFact(
      _id = BSONObjectID.generate().stringify,
      userId = userId,
      doneBy = doneBy,
      version = version,
      groups = groups,
      lastUpdate = lastUpdate,
      orgKey = orgKey,
      metaData = metaData
    )

  val consentFactReadsWithoutIdAndLastUpdate: Reads[ConsentFact] = (
    (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "lastUpdate").readWithDefault[DateTime](
        DateTime.now(DateTimeZone.UTC))(DateUtils.utcDateTimeReads) and
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
      (__ \ "lastUpdateSystem").readWithDefault[DateTime](
        DateTime.now(DateTimeZone.UTC))(DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]]
  )(ConsentFact.apply _)

  val consentFactWrites: Writes[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]]
  )(unlift(ConsentFact.unapply))

  val consentFactWritesWithoutId: Writes[ConsentFact] = (
    (JsPath \ "_id").writeNullable[String].contramap((_: String) => None) and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .writeNullable[DateTime](DateUtils.utcDateTimeWrites)
        .contramap((_: DateTime) => None) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]]
  )(unlift(ConsentFact.unapply))

  val consentFactOWrites: OWrites[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]]
  )(unlift(ConsentFact.unapply))

  val consentFactFormats = Format(consentFactReads, consentFactWrites)
  implicit val consentFactOFormats =
    OFormat(consentFactReads, consentFactOWrites)

  def template(orgVerNum: Int, groups: Seq[ConsentGroup], orgKey: String) =
    ConsentFact(
      _id = null,
      userId = "fill",
      doneBy = DoneBy("fill", "fill"),
      version = orgVerNum,
      groups = groups,
      lastUpdate = DateTime.now(DateTimeZone.UTC),
      lastUpdateSystem = null,
      orgKey = Some(orgKey)
    )

  implicit val readXml: XMLRead[ConsentFact] =
    (node: NodeSeq, path: Option[String]) =>
      (
        BSONObjectID.generate().stringify.toXmlResult,
        (node \ "userId").validate[String](Some(s"${path.convert()}userId")),
        (node \ "doneBy").validate[DoneBy](Some(s"${path.convert()}doneBy")),
        (node \ "lastUpdate")
          .validateNullable[DateTime](DateTime.now(DateTimeZone.UTC),
                                      Some(s"${path.convert()}lastUpdate")),
        (node \ "orgKey").validateNullable[String](
          Some(s"${path.convert()}orgKey")),
        (node \ "version").validate[Int](Some(s"${path.convert()}version")),
        (node \ "groups").validate[Seq[ConsentGroup]](
          Some(s"${path.convert()}groups")),
        (node \ "metaData").validateNullable[Seq[Metadata]](
          Some(s"${path.convert()}metaData"))
      ).mapN {
        (id, userId, doneBy, lastUpdate, orgKey, version, groups, metadata) =>
          ConsentFact(
            _id = id,
            userId = userId,
            doneBy = doneBy,
            version = version,
            lastUpdate = lastUpdate,
            orgKey = orgKey,
            groups = groups,
            metaData = metadata.map(m => m.map(ev => (ev.key, ev.value)).toMap)
          )
    }

  def fromXml(xml: Elem): Either[AppErrors, ConsentFact] = {
    readXml.read(xml, Some("consentFact")).toEither
  }

  def fromJson(json: JsValue): Either[AppErrors, ConsentFact] = {
    json.validate[ConsentFact](
      ConsentFact.consentFactReadsWithoutIdAndLastUpdate) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }

  def addOrgKey(consentFact: ConsentFact, orgKey: String): ConsentFact = {
    consentFact.copy(orgKey = Some(orgKey))
  }
}
