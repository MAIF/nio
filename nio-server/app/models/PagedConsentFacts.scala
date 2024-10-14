package models

import play.api.libs.json.{JsArray, JsObject, Json}

import scala.collection.Seq
import scala.xml.Elem

case class PagedConsentFacts(page: Int, pageSize: Int, count: Long, items: Seq[ConsentFact]) extends ModelTransformAs {

  def asJson(): JsObject =
    Json.obj("page" -> page, "pageSize" -> pageSize, "count" -> count, "items" -> JsArray(items.map(_.asJson())))

  def asXml(): Elem = <pagedConsentFacts>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map(_.asXml())}</items>
    </pagedConsentFacts>

}
