package db

import models.Account

import scala.concurrent.Future

trait AccountDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def deleteOneById(tenant: String, id: String): Future[Boolean]

  def deleteAccountByTenant(tenantKey: String): Future[Boolean]

  def delete(tenant: String, accountId: String): Future[Boolean]

  def update(tenant: String,
             accountId: String,
             account: Account): Future[Boolean]

  def create(tenant: String, account: Account): Future[Boolean]

  def findAll(tenant: String, page: Int, pageSize: Int): Future[Seq[Account]]

  def findByAccountId(tenant: String,
                      accountId: String): Future[Option[Account]]

}
