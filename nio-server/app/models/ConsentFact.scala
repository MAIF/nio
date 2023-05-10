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
import reactivemongo.api.bson.BSONObjectID
import utils.DateUtils
import utils.Result.AppErrors
import utils.Result.AppErrors._
import utils.json.JsResultOps

import scala.xml.{Elem, NodeSeq}
import scala.collection.Seq

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
      ).mapN {
        DoneBy.apply
      }
  }
}

case class Consent(key: String, label: String, checked: Boolean) {
  def asXml() = <consent>
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
  def asXml() = <consentGroup>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <consents>
      {consents.map(_.asXml())}
    </consents>
  </consentGroup>.clean()
}

object ConsentGroup {
  implicit val consentGroupFormats = Json.format[ConsentGroup]

  implicit val xmlRead: XMLRead[ConsentGroup] =
    (xml: NodeSeq, path: Option[String]) => {
      (
        (xml \ "key").validate[String](Some(s"${path.convert()}key")),
        (xml \ "label").validate[String](Some(s"${path.convert()}label")),
        (xml \ "consents").validate[Seq[Consent]](Some(s"${path.convert()}consents"))
      ).mapN(ConsentGroup.apply)
    }
}

case class ConsentOffer(
    key: String,
    label: String,
    version: Int,
    lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
    groups: Seq[ConsentGroup]
) extends ModelTransformAs {
  override def asXml(): Elem = <offer>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <version>
      {version}
    </version>
    <lastUpdate>
      {lastUpdate.toString(DateUtils.utcDateFormatter)}
    </lastUpdate>
    <groups>
      {groups.map(_.asXml())}
    </groups>
  </offer>.clean()

  override def asJson(): JsValue = ConsentOffer.offerWrites.writes(this)
}

object ConsentOffer extends ReadableEntity[ConsentOffer] {
  implicit val offerReads: Reads[ConsentOffer] = (
    (__ \ "key").read[String] and
      (__ \ "label").read[String] and
      (__ \ "version").read[Int] and
      (__ \ "lastUpdate").readWithDefault[DateTime](DateTime.now(DateTimeZone.UTC))(DateUtils.utcDateTimeReads) and
      (__ \ "groups").read[Seq[ConsentGroup]]
  )(ConsentOffer.apply _)

  implicit val offerWrites: Writes[ConsentOffer] = (
    (__ \ "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "lastUpdate").write[DateTime](DateUtils.utcDateTimeWrites) and
      (__ \ "groups").write[Seq[ConsentGroup]]
  )(unlift(ConsentOffer.unapply))

  implicit val offerOWrites: OWrites[ConsentOffer] = (
    (__ \ "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "lastUpdate").write[DateTime](DateUtils.utcDateTimeWrites) and
      (__ \ "groups").write[Seq[ConsentGroup]]
  )(unlift(ConsentOffer.unapply))

  implicit val format: Format[ConsentOffer]   = Format(offerReads, offerWrites)
  implicit val oformat: OFormat[ConsentOffer] =
    OFormat(offerReads, offerOWrites)

  implicit val offerReadXml: XMLRead[ConsentOffer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "version").validate[Int](Some(s"${path.convert()}version")),
        (node \ "lastUpdate").validate[DateTime](Some(s"${path.convert()}lastUpdate")),
        (node \ "groups").validate[Seq[ConsentGroup]](Some(s"${path.convert()}groups"))
      ).mapN((key, label, version, lastUpdate, groups) => ConsentOffer(key, label, version, lastUpdate, groups))

  override def fromXml(xml: Elem): Either[AppErrors, ConsentOffer] =
    offerReadXml.read(xml, Some("offer")).toEither

  override def fromJson(json: JsValue): Either[AppErrors, ConsentOffer] =
    json.validate[ConsentOffer] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors)     => Left(AppErrors.fromJsError(errors))
    }
}


case class PartialConsent(key: String, label: Option[String], checked: Boolean) {
  def toConsent: Consent = Consent(key, label.getOrElse(""), checked)

}

object PartialConsent {
  def merge(pcs: Seq[PartialConsent], consents: Seq[Consent]): Seq[Consent] = {
    consents.map { c =>
      pcs.find(pc => pc.key == c.key).fold(c) { pc =>
        c.copy(
          label = pc.label.getOrElse(c.label),
          checked = pc.checked
        )
      }
    } ++ pcs.filter(pc => !consents.exists(c => c.key == pc.key)).map { pc =>
      Consent(pc.key, pc.label.getOrElse(""), pc.checked)
    }
  }

  implicit val format = Json.format[PartialConsent]
  implicit val partialConsentReadXml: XMLRead[PartialConsent] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validateNullable[String](Some(s"${path.convert()}label")),
        (node \ "checked").validate[Boolean](Some(s"${path.convert()}checked"))
      ).mapN((key, label, checked) => PartialConsent(key, label, checked))
}
case class PartialConsentGroup (key: String, label: Option[String], consents: Option[Seq[PartialConsent]])

