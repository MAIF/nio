package db

import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import akka.stream.Materializer
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-users"))

  implicit def format: Format[User] = User.formats

  def insert(tenant: String, user: User) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(user).as[JsObject]).map(_.ok))

  def findByOrgKeyAndUserId(tenant: String, orgKey: String, userId: String) = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("orgKey" -> orgKey),
                         Json.obj("userId" -> userId)))
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
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("orgKey" -> orgKey), page, pageSize)
  }

  def findAll(tenant: String, page: Int, pageSize: Int) = {
    findAllByQuery(tenant, Json.obj(), page, pageSize)
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

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
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

  def ensureIndices(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      Future.sequence(
        Seq(
          col.indexesManager.ensure(
            Index(Seq("orgKey" -> IndexType.Ascending, "userId" -> IndexType.Ascending),
                  unique = true,
                  sparse = true)
          ),
          col.indexesManager.ensure(
            Index(Seq("orgKey" -> IndexType.Ascending),
              unique = false,
              sparse = true)
          )
        )
      )
    }
  }

}
