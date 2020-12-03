package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import cats.implicits.toShow
import models.ExtractionTask
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.akkastream.{cursorProducer, State}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.{immutable, Seq}

class ExtractionTaskMongoDataStore(val mongoApi: ReactiveMongoApi)(implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ExtractionTask] {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import models.ExtractionTaskStatus._
  import lax._
  import bson2json._
  import json2bson._

  val format: OFormat[ExtractionTask]         = models.ExtractionTask.fmt
  override def collectionName(tenant: String) = s"$tenant-extractionTasks"

  override def indices = Seq(
    Index(immutable.Seq("orgKey" -> IndexType.Ascending), name = Some("orgKey"), unique = false, sparse = true),
    Index(immutable.Seq("userId" -> IndexType.Ascending), name = Some("userId"), unique = false, sparse = true)
  )

  def insert(tenant: String, extractionTask: ExtractionTask): Future[Boolean] =
    insertOne(tenant, extractionTask)

  def updateById(tenant: String, id: String, extractionTask: ExtractionTask): Future[Boolean] =
    updateOne(tenant, id, extractionTask)

  def findById(tenant: String, id: String): Future[Option[ExtractionTask]] =
    findOneById(tenant, id)

  def findAll(tenant: String, page: Int, pageSize: Int): Future[(Seq[ExtractionTask], Long)] =
    findManyByQueryPaginateCount(tenant, query = Json.obj(), sort = Json.obj("orgKey" -> 1, "userId" -> 1), page = page, pageSize = pageSize)

  def findAllByOrgKey(tenant: String, orgKey: String, page: Int, pageSize: Int): Future[(Seq[ExtractionTask], Long)] =
    findManyByQueryPaginateCount(tenant, query = Json.obj("orgKey" -> orgKey), sort = Json.obj("userId" -> 1),page = page, pageSize = pageSize)

  def findAllByUserId(tenant: String, userId: String, page: Int, pageSize: Int): Future[(Seq[ExtractionTask], Long)] =
    findManyByQueryPaginateCount(tenant, query = Json.obj("userId" -> userId), sort = Json.obj("userId" -> 1), page = page, pageSize = pageSize)

  def streamAllByState(tenant: String, status: ExtractionTaskStatus)(implicit
      m: Materializer
  ): Future[Source[JsValue, Future[State]]]                                                                          =
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj("status" -> status.show))
        .cursor[JsValue]()
        .documentSource()
    }

  def deleteExtractionTaskByTenant(tenant: String): Future[Boolean] =
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
}
