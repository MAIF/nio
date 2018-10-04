package models

import cats.data.Validated._
import cats.data.ValidatedNel
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
import utils.Result.{AppErrors, ErrorMessage, Result}

import scala.xml.{Elem, NodeSeq}

case class VersionInfo(status: String = "DRAFT",
                       num: Int = 1,
                       latest: Boolean = false,
                       neverReleased: Option[Boolean] = Some(true),
                       lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC)) {
  def copyUpdated = copy(lastUpdate = DateTime.now(DateTimeZone.UTC))
}

object VersionInfo {
  implicit val versionInfoWritesWithoutNeverReleased: Writes[VersionInfo] =
    Writes { versionInfo =>
      Json.obj(
        "status" -> versionInfo.status,
        "num" -> versionInfo.num,
        "latest" -> versionInfo.latest,
        "lastUpdate" -> versionInfo.lastUpdate.toString(
          DateUtils.utcDateFormatter)
      )
    }
  implicit val utcDateTimeFormats = DateUtils.utcDateTimeFormats
  implicit val formats = Json.format[VersionInfo]

  implicit val readXml: XMLRead[VersionInfo] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "status").validate[String](Some(s"${path.convert()}status")),
        (node \ "num").validate[Int](Some(s"${path.convert()}num")),
        (node \ "latest").validate[Boolean](Some(s"${path.convert()}latest")),
        (node \ "lastUpdate").validateNullable[DateTime](
          DateTime.now(DateTimeZone.UTC),
          Some(s"${path.convert()}lastUpdate"))
      ).mapN { (status, num, latest, lastUpdate) =>
        VersionInfo(status = status,
                    num = num,
                    latest = latest,
                    lastUpdate = lastUpdate)
    }
}

case class Offer(name: String, groups: Seq[PermissionGroup])
    extends ModelTransformAs {
  override def asXml(): Elem = <offer>
    <name>
      {name}
    </name>
    <groups>
      {groups.map(_.asXml)}
    </groups>
  </offer>.clean()

  override def asJson(): JsValue = Offer.offerWrites.writes(this)
}

object Offer extends ReadableEntity[Offer] {
  implicit val offerReads: Reads[Offer] = (
    (__ \ "name").read[String] and
      (__ \ "groups").read[Seq[PermissionGroup]]
  )(Offer.apply _)

  implicit val offerWrites: Writes[Offer] = (
    (__ \ "name").write[String] and
      (__ \ "groups").write[Seq[PermissionGroup]]
  )(unlift(Offer.unapply))

  implicit val offerOWrites: OWrites[Offer] = (
    (__ \ "name").write[String] and
      (__ \ "groups").write[Seq[PermissionGroup]]
  )(unlift(Offer.unapply))

  implicit val format: Format[Offer] = Format(offerReads, offerWrites)
  implicit val oformat: OFormat[Offer] = OFormat(offerReads, offerOWrites)

  implicit val offerReadXml: XMLRead[Offer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "name").validate[String](Some(s"${path.convert()}name")),
        (node \ "groups").validate[Seq[PermissionGroup]](
          Some(s"${path.convert()}groups"))
      ).mapN(
        (name, groups) => Offer(name, groups)
    )

  override def fromXml(xml: Elem): Either[AppErrors, Offer] =
    offerReadXml.read(xml, Some("offer")).toEither

  override def fromJson(json: JsValue): Either[AppErrors, Offer] =
    json.validate[Offer] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors)     => Left(AppErrors.fromJsError(errors))
    }
}

