package models

import play.api.libs.json.{JsArray, Json}
import scala.collection.Seq

case class PagedConsentFacts(page: Int, pageSize: Int, count: Long, items: Seq[ConsentFact]) extends ModelTransformAs {

  def asJson() =
    Json.obj("page" -> page, "pageSize" -> pageSize, "count" -> count, "items" -> JsArray(items.map(_.asJson())))

  def asXml()  = <pagedConsentFacts>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map(_.asXml())}</items>
    </pagedConsentFacts>

}
