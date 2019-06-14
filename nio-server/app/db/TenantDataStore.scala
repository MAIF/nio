package db

import models.Tenant

import scala.concurrent.Future

trait TenantDataStore {

  def init(): Future[Unit]

  def insert(tenant: Tenant): Future[Boolean]

  def findAll(): Future[Seq[Tenant]]

  def findByKey(key: String): Future[Option[Tenant]]

  def removeByKey(key: String): Future[Boolean]

}
