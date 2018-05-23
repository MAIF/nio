package models

import play.api.libs.json.JsValue

import scala.xml.Elem

trait ModelTransformAs {

  def asXml(): Elem
  def asJson(): JsValue
}
