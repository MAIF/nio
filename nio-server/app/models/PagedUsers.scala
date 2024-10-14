package models

import play.api.libs.json.{JsArray, JsObject, Json}
import libs.xml.XmlUtil.XmlCleaner

import scala.collection.Seq
import scala.xml.Elem

case class PagedUsers(page: Int, pageSize: Int, count: Long, items: Seq[User]) extends ModelTransformAs {

  def asJson(): JsObject =
    Json.obj(
      "page"     -> page,
      "pageSize" -> pageSize,
      "count"    -> count,
      "items"    -> JsArray(items.map(u => Json.obj("userId" -> u.userId, "orgKey" -> u.orgKey)))
    )

  def asXml(): Elem = <pagedUsers>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map(item => <user><userId>{item.userId}</userId><orgKey>{item.orgKey}</orgKey></user>)}</items>
    </pagedUsers>.clean()

}