object PartialConsentGroup {

  def merge(partialGroups: Seq[PartialConsentGroup], existingGroups: Seq[ConsentGroup]): Seq[ConsentGroup] = {
    existingGroups.map { g =>
      partialGroups.find(pg => pg.key == g.key).fold( g ) { pg =>
          g.copy(
            label = pg.label.getOrElse(g.label),
            consents = pg.consents.map(pcs => PartialConsent.merge(pcs, g.consents)).getOrElse(g.consents)
          )
        }
    } ++ partialGroups.filter(pg => !existingGroups.exists(g => pg.key == g.key)).map { pc =>
      ConsentGroup(
        pc.key,
        pc.label.getOrElse(""),
        pc.consents.toList.flatten.map(pc => pc.toConsent)
      )
    }
  }

  implicit val format = Json.format[PartialConsentGroup]
  implicit val partialConsentGroupReadXml: XMLRead[PartialConsentGroup] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validateNullable[String](Some(s"${path.convert()}label")),
        (node \ "consents").validateNullable[Seq[PartialConsent]](Some(s"${path.convert()}consents"))
      ).mapN((key, label, consents) => PartialConsentGroup(key, label, consents))
}

case class PartialConsentOffer(key: String, label: Option[String], lastUpdate: Option[DateTime], version: Option[Int], groups: Option[Seq[PartialConsentGroup]])

object PartialConsentOffer {
  def merge(partialOffers: Seq[PartialConsentOffer], existingOffers: Option[Seq[ConsentOffer]]): Option[Seq[ConsentOffer]] = {
    if (partialOffers.isEmpty && existingOffers.isEmpty) {
      None
    } else {
      val flattenOffers = existingOffers.toList.flatten
      val updatedOffers: Seq[ConsentOffer] = flattenOffers.map { o =>
        partialOffers.find(pg => pg.key == o.key).fold(o) { po =>
          o.copy(
            label = po.label.getOrElse(o.label),
            lastUpdate = po.lastUpdate.getOrElse(o.lastUpdate),
            version = po.version.getOrElse(o.version),
            groups = PartialConsentGroup.merge(po.groups.toList.flatten, o.groups)
          )
        }
      }
      val newOffers: Seq[ConsentOffer] = partialOffers.filter(po => !flattenOffers.exists(o => po.key == o.key)).map { po =>
        ConsentOffer(
          key = po.key,
          label = po.label.getOrElse(""),
          version = po.version.getOrElse(0),
          lastUpdate = po.lastUpdate.getOrElse(DateTime.now(DateTimeZone.UTC)),
          groups = po.groups.toList.flatten.map(pg => ConsentGroup(
            key = pg.key,
            label = pg.label.getOrElse(""),
            consents = pg.consents.map(_.map(_.toConsent)).toList.flatten
          ))
        )
      }
      Some(updatedOffers ++ newOffers)
    }
  }

  implicit val format =  {
    implicit val dateRead = DateUtils.utcDateTimeFormats
    Json.format[PartialConsentOffer]
  }

  implicit val partialConsentOfferReadXml: XMLRead[PartialConsentOffer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validateNullable[String](Some(s"${path.convert()}label")),
        (node \ "lastUpdate").validateNullable[DateTime](Some(s"${path.convert()}lastUpdate")),
        (node \ "version").validateNullable[Int](Some(s"${path.convert()}version")),
        (node \ "groups").validateNullable[Seq[PartialConsentGroup]](Some(s"${path.convert()}consents"))
      ).mapN((key, label, lastUpdate, version, groups) => PartialConsentOffer(key, label, lastUpdate, version, groups))
}
case class PartialConsentFact(
                        _id: Option[String] = None,
                        userId: Option[String] = None,
                        doneBy: Option[DoneBy] = None,
                        version: Option[Int] = None,
                        lastUpdate: Option[DateTime] = None,
                        groups: Option[Seq[PartialConsentGroup]] = None,
                        offers: Option[Seq[PartialConsentOffer]] = None,
                        orgKey: Option[String] = None,
                        metaData: Option[Map[String, String]] = None,
                        sendToKafka: Option[Boolean] = None) {
  def applyTo(lastConsentFact: ConsentFact, currentVersion: VersionInfo): ConsentFact = {
    val finalConsent = lastConsentFact.copy(
        _id = BSONObjectID.generate().stringify,
        userId = userId.getOrElse(lastConsentFact.userId),
        doneBy = doneBy.getOrElse(lastConsentFact.doneBy),
        version = version.getOrElse(currentVersion.num),
        lastUpdate = lastUpdate.getOrElse(lastConsentFact.lastUpdate),
        lastUpdateSystem = DateTime.now(DateTimeZone.UTC),
        groups = groups.map(g => PartialConsentGroup.merge(g, lastConsentFact.groups)).getOrElse(lastConsentFact.groups),
        offers = offers.map(o => PartialConsentOffer.merge(o, lastConsentFact.offers)).getOrElse(lastConsentFact.offers),
        orgKey = orgKey.orElse(lastConsentFact.orgKey),
        metaData = metaData.map(meta => lastConsentFact.metaData.map(m => m.combine(meta)).getOrElse(meta)).orElse(lastConsentFact.metaData),
        sendToKafka = sendToKafka.orElse(lastConsentFact.sendToKafka)
    )
    println(s"Merged data : $this, \n $lastConsentFact, \n $finalConsent")
    finalConsent
  }
}

