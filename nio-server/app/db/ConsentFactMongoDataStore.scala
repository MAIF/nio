package db

import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConsentFactMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-consentFacts"))

  implicit def format: Format[ConsentFact] = ConsentFact.consentFactFormats

  def insert(tenant: String, consentFact: ConsentFact) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(consentFact).as[JsObject]).map(_.ok))

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
    storedCollection(tenant).flatMap(_.find(query).one[ConsentFact])
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("userId" -> userId), page, pageSize)
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
          .sort(Json.obj("lastUpdate" -> -1))
          .options(options)
          .cursor[ConsentFact](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize,
                        Cursor.FailOnError[Seq[ConsentFact]]())
      } yield {
        (queryRes, count)
      }
    }
  }

  def updateByUserId(tenant: String,
                     id: String,
                     consentFact: ConsentFact): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("userId" -> id),
               format.writes(consentFact).as[JsObject])
        .map(_.ok))
  }

  def findAll(tenant: String) =
    storedCollection(tenant).flatMap(
      _.find(Json.obj())
        .cursor[ConsentFact](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[ConsentFact]]()))

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def deleteConsentFactByTenant(tenant: String) = {
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
            Index(Seq("orgKey" -> IndexType.Ascending,
                      "userId" -> IndexType.Ascending),
                  name = Some("orgKey_userId"),
                  unique = false,
                  sparse = true)
          ),
          col.indexesManager.ensure(
            Index(Seq("orgKey" -> IndexType.Ascending),
                  unique = false,
                  sparse = true)
          ),
          col.indexesManager.ensure(
            Index(Seq("userId" -> IndexType.Ascending),
                  unique = false,
                  sparse = true)
          )
        )
      )
    }
  }

}
