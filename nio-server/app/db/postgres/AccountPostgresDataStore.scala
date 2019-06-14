package db.postgres

import db.AccountDataStore
import models.{Account, _}
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}

class AccountPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[Account]
    with AccountDataStore {

  val format: OFormat[Account] = models.Account.oformat

  override val tableName = "accounts"

  def findByAccountId(tenant: String,
                      accountId: String): Future[Option[Account]] = {

    findOneByQuery(tenant, Json.obj("accountId" -> accountId))
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[Seq[Account]] = {

    findManyByQueryPaginate(tenant = tenant,
                            query = Json.obj(),
                            page = page,
                            pageSize = pageSize)
  }

  def update(tenant: String,
             accountId: String,
             account: Account): Future[Boolean] = {
    updateOneByQuery(tenant, Json.obj("accountId" -> accountId), account)
  }

  def create(tenant: String, account: Account): Future[Boolean] = {
    insertOne(tenant, account)
  }

  def delete(tenant: String, accountId: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("accountId" -> accountId))
  }

  def deleteAccountByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }
}
