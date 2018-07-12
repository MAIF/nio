package models

import cats.data.Validated._
import cats.implicits._
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.json.Json

import scala.xml.NodeSeq

case class PermissionGroup(key: String,
                           label: String,
                           permissions: Seq[Permission]) {
  def asXml = {
    <permissionGroup>
      <key>{key}</key>
      <label>{label}</label>
      <permissions>{permissions.map(_.asXml)}</permissions>
    </permissionGroup>.clean()
  }
}

object PermissionGroup {
  implicit val json = Json.format[PermissionGroup]

  implicit val readXml: XMLRead[PermissionGroup] = (node: NodeSeq) =>
    (
      (node \ "key").validate[String],
      (node \ "label").validate[String],
      (node \ "permissions").validate[Seq[Permission]]
    ).mapN(PermissionGroup.apply)

}
