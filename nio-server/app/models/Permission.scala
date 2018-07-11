package models

import play.api.libs.json.Json

import scala.xml.Elem
import libs.xml.XmlUtil.XmlCleaner

case class Permission(key: String, label: String) {
  def asXml = {
    <permission>
      <key>{key}</key>
      <label>{label}</label>
    </permission>.clean()
  }
}

object Permission {
  implicit val formats = Json.format[Permission]

  def fromXml(xml: Elem) = {
    val key = (xml \ "key").head.text
    val label = (xml \ "label").head.text
    Permission(key, label)
  }
}
