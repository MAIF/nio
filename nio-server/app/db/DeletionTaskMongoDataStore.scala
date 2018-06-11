package db

import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeletionTaskMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-deletionTasks"))

  implicit def format: Format[DeletionTask] = DeletionTask.deletionTaskFormats

  def insert(tenant: String, deletionTask: DeletionTask) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(deletionTask).as[JsObject]).map(_.ok))

  def updateById(tenant: String,
                 id: String,
                 deletionTask: DeletionTask): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("_id" -> id), deletionTask.asJson)
        .map(_.ok))
  }

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
    storedCollection(tenant).flatMap(_.find(query).one[DeletionTask])
  }

  def findAll(tenant: String, page: Int, pageSize: Int) = {
    findAllByQuery(tenant, Json.obj(), page, pageSize)
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("orgKey" -> orgKey), page, pageSize)
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
          .cursor[DeletionTask](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize,
                        Cursor.FailOnError[Seq[DeletionTask]]())
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

  def ensureIndices(tenant: String) = {
    reactiveMongoApi.database
      .map(_.collectionNames)
      .flatMap(collectionNames => {
        collectionNames.flatMap(
          cols =>
            cols.find(c => c == s"$tenant-deletionTasks") match {
              case Some(_) =>
                storedCollection(tenant).flatMap {
                  col =>
                    Future.sequence(
                      Seq(
                        col.indexesManager.ensure(
                          Index(Seq("orgKey" -> IndexType.Ascending),
                                name = Some("orgKey"),
                                unique = false,
                                sparse = true)
                        ),
                        col.indexesManager.ensure(
                          Index(Seq("userId" -> IndexType.Ascending),
                                name = Some("userId"),
                                unique = false,
                                sparse = true)
                        )
                      )
                    )
                }
              case None =>
                Logger.error(s"unknow collection $tenant-deletionTasks")
                Future {
                  Seq()
                }
          }
        )
      })

  }

}
