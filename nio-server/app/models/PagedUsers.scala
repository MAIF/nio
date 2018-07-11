package models

import play.api.libs.json.{JsArray, Json}
import libs.xml.XmlUtil.XmlCleaner

case class PagedUsers(page: Int, pageSize: Int, count: Int, items: Seq[User])
    extends ModelTransformAs {

  def asJson = {
    Json.obj(
      "page" -> page,
      "pageSize" -> pageSize,
      "count" -> count,
      "items" -> JsArray(
        items.map(u => Json.obj("userId" -> u.userId, "orgKey" -> u.orgKey))))
  }

  def asXml = {
    <pagedUsers>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map{ item => <user><userId>{item.userId}</userId><orgKey>{item.orgKey}</orgKey></user>}}</items>
    </pagedUsers>.clean()
  }

}
