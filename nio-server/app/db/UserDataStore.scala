package db

import akka.stream._
import akka.stream.scaladsl.Source
import models._
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait UserDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def insert(tenant: String, user: User): Future[Boolean]

  def findByOrgKeyAndUserId(tenant: String,
                            orgKey: String,
                            userId: String): Future[Option[User]]

  def updateById(tenant: String, id: String, user: User): Future[Boolean]

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int,
                      maybeUserId: Option[String]): Future[(Seq[User], Int)]

  def findAll(tenant: String,
              page: Int,
              pageSize: Int,
              maybeUserId: Option[String]): Future[(Seq[User], Int)]

  def streamAll(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[Any]]]

  def streamAllConsentFactIds(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[Any]]]

  def deleteUserByTenant(tenant: String): Future[Boolean]

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean]
}
