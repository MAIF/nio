package models

import cats.data.Validated.*
import cats.implicits.*
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits.*
import libs.xml.syntax.*
import play.api.libs.functional.syntax.*
import play.api.libs.json.Reads.*
import play.api.libs.json.*
import reactivemongo.api.bson.BSONObjectID
import utils.DateUtils
import utils.Result.AppErrors
import utils.Result.AppErrors.*
import utils.json.JsResultOps

import java.time.{Clock, LocalDateTime}
import scala.xml.{Elem, NodeBuffer, NodeSeq}
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
  implicit val doneByFormats: OFormat[DoneBy] = Json.format[DoneBy]

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

case class Consent(key: String, label: String, checked: Boolean, expiredAt: Option[LocalDateTime] = None) {

  def isActive: Boolean = {
    val now = LocalDateTime.now(Clock.systemUTC())
    this.expiredAt.isEmpty || this.expiredAt.exists(d => d.isAfter(now))
  }

  def isExpired: Boolean = !isActive

  def asXml(): Elem = <consent>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <checked>
      {checked}
    </checked>
    {expiredAt.map(l => <expiredAt>{l.format(DateUtils.utcDateFormatter)}</expiredAt>).getOrElse(new NodeBuffer())}
  </consent>.clean()
}

object Consent {
  implicit val consentFormats: OFormat[Consent] = {
    implicit val dateFormat: Format[LocalDateTime] = DateUtils.utcDateTimeFormats
    Json.format[Consent]
  }

  implicit val xmlRead: XMLRead[Consent] =
    (xml: NodeSeq, path: Option[String]) => {
      (
        (xml \ "key").validate[String](Some(s"${path.convert()}key")),
        (xml \ "label").validate[String](Some(s"${path.convert()}label")),
        (xml \ "checked").validate[Boolean](Some(s"${path.convert()}checked")),
        (xml \ "expiredAt").validateNullable[LocalDateTime](Some(s"${path.convert()}expiredAt"))
      ).mapN(Consent.apply)
    }

}

case class ConsentGroup(key: String, label: String, consents: Seq[Consent]) {

  def activeConsents: Seq[Consent] = this.consents.filter(_.isActive)

  def asXml(): Elem = <consentGroup>
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
  implicit val consentGroupFormats: OFormat[ConsentGroup] = Json.format[ConsentGroup]

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
    lastUpdate: LocalDateTime = LocalDateTime.now(Clock.systemUTC()),
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
      {lastUpdate.format(DateUtils.utcDateFormatter)}
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
      (__ \ "lastUpdate").readWithDefault[LocalDateTime](LocalDateTime.now(Clock.systemUTC()))(DateUtils.utcDateTimeReads) and
      (__ \ "groups").read[Seq[ConsentGroup]]
  )(ConsentOffer.apply)

  implicit val offerWrites: Writes[ConsentOffer] = (
    (__ \   "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "lastUpdate").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (__ \ "groups").write[Seq[ConsentGroup]]
  )(o => (o.key, o.label, o.version, o.lastUpdate, o.groups))

  implicit val offerOWrites: OWrites[ConsentOffer] = (
    (__ \ "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "lastUpdate").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (__ \ "groups").write[Seq[ConsentGroup]]
  )(o => (o.key, o.label, o.version, o.lastUpdate, o.groups))

  implicit val format: Format[ConsentOffer]   = Format(offerReads, offerWrites)
  implicit val oformat: OFormat[ConsentOffer] =
    OFormat(offerReads, offerOWrites)

  implicit val offerReadXml: XMLRead[ConsentOffer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "version").validate[Int](Some(s"${path.convert()}version")),
        (node \ "lastUpdate").validate[LocalDateTime](Some(s"${path.convert()}lastUpdate")),
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
  def toConsent(permission: Option[Permission]): Consent = Consent(key, label.getOrElse(""), checked, permission.flatMap(_.getValidityPeriod))

}

object PartialConsent {

  def merge(pcs: Seq[PartialConsent], consents: Seq[Consent], permission: Seq[Permission]): Seq[Consent] = {
    val keyToPermission = permission.groupBy(_.key)
    consents.map { c =>
      val mayBePermission: Option[Permission] = keyToPermission.get(c.key).flatMap(_.headOption)
      pcs.find(pc => pc.key == c.key).fold(c) { pc =>
        c.copy(
          label = pc.label.getOrElse(c.label),
          checked = pc.checked,
          expiredAt = mayBePermission.flatMap(_.getValidityPeriod)
        )
      }
    } ++ pcs.filter(pc => !consents.exists(c => c.key == pc.key)).map { pc =>
      val mayBePermission: Option[Permission] = keyToPermission.get(pc.key).flatMap(_.headOption)
      Consent(pc.key, pc.label.getOrElse(""), pc.checked, mayBePermission.flatMap(_.getValidityPeriod))
    }
  }

  implicit val format: OFormat[PartialConsent] = Json.format[PartialConsent]
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

  def merge(partialGroups: Seq[PartialConsentGroup], existingGroups: Seq[ConsentGroup], permissionGroups: Seq[PermissionGroup]): Seq[ConsentGroup] = {
    val permissionsByKey: Map[String, Seq[PermissionGroup]] = permissionGroups.groupBy(_.key)
    existingGroups.map { g =>
      partialGroups.find(pg => pg.key == g.key).fold( g ) { pg =>
          val mayBePermission: Option[PermissionGroup] = permissionsByKey.get(pg.key).flatMap(_.headOption)
          g.copy(
            label = pg.label.getOrElse(g.label),
            consents = pg.consents.map(pcs => PartialConsent.merge(pcs, g.consents, mayBePermission.toList.flatMap(_.permissions))).getOrElse(g.consents)
          )
        }
    } ++ partialGroups.filter(pg => !existingGroups.exists(g => pg.key == g.key)).map { pcg =>
      val mayBePermission: List[Permission] = permissionsByKey.get(pcg.key).toList.flatMap(_.headOption).flatMap(_.permissions)
      ConsentGroup(
        pcg.key,
        pcg.label.getOrElse(""),
        pcg.consents.toList.flatten.map(pc => pc.toConsent(mayBePermission.find(_.key == pc.key)))
      )
    }
  }

  implicit val format: OFormat[PartialConsentGroup] = Json.format[PartialConsentGroup]
  implicit val partialConsentGroupReadXml: XMLRead[PartialConsentGroup] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validateNullable[String](Some(s"${path.convert()}label")),
        (node \ "consents").validateNullable[Seq[PartialConsent]](Some(s"${path.convert()}consents"))
      ).mapN((key, label, consents) => PartialConsentGroup(key, label, consents))
}

