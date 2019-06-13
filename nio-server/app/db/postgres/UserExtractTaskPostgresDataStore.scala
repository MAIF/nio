package db.postgres

import db.UserExtractTaskDataStore
import models.UserExtractTask
import play.api.libs.json.{Json, OFormat}
import scalikejdbc.async.AsyncDB
import scalikejdbc._
import async._

import scala.concurrent.{ExecutionContext, Future}

class UserExtractTaskPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[UserExtractTask]
    with UserExtractTaskDataStore {

  override implicit def format: OFormat[UserExtractTask] =
    UserExtractTask.oformat

  override val tableName = "user_extract_tasks"

  def create(userExtractTask: UserExtractTask): Future[Boolean] = {
    insertOne(userExtractTask.tenant, userExtractTask)
  }

  def update(_id: String, userExtractTask: UserExtractTask) = {
    updateOne(userExtractTask.tenant, _id, userExtractTask)
  }

  def delete(tenant: String,
             orgKey: String,
             userId: String): Future[Boolean] = {
    deleteByQuery(
      tenant,
      Json.obj("tenant" -> tenant, "orgKey" -> orgKey, "userId" -> userId))
  }

  def find(tenant: String,
           orgKey: String,
           userId: String): Future[Option[UserExtractTask]] = {

    val query = Json.obj("orgKey" -> orgKey, "userId" -> userId)

    AsyncDB withPool { implicit session =>
      sql"""select * from ${table} where tenant=${tenant}
           and payload @> ${query
        .toString()}::jsonb and payload->>'endedAt' is null"""
        .map(rs => fromResultSet(rs))
        .list()
        .future()
        .map(_.headOption)
    }
  }

  def findByOrgKey(tenant: String,
                   orgKey: String,
                   page: Int,
                   pageSize: Int): Future[(Seq[UserExtractTask], Int)] =
    findManyByQueryPaginateCount(
      tenant = tenant,
      query = Json.obj("tenant" -> tenant, "orgKey" -> orgKey),
      page = page,
      pageSize = pageSize)

  def findByOrgKeyAndUserId(
      tenant: String,
      orgKey: String,
      userId: String,
      page: Int,
      pageSize: Int): Future[(Seq[UserExtractTask], Int)] =
    findManyByQueryPaginateCount(
      tenant = tenant,
      query =
        Json.obj("tenant" -> tenant, "orgKey" -> orgKey, "userId" -> userId),
      page = page,
      pageSize = pageSize)

  def deleteUserExtractTaskByTenant(tenant: String): Future[Boolean] =
    deleteByTenant(tenant)
}
