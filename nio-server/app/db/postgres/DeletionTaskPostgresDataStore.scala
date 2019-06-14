package db.postgres

import db.DeletionTaskDataStore
import models._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class DeletionTaskPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[DeletionTask]
    with DeletionTaskDataStore {

  val format: OFormat[DeletionTask] = models.DeletionTask.deletionTaskFormats

  override val tableName = "deletion_tasks"

  def insert(tenant: String, deletionTask: DeletionTask): Future[Boolean] =
    insertOne(tenant, deletionTask)

  def updateById(tenant: String,
                 id: String,
                 deletionTask: DeletionTask): Future[Boolean] = {
    updateOne(tenant, id, deletionTask)
  }

  def findById(tenant: String, id: String): Future[Option[DeletionTask]] = {
    findOneById(tenant, id)
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[(Seq[DeletionTask], Int)] = {
    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj(),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[DeletionTask], Int)] = {
    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj("orgKey" -> orgKey),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[DeletionTask], Int)] = {
    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj("userId" -> userId),
                                 page = page,
                                 pageSize = pageSize)
  }

  def deleteDeletionTaskByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }
}