case class PartialConsentOffer(key: String, label: Option[String], lastUpdate: Option[LocalDateTime], version: Option[Int], groups: Option[Seq[PartialConsentGroup]])

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
            groups = PartialConsentGroup.merge(po.groups.toList.flatten, o.groups, Seq.empty)
          )
        }
      }
      val newOffers: Seq[ConsentOffer] = partialOffers.filter(po => !flattenOffers.exists(o => po.key == o.key)).map { po =>
        ConsentOffer(
          key = po.key,
          label = po.label.getOrElse(""),
          version = po.version.getOrElse(0),
          lastUpdate = po.lastUpdate.getOrElse(LocalDateTime.now(Clock.systemUTC)),
          groups = po.groups.toList.flatten.map(pg => ConsentGroup(
            key = pg.key,
            label = pg.label.getOrElse(""),
            consents = pg.consents.map(_.map(_.toConsent(None))).toList.flatten
          ))
        )
      }
      Some(updatedOffers ++ newOffers)
    }
  }

  implicit val format: OFormat[PartialConsentOffer] =  {
    implicit val dateRead: Format[LocalDateTime] = DateUtils.utcDateTimeFormats
    Json.format[PartialConsentOffer]
  }

  implicit val partialConsentOfferReadXml: XMLRead[PartialConsentOffer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validateNullable[String](Some(s"${path.convert()}label")),
        (node \ "lastUpdate").validateNullable[LocalDateTime](Some(s"${path.convert()}lastUpdate")),
        (node \ "version").validateNullable[Int](Some(s"${path.convert()}version")),
        (node \ "groups").validateNullable[Seq[PartialConsentGroup]](Some(s"${path.convert()}consents"))
      ).mapN((key, label, lastUpdate, version, groups) => PartialConsentOffer(key, label, lastUpdate, version, groups))
}
case class PartialConsentFact(
                        _id: Option[String] = None,
                        userId: Option[String] = None,
                        doneBy: Option[DoneBy] = None,
                        version: Option[Int] = None,
                        lastUpdate: Option[LocalDateTime] = Some(LocalDateTime.now(Clock.systemUTC)),
                        groups: Option[Seq[PartialConsentGroup]] = None,
                        offers: Option[Seq[PartialConsentOffer]] = None,
                        orgKey: Option[String] = None,
                        metaData: Option[Map[String, String]] = None,
                        sendToKafka: Option[Boolean] = None) {
  def applyTo(lastConsentFact: ConsentFact, organisation: Organisation): ConsentFact = {
    val finalConsent = lastConsentFact.copy(
        _id = BSONObjectID.generate().stringify,
        userId = userId.getOrElse(lastConsentFact.userId),
        doneBy = doneBy.getOrElse(lastConsentFact.doneBy),
        version = version.getOrElse(organisation.version.num),
        lastUpdate = lastUpdate.getOrElse(LocalDateTime.now(Clock.systemUTC)),
        lastUpdateSystem = LocalDateTime.now(Clock.systemUTC),
        groups = groups.map(g => PartialConsentGroup.merge(g, lastConsentFact.groups, organisation.groups)).getOrElse(lastConsentFact.groups),
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

  implicit val format: OFormat[PartialConsentFact] = {
    implicit val dateRead: Format[LocalDateTime] = DateUtils.utcDateTimeFormats
    Json.format[PartialConsentFact]
  }

  implicit val partialConsentFactReadXml: XMLRead[PartialConsentFact] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](Some(s"${path.convert()}_id")),
        (node \ "userId").validateNullable[String](Some(s"${path.convert()}userId")),
        (node \ "doneBy").validateNullable[DoneBy](Some(s"${path.convert()}doneBy")),
        (node \ "version").validateNullable[Int](Some(s"${path.convert()}version")),
        (node \ "lastUpdate").validateNullable[LocalDateTime](Some(s"${path.convert()}lastUpdate")),
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

  override def fromJson(json: JsValue): Either[AppErrors, PartialConsentFact] = PartialConsentFact.format.reads(json).toEither(AppErrors.fromJsError)
}

// A user will have multiple consent facts
case class ConsentFact(
    _id: String = BSONObjectID.generate().stringify,
    userId: String,
    doneBy: DoneBy,
    version: Int,
    groups: Seq[ConsentGroup],
    offers: Option[Seq[ConsentOffer]] = None,
    lastUpdate: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    lastUpdateSystem: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    orgKey: Option[String] = None,
    metaData: Option[Map[String, String]] = None,
    sendToKafka: Option[Boolean] = None
) extends ModelTransformAs {

  def notYetSendToKafka(): ConsentFact = this.copy(sendToKafka = Some(false))
  def nowSendToKafka(): ConsentFact = this.copy(sendToKafka = Some(true))

  def asJson(): JsValue =
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
      {groups.filter(cf => cf.consents.nonEmpty).map(_.asXml())}
    </groups>

    {
    offers match {
      case Some(seqOffer) if seqOffer.isEmpty => ""
      case Some(l)                            => <offers>{l.map(_.asXml())}</offers>
      case None                               => ""
    }
  }
    <lastUpdate>
      {lastUpdate.format(DateUtils.utcDateFormatter)}
    </lastUpdate>
    <orgKey>
      {orgKey.getOrElse("")}
    </orgKey>
    {metaData.map { md =>
          <metaData>
            {md.map(e => <metaDataEntry key={e._1} value={e._2}/>)}
          </metaData>
        }.getOrElse(new NodeBuffer())
    }
  </consentFact>.clean()

  case class KeyPermissionGroup(group: String, permission: String)

  def setUpValidityPeriods(organisation: Organisation): ConsentFact = {
    val indexedKeys: Seq[(KeyPermissionGroup, Permission)] = for {
      g <- organisation.groups
      p <- g.permissions
    } yield (KeyPermissionGroup(g.key, p.key), p)
    val indexed: Map[KeyPermissionGroup, Seq[(KeyPermissionGroup, Permission)]] = indexedKeys.groupBy(_._1)
    this.copy(groups = this.groups.map( group =>
      group.copy(
        consents = group.consents.map ( consent =>
          consent.copy(expiredAt = indexed.get(KeyPermissionGroup(group.key, consent.key))
            .flatMap(_.headOption)
            .flatMap(_._2.getValidityPeriod)
          )
        )
      )
    ))
  }

  def filterExpiredConsent(showExpiredConsents: Boolean): ConsentFact = {
    if (showExpiredConsents) {
      this
    } else {
      this.copy(groups = this.groups
        .map(group => group.copy(consents = group.activeConsents))
        .filter(group => group.consents.nonEmpty)
      )
    }
  }
}

object ConsentFact extends ReadableEntity[ConsentFact] {
  def newWithoutIdAndLastUpdate(
      userId: String,
      doneBy: DoneBy,
      version: Int,
      groups: Seq[ConsentGroup],
      offers: Option[Seq[ConsentOffer]] = None,
      lastUpdate: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
      orgKey: Option[String] = None,
      metaData: Option[Map[String, String]] = None,
      sendToKafka: Option[Boolean] = None
  ): ConsentFact =
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
      lastUpdate: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
      lastUpdateSystem: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
      orgKey: Option[String] = None,
      metaData: Option[Map[String, String]] = None
  ): ConsentFact =
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
      (__ \ "lastUpdate").readWithDefault[LocalDateTime](LocalDateTime.now(Clock.systemUTC))(DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]] and
      (__ \ "sendToKafka").readNullable[Boolean]
  )(ConsentFact.newWithoutIdAndLastUpdate)

  private val consentFactReads: Reads[ConsentFact] = (
    (__ \ "_id").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "doneBy").read[DoneBy] and
      (__ \ "version").read[Int] and
      (__ \ "groups").read[Seq[ConsentGroup]] and
      (__ \ "offers").readNullable[Seq[ConsentOffer]] and
      (__ \ "lastUpdate").read[LocalDateTime](DateUtils.utcDateTimeReads) and
      (__ \ "lastUpdateSystem").read[LocalDateTime](DateUtils.utcDateTimeReads) and
      (__ \ "orgKey").readNullable[String] and
      (__ \ "metaData").readNullable[Map[String, String]]
  )(ConsentFact.newWithoutKafkaFlag)

  private val consentFactWrites: Writes[ConsentFact] = (
      (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(cf => (cf._id, cf.userId, cf.doneBy, cf.version, cf.groups, cf.offers, cf.lastUpdate, cf.lastUpdateSystem, cf.orgKey, cf.metaData, cf.sendToKafka))

  private val consentFactWritesWithoutId: Writes[ConsentFact] = (
    (JsPath \ "_id").writeNullable[String].contramap((_: String) => None) and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem").writeNullable[LocalDateTime](DateUtils.utcDateTimeWrites).contramap((_: LocalDateTime ) => None) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(cf => (cf._id, cf.userId, cf.doneBy, cf.version, cf.groups, cf.offers, cf.lastUpdate, cf.lastUpdateSystem, cf.orgKey, cf.metaData, cf.sendToKafka))

  private val consentFactOWrites: OWrites[ConsentFact] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "doneBy").write[DoneBy](DoneBy.doneByFormats) and
      (JsPath \ "version").write[Int] and
      (JsPath \ "groups").write[Seq[ConsentGroup]] and
      (JsPath \ "offers").writeNullable[Seq[ConsentOffer]] and
      (JsPath \ "lastUpdate").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "lastUpdateSystem").write[LocalDateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "orgKey").writeNullable[String] and
      (JsPath \ "metaData").writeNullable[Map[String, String]] and
      (JsPath \ "sendToKafka").writeNullable[Boolean]
  )(cf => (cf._id, cf.userId, cf.doneBy, cf.version, cf.groups, cf.offers, cf.lastUpdate, cf.lastUpdateSystem, cf.orgKey, cf.metaData, cf.sendToKafka))

  val consentFactFormats: Format[ConsentFact]            = Format(consentFactReads, consentFactWrites)
  implicit val consentFactOFormats: OFormat[ConsentFact] = OFormat(consentFactReads, consentFactOWrites)

  def template(orgVerNum: Int, groups: Seq[ConsentGroup], offers: Option[Seq[ConsentOffer]] = None, orgKey: String): ConsentFact =
    ConsentFact(
      _id = null,
      userId = "fill",
      doneBy = DoneBy("fill", "fill"),
      version = orgVerNum,
      groups = groups,
      offers = offers,
      lastUpdate = LocalDateTime.now(Clock.systemUTC),
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
          .validateNullable[LocalDateTime](LocalDateTime.now(Clock.systemUTC), Some(s"${path.convert()}lastUpdate")),
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

sealed trait ConsentFactCommand

object ConsentFactCommand {
  case class PatchConsentFact(userId: String, command: PartialConsentFact) extends ConsentFactCommand

  object PatchConsentFact {
    val format: OFormat[PatchConsentFact] = Json.format[PatchConsentFact]
  }
  case class UpdateConsentFact(userId: String, command: ConsentFact) extends ConsentFactCommand

  object UpdateConsentFact {
    val format: OFormat[UpdateConsentFact] = OFormat[UpdateConsentFact](
      ((__ \ "userId").read[String] and
        (__ \ "command").read[ConsentFact](ConsentFact.consentFactReadsWithoutIdAndLastUpdate))(UpdateConsentFact.apply),
      Json.writes[UpdateConsentFact]
    )
  }

  implicit val format: Format[ConsentFactCommand] = Format(
    Reads[ConsentFactCommand] { js =>
      (js \ "type").validate[String].flatMap {
        case "Update" => UpdateConsentFact.format.reads(js)
        case "Patch" => PatchConsentFact.format.reads(js)
      }
    },
    Writes[ConsentFactCommand] {
      case c: UpdateConsentFact => UpdateConsentFact.format.writes(c) ++ Json.obj("type" -> "Update")
      case c: PatchConsentFact => PatchConsentFact.format.writes(c) ++ Json.obj("type" -> "Patch")
    }
  )

}

