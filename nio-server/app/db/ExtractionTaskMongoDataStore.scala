package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import models.ExtractionTask
import models.ExtractionTaskStatus.ExtractionTaskStatus
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.{State, cursorProducer}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class ExtractionTaskMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ExtractionTask] {

  val format: OFormat[ExtractionTask] = models.ExtractionTask.fmt
  override def collectionName(tenant: String) = s"$tenant-extractionTasks"

  override def indices = Seq(
    Index(Seq("orgKey" -> IndexType.Ascending),
          name = Some("orgKey"),
          unique = false,
          sparse = true),
    Index(Seq("userId" -> IndexType.Ascending),
          name = Some("userId"),
          unique = false,
          sparse = true)
  )

  def insert(tenant: String, extractionTask: ExtractionTask): Future[Boolean] =
    insertOne(tenant, extractionTask)

  def updateById(tenant: String,
                 id: String,
                 extractionTask: ExtractionTask): Future[Boolean] = {
    updateOne(tenant, id, extractionTask)
  }

  def findById(tenant: String, id: String): Future[Option[ExtractionTask]] = {
    findOneById(tenant, id)
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[(Seq[ExtractionTask], Int)] = {
    findManyByQueryPaginateCount(tenant,
                                 query = Json.obj(),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ExtractionTask], Int)] = {
    findManyByQueryPaginateCount(tenant,
                                 query = Json.obj("orgKey" -> orgKey),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ExtractionTask], Int)] = {
    findManyByQueryPaginateCount(tenant,
                                 query = Json.obj("userId" -> userId),
                                 page = page,
                                 pageSize = pageSize)
  }

  def streamAllByState(tenant: String, status: ExtractionTaskStatus)(
      implicit m: Materializer): Future[Source[JsValue, Future[State]]] = {
    storedCollection(tenant).map { col =>
      col
        .find(Json.obj("status" -> status))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  def deleteExtractionTaskByTenant(tenant: String): Future[Boolean] = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }
}
