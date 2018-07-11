package models

import play.api.libs.json.Json

import scala.xml.Elem
import libs.xml.XmlUtil.XmlCleaner

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

  def fromXml(xml: Elem) = {
    val key = (xml \ "key").head.text
    val label = (xml \ "label").head.text
    val permissionsXml = (xml \ "permissions").head
    val permissions = permissionsXml.child.collect {
      case e: Elem => Permission.fromXml(e)
    }
    PermissionGroup(key, label, permissions)
  }
}