case class Organisation(_id: String = BSONObjectID.generate().stringify,
                        key: String,
                        label: String,
                        version: VersionInfo = VersionInfo(),
                        groups: Seq[PermissionGroup],
                        offers: Option[Seq[Offer]] = None)
    extends ModelTransformAs {

  def asJson = Organisation.organisationWritesWithoutId.writes(this)

  def asXml = <organisation>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <version>
      <status>
        {version.status}
      </status>
      <num>
        {version.num}
      </num>
      <latest>
        {version.latest}
      </latest>
      <lastUpdate>
        {version.lastUpdate.toString(DateUtils.utcDateFormatter)}
      </lastUpdate>
    </version>
    <groups>
      {groups.map(_.asXml)}
    </groups>{offers.map(l => <offers>l.map(_.asXml())</offers>)}
  </organisation>.clean()

  def newWith(version: VersionInfo) =
    this.copy(_id = BSONObjectID.generate().stringify, version = version)

  def isValidWith(cf: ConsentFact) = {
    if (cf.groups.length != groups.length) {
      Some("error.invalid.groups.length")
    } else {
      // check structure of org corresponds to the structure of consents
      (cf.groups.sortBy(_.key) zip groups.sortBy(_.key)).collectFirst {
        case (cg, og) if cg.consents.length != og.permissions.length =>
          "error.invalid.group.consents.length"
        case (cg, og) if cg.key != og.key     => "error.invalid.group.key"
        case (cg, og) if cg.label != og.label => "error.invalid.group.label"
        case (cg, og)
            if (cg.consents.sortBy(_.key) zip og.permissions.sortBy(_.key))
              .exists {
                case (cgc, ogp) =>
                  cgc.key != ogp.key || cgc.label != ogp.label
              } =>
          "error.invalid.group.consents.key.or.label"
      }
    }
  }
}

object Organisation extends ReadableEntity[Organisation] {

  implicit val organisationReads: Reads[Organisation] = (
    (__ \ "_id").readNullable[String].map { mayBeId =>
      mayBeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "key").read[String] and
      (__ \ "label").read[String] and
      (__ \ "version").readNullable[VersionInfo].map { maybeVersion =>
        maybeVersion.getOrElse(VersionInfo())
      } and
      (__ \ "groups").read[Seq[PermissionGroup]] and
      (__ \ "offers").readNullable[Seq[Offer]]
  )(Organisation.apply _)

  implicit val organisationWrites: Writes[Organisation] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "key").write[String] and
      (JsPath \ "label").write[String] and
      (JsPath \ "version").write[VersionInfo] and
      (JsPath \ "groups").write[Seq[PermissionGroup]] and
      (JsPath \ "offers").writeNullable[Seq[Offer]]
  )(unlift(Organisation.unapply))

  implicit val organisationOWrites: OWrites[Organisation] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "key").write[String] and
      (JsPath \ "label").write[String] and
      (JsPath \ "version").write[VersionInfo] and
      (JsPath \ "groups").write[Seq[PermissionGroup]] and
      (JsPath \ "offers").writeNullable[Seq[Offer]]
  )(unlift(Organisation.unapply))

  implicit val formats: Format[Organisation] =
    Format(organisationReads, organisationWrites)
  implicit val oFormats: OFormat[Organisation] =
    OFormat(organisationReads, organisationOWrites)

  implicit val organisationWritesWithoutId: Writes[Organisation] = Writes {
    org =>
      {

        val organisation: JsObject = Json.obj(
          "key" -> org.key,
          "label" -> org.label,
          "version" -> VersionInfo.versionInfoWritesWithoutNeverReleased.writes(
            org.version),
          "groups" -> org.groups
        )

        if (org.offers.isDefined) {
          organisation ++ Json.obj("offers" -> org.offers.get.map(_.asJson()))
        } else
          organisation
      }
  }

  implicit val readXml: XMLRead[Organisation] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](
          BSONObjectID.generate().stringify,
          Some(s"${path.convert()}_id")),
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "version").validate[VersionInfo](
          Some(s"${path.convert()}version")),
        (node \ "groups").validate[Seq[PermissionGroup]](
          Some(s"${path.convert()}groups")),
        (node \ "offers").validateNullable[Seq[Offer]](
          Some(s"${path.convert()}offers")),
      ).mapN(
        (_id, key, label, version, groups, offers) =>
          Organisation(_id = _id,
                       key = key,
                       label = label,
                       version = version,
                       groups = groups,
                       offers = offers)
    )

  def fromXml(xml: Elem): Either[AppErrors, Organisation] = {
    readXml.read(xml, Some("organisation")).toEither
  }

  def fromJson(json: JsValue) = {
    json.validate[Organisation] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }
}

case class Organisations(organisations: Seq[Organisation])
    extends ModelTransformAs {
  override def asXml(): Elem =
    <organisations>
      {organisations.map(_.asXml)}
    </organisations>.clean()

  override def asJson(): JsValue = JsArray(organisations.map(_.asJson))
}

object Organisations {}

case class VersionInfoLight(status: String, num: Int, lastUpdate: DateTime)