object PartialConsentFact extends ReadableEntity[PartialConsentFact] {

  implicit val format = {
    implicit val dateRead = DateUtils.utcDateTimeFormats
    Json.format[PartialConsentFact]
  }

  implicit val partialConsentFactReadXml: XMLRead[PartialConsentFact] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](Some(s"${path.convert()}_id")),
        (node \ "userId").validateNullable[String](Some(s"${path.convert()}userId")),
        (node \ "doneBy").validateNullable[DoneBy](Some(s"${path.convert()}doneBy")),
        (node \ "version").validateNullable[Int](Some(s"${path.convert()}version")),
        (node \ "lastUpdate").validateNullable[DateTime](Some(s"${path.convert()}lastUpdate")),
        (node \ "groups").validateNullable[Seq[PartialConsentGroup]](Some(s"${path.convert()}consents")),
        (node \ "offers").validateNullable[Seq[PartialConsentOffer]](Some(s"${path.convert()}offers")),
        (node \ "orgKey").validateNullable[String](Some(s"${path.convert()}orgKey")),
        (node \ "metaData").validateNullable[Seq[Metadata]](Some(s"${path.convert()}metaData")),
        (node \ "sendToKafka").validateNullable[Boolean](Some(s"${path.convert()}sendToKafka"))
      ).mapN((_id, userId, doneBy, version, lastUpdate, groups, offers, orgKey, metadata, sendToKafka) =>
        PartialConsentFact(
          _id,
          userId,
          doneBy,
          version,
          lastUpdate,
          groups,
          offers,
          orgKey,
          metadata.map(m => m.map(ev => (ev.key, ev.value)).toMap),
          sendToKafka
        )
      )

  override def fromXml(xml: Elem): Either[AppErrors, PartialConsentFact] = PartialConsentFact.partialConsentFactReadXml.read(xml).toEither

  override def fromJson(json: JsValue): Either[AppErrors, PartialConsentFact] = PartialConsentFact.format.reads(json).toEither(AppErrors.fromJsError _ )
}

// A user will have multiple consent facts
case class ConsentFact(
    _id: String = BSONObjectID.generate().stringify,
    userId: String,
    doneBy: DoneBy,
    version: Int,
    groups: Seq[ConsentGroup],
    offers: Option[Seq[ConsentOffer]] = None,
    lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
    lastUpdateSystem: DateTime = DateTime.now(DateTimeZone.UTC),
    orgKey: Option[String] = None,
    metaData: Option[Map[String, String]] = None,
    sendToKafka: Option[Boolean] = None
) extends ModelTransformAs {

  def notYetSendToKafka() = this.copy(sendToKafka = Some(false))
  def nowSendToKafka()    = this.copy(sendToKafka = Some(true))

  def asJson() =
    transform(ConsentFact.consentFactWritesWithoutId.writes(this))

  private def transform(jsValue: JsValue): JsValue =
    offers match {
      case Some(o) if o.isEmpty =>
        jsValue.as[JsObject] - "offers"
      case _                    =>
        jsValue

    }

  def asXml(): Elem = <consentFact>
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
      {groups.map(_.asXml())}
    </groups>

    {
    offers match {
      case Some(seqOffer) if seqOffer.isEmpty => ""
      case Some(l)                            => <offers>{l.map(_.asXml())}</offers>
      case None                               => ""
    }
  }
    <lastUpdate>
      {lastUpdate.toString(DateUtils.utcDateFormatter)}
    </lastUpdate>
    <orgKey>
      {orgKey.getOrElse("")}
    </orgKey>{
    if (metaData.isDefined)
      metaData.map { md =>
        <metaData>
          {md.map(e => <metaDataEntry key={e._1} value={e._2}/>)}
        </metaData>
      }.get
  }
  </consentFact>.clean()
}

