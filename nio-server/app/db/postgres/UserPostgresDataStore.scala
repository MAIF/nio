package db.postgres

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import db.UserDataStore
import models._
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}

class UserPostgresDataStore()(implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[User]
    with UserDataStore {

  val format: OFormat[User] = models.User.formats

  override val tableName = "users"

  def insert(tenant: String, user: User): Future[Boolean] =
    insertOne(tenant, user)

  def findByOrgKeyAndUserId(tenant: String,
                            orgKey: String,
                            userId: String): Future[Option[User]] = {
    val query = Json.obj("orgKey" -> orgKey, "userId" -> userId)
    findOneByQuery(tenant, query)
  }

  def updateById(tenant: String, id: String, user: User): Future[Boolean] = {
    updateOne(tenant, id, user)
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int,
                      maybeUserId: Option[String]): Future[(Seq[User], Int)] = {

    val query = maybeUserId match {
      case Some(userId) =>
        Json.obj("userId" -> userId, "orgKey" -> orgKey)
      case None => Json.obj("orgKey" -> orgKey)
    }
    findManyByQueryPaginateCount(tenant,
                                 query = query,
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int,
              maybeUserId: Option[String]): Future[(Seq[User], Int)] = {
    val query = maybeUserId match {
      case Some(userId) =>
        Json.obj("userId" -> userId)
      case None => Json.obj()
    }
    findManyByQueryPaginateCount(tenant,
                                 query = query,
                                 page = page,
                                 pageSize = pageSize)
  }

  def streamAll(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[Any]]] = {

    streamAsyncAsJsValue(tenant)
      .map(
        _.map(
          js =>
            Json.obj(
              "userId" -> (js \ "userId").as[String],
              "orgKey" -> (js \ "orgKey").as[String],
              "orgVersion" -> (js \ "orgVersion").as[Int]
          )))
  }

  def streamAllConsentFactIds(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[Any]]] = {
    streamAsyncAsJsValue(tenant)
      .map(
        _.map(js =>
          Json.obj(
            "latestConsentFactId" -> (js \ "latestConsentFactId").as[String])))
  }

  def deleteUserByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))
  }

}
