package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import models._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.akkastream.{AkkaStreamCursor, State, cursorProducer}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class UserMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[User] {

  val format: OFormat[User] = models.User.formats

  override def collectionName(tenant: String) = s"$tenant-users"

  override def indices = Seq(
    Index(key = Seq("orgKey" -> IndexType.Ascending,
                    "userId" -> IndexType.Ascending),
          name = Some("orgKey_userId"),
          unique = true,
          sparse = true),
    Index(Seq("orgKey" -> IndexType.Ascending),
          name = Some("orgKey"),
          unique = false,
          sparse = true)
  )

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
      implicit m: Materializer): Future[Source[JsValue, Future[State]]] = {
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj(),
              Some(
                Json.obj(
                  "_id" -> 0,
                  "userId" -> 1,
                  "orgKey" -> 1,
                  "orgVersion" -> 1
                )))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  def streamAllConsentFactIds(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[State]]] = {
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj(),
              Some(
                Json.obj(
                  "_id" -> 0,
                  "latestConsentFactId" -> 1
                )))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  def streamAllUsersConsentFacts(tenant: String)(
      implicit m: Materializer): Future[Source[JsValue, Future[State]]] = {
    storedCollection(tenant).map { users =>
      import users.BatchCommands.AggregationFramework.Lookup

      users
        .aggregatorContext[JsValue](
          Lookup(s"$tenant-consentFacts",
                 "latestConsentFactId",
                 "_id",
                 "consentFact"))
        .prepared[AkkaStreamCursor]
        .cursor
        .documentSource()
    }
  }

  def deleteUserByTenant(tenant: String): Future[Boolean] = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))
  }

}