object ConsentFact extends ReadableEntity[ConsentFact] {
  def newWithoutIdAndLastUpdate(
      userId: String,
      doneBy: DoneBy,
      version: Int,
      groups: Seq[ConsentGroup],
      offers: Option[Seq[ConsentOffer]] = None,
      lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
      orgKey: Option[String] = None,
      metaData: Option[Map[String, String]] = None,
      sendToKafka: Option[Boolean] = None
  ) =
    ConsentFact(
      _id = BSONObjectID.generate().stringify,
      userId = userId,
      doneBy = doneBy,
      version = version,
      groups = groups,
      offers = offers,
      lastUpdate = lastUpdate,
      orgKey = orgKey,
      metaData = metaData
    )

  def newWithoutKafkaFlag(
      _id: String = BSONObjectID.generate().stringify,
      userId: String,
      doneBy: DoneBy,
      version: Int,
      groups: Seq[ConsentGroup],
      offers: Option[Seq[ConsentOffer]] = None,
      lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
      lastUpdateSystem: DateTime = DateTime.now(DateTimeZone.UTC),
      orgKey: Option[String] = None,
      metaData: Option[Map[String, String]] = None
  ) =
    ConsentFact(
      _id = _id,
      userId = userId,
      doneBy = doneBy,
      version = version,
      groups = groups,
      offers = offers,
      lastUpdate = lastUpdate,
      lastUpdateSystem = lastUpdateSystem,
      orgKey = orgKey,
      metaData = metaData
    )

  val consentFactReadsWithoutIdAndLastUpdate: Reads[ConsentFact] = (
    (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "offers").readNullable[Seq[ConsentOffer]] and
      (__ \ "lastUpdate").readWithDefault[DateTime](DateTime.now(DateTimeZone.UTC))(DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]] and
      (__ \ "sendToKafka").readNullable[Boolean]
  )(ConsentFact.newWithoutIdAndLastUpdate _)

  val consentFactReads: Reads[ConsentFact] = (
    (__ \ "_id").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "offers").readNullable[Seq[ConsentOffer]] and
      (__ \ "lastUpdate").read[DateTime](DateUtils.utcDateTimeReads) and
      (__ \ "lastUpdateSystem").read[DateTime](DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]]
  )(ConsentFact.newWithoutKafkaFlag _)

  val consentFactWrites: Writes[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(unlift(ConsentFact.unapply))

  val consentFactWritesWithoutId: Writes[ConsentFact] = (
    (JsPath \ "_id").writeNullable[String].contramap((_: String) => None) and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .writeNullable[DateTime](DateUtils.utcDateTimeWrites)
        .contramap((_: DateTime) => None) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(unlift(ConsentFact.unapply))

  val consentFactOWrites: OWrites[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(unlift(ConsentFact.unapply))

  val consentFactFormats: Format[ConsentFact]            =
    Format(consentFactReads, consentFactWrites)
  implicit val consentFactOFormats: OFormat[ConsentFact] =
    OFormat(consentFactReads, consentFactOWrites)

  def template(orgVerNum: Int, groups: Seq[ConsentGroup], offers: Option[Seq[ConsentOffer]] = None, orgKey: String) =
    ConsentFact(
      _id = null,
      userId = "fill",
      doneBy = DoneBy("fill", "fill"),
      version = orgVerNum,
      groups = groups,
      offers = offers,
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
          .validateNullable[DateTime](DateTime.now(DateTimeZone.UTC), Some(s"${path.convert()}lastUpdate")),
        (node \ "orgKey").validateNullable[String](Some(s"${path.convert()}orgKey")),
        (node \ "version").validate[Int](Some(s"${path.convert()}version")),
        (node \ "groups").validate[Seq[ConsentGroup]](Some(s"${path.convert()}groups")),
        (node \ "offers").validateNullable[Seq[ConsentOffer]](Some(s"${path.convert()}offers")),
        (node \ "metaData").validateNullable[Seq[Metadata]](Some(s"${path.convert()}metaData"))
      ).mapN { (id, userId, doneBy, lastUpdate, orgKey, version, groups, offers, metadata) =>
        ConsentFact(
          _id = id,
          userId = userId,
          doneBy = doneBy,
          version = version,
          lastUpdate = lastUpdate,
          orgKey = orgKey,
          groups = groups,
          offers = offers,
          metaData = metadata.map(m => m.map(ev => (ev.key, ev.value)).toMap)
        )
      }

  def fromXml(xml: Elem): Either[AppErrors, ConsentFact] =
    readXml.read(xml, Some("consentFact")).toEither

  def fromJson(json: JsValue): Either[AppErrors, ConsentFact] =
    json.validate[ConsentFact](ConsentFact.consentFactReadsWithoutIdAndLastUpdate) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }

  def addOrgKey(consentFact: ConsentFact, orgKey: String): ConsentFact =
    consentFact.copy(orgKey = Some(orgKey))
}
