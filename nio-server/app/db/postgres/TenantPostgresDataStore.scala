package db.postgres

import db.TenantDataStore
import models._
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}

class TenantPostgresDataStore()(implicit val ec: ExecutionContext)
    extends PostgresDataStoreUtils[Tenant]
    with TenantDataStore {

  override val tableName = "tenants"

  override val columns = Seq("payload")

  implicit def format: Format[Tenant] = Tenant.tenantFormats

  def findByKey(key: String): Future[Option[Tenant]] =
    findOneByQuery(Json.obj("key" -> key))

  def removeByKey(key: String): Future[Boolean] = {
    deleteByQuery(Json.obj("key" -> key))
  }

}
