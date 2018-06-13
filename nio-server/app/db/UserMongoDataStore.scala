package db

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserMongoDataStore @Inject()(val reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext)
    extends DataStoreUtils {

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

  implicit def format: Format[User] = User.formats

  def insert(tenant: String, user: User) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(user).as[JsObject]).map(_.ok))

  def findByOrgKeyAndUserId(tenant: String, orgKey: String, userId: String) = {
    val query = Json.obj("orgKey" -> orgKey, "userId" -> userId)
    storedCollection(tenant).flatMap(_.find(query).one[User])
  }

  def updateById(tenant: String, id: String, user: User): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("_id" -> id), format.writes(user).as[JsObject])
        .map(_.ok))
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int,
                      maybeUserId: Option[String]) = {

    val query = maybeUserId match {
      case Some(userId) =>
        Json.obj(
          "userId" -> Json.obj("$regex" -> s".*$userId.*", "$options" -> "i"),
          "orgKey" -> orgKey)
      case None => Json.obj("orgKey" -> orgKey)
    }
    findAllByQuery(tenant, query, page, pageSize)
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int,
              maybeUserId: Option[String]) = {

    val query = maybeUserId match {
      case Some(userId) =>
        Json.obj(
          "userId" -> Json.obj("$regex" -> s".*$userId.*", "$options" -> "i"))
      case None => Json.obj()
    }

    findAllByQuery(tenant, query, page, pageSize)
  }

  def streamAll(tenant: String)(implicit m: Materializer) = {
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj(),
              Json.obj(
                "_id" -> 0,
                "userId" -> 1,
                "orgKey" -> 1,
                "orgVersion" -> 1
              ))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  def streamAllConsentFactIds(tenant: String)(implicit m: Materializer) = {
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj(),
              Json.obj(
                "_id" -> 0,
                "latestConsentFactId" -> 1
              ))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  private def findAllByQuery(tenant: String,
                             query: JsObject,
                             page: Int,
                             pageSize: Int) = {
    val options = QueryOpts(skipN = page * pageSize, pageSize)
    storedCollection(tenant).flatMap { coll =>
      for {
        count <- coll.count(Some(query))
        queryRes <- coll
          .find(query)
          .options(options)
          .cursor[User](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[User]]())
      } yield {
        (queryRes, count)
      }
    }
  }

  def deleteUserByTenant(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByOrgKey(tenant: String, orgKey: String) = {
    storedCollection(tenant).flatMap { col =>
      col.remove(Json.obj("orgKey" -> orgKey))
    }
  }

}