case class OrganisationLight(key: String,
                             label: String,
                             version: VersionInfoLight) {
  def asXml = <organisationLight>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <version>
      <status>
        {version.status}
      </status>
      <num>
        {version.num}
      </num>
      <lastUpdate>
        {version.lastUpdate.toString(DateUtils.utcDateFormatter)}
      </lastUpdate>
    </version>
  </organisationLight>.clean()

  def asJson = {
    Json.obj(
      "key" -> key,
      "label" -> label,
      "version" -> Json.obj(
        "status" -> version.status,
        "num" -> version.num,
        "lastUpdate" -> version.lastUpdate.toString(DateUtils.utcDateFormatter))
    )
  }
}

object OrganisationLight {
  def from(o: Organisation) = {
    OrganisationLight(
      key = o.key,
      label = o.label,
      version =
        VersionInfoLight(o.version.status, o.version.num, o.version.lastUpdate))
  }
}

case class OrganisationsLights(organisations: Seq[OrganisationLight])
    extends ModelTransformAs {
  override def asXml(): Elem =
    <organisationLights>
      {organisations.map(_.asXml)}
    </organisationLights>
      .clean()

  override def asJson(): JsValue = JsArray(organisations.map(_.asJson))
}

object OrganisationsLights {}

sealed trait ValidatorUtils {
  type ValidationResult[A] = ValidatedNel[String, A]

  def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) { (e, acc) =>
      for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  def sequence[A](s: Seq[ValidationResult[A]]): ValidationResult[List[A]] = {
    s.toList.sequence[ValidationResult, A]
  }

  def keyPattern = "^\\w+$"
}

object ValidatorUtils extends ValidatorUtils

sealed trait PermissionValidator {
  private def validateKey(
      key: String,
      indexGroup: Int,
      index: Int): ValidatorUtils.ValidationResult[String] = {
    key match {
      case k if k.matches(ValidatorUtils.keyPattern) => key.validNel
      case _ =>
        s"error.organisation.groups.$indexGroup.permissions.$index.key".invalidNel
    }
  }

  def validatePermission(
      permission: Permission,
      indexGroup: Int,
      index: Int): ValidatorUtils.ValidationResult[Permission] = {
    validateKey(permission.key, indexGroup, index).map(_ => permission)
  }
}

object PermissionValidator extends PermissionValidator

sealed trait GroupValidator {

  private def validateKey(
      key: String,
      index: Int): ValidatorUtils.ValidationResult[String] = {
    key match {
      case k if k.matches(ValidatorUtils.keyPattern) => key.validNel
      case _                                         => s"error.organisation.groups.$index.key".invalidNel
    }
  }

  private def validatePermissions(
      permissions: Seq[Permission],
      index: Int): ValidatorUtils.ValidationResult[Seq[Permission]] = {
    if (permissions.nonEmpty)
      permissions.validNel
    else
      s"error.organisation.groups.$index.permissions.empty".invalidNel
  }

  def validateGroup(
      group: PermissionGroup,
      index: Int): ValidatorUtils.ValidationResult[PermissionGroup] = {
    (
      validateKey(group.key, index),
      validatePermissions(group.permissions, index),
      ValidatorUtils.sequence(group.permissions.zipWithIndex.map {
        case (permission, indexPermission) =>
          PermissionValidator.validatePermission(permission,
                                                 index,
                                                 indexPermission)
      })
    ).mapN((_, _, _) => group)
  }
}

object GroupValidator extends GroupValidator

sealed trait OrganisationValidator {

  private def validateKey(
      key: String): ValidatorUtils.ValidationResult[String] = {
    key match {
      case k if k.matches(ValidatorUtils.keyPattern) => key.validNel
      case _                                         => "error.organisation.invalid.key".invalidNel
    }
  }

  private def validateGroups(groups: Seq[PermissionGroup])
    : ValidatorUtils.ValidationResult[Seq[PermissionGroup]] = {

    if (groups.nonEmpty)
      groups.validNel
    else
      "error.organisation.groups.empty".invalidNel
  }

  def validateOrganisation(organisation: Organisation): Result[Organisation] = {

    (
      validateKey(organisation.key),
      validateGroups(organisation.groups),
      ValidatorUtils.sequence(organisation.groups.zipWithIndex.map {
        case (group, index) => GroupValidator.validateGroup(group, index)
      })
    ).mapN((_, _, _) => organisation)
      .toEither
      .leftMap(s =>
        AppErrors(s.toList.map(errorMessage => ErrorMessage(errorMessage))))
  }
}

object OrganisationValidator extends OrganisationValidator
