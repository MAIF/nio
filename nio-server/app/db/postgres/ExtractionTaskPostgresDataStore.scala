package db.postgres

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import db.ExtractionTaskDataStore
import models.ExtractionTask
import models.ExtractionTaskStatus.ExtractionTaskStatus
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class ExtractionTaskPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[ExtractionTask]
    with ExtractionTaskDataStore {

  val format: OFormat[ExtractionTask] = models.ExtractionTask.fmt

  override val tableName = "extraction_tasks"

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
      implicit m: Materializer): Future[Source[JsValue, Future[Any]]] = {
    streamAsyncByQueryAsJsValue(tenant, Json.obj("status" -> status))
  }

  def deleteExtractionTaskByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }
}
