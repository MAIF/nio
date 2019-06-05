package db.postgres

import akka.http.scaladsl.util.FastFuture
import db.CatchupLockDataStore
import models.CatchupLock
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class CatchupLockPostgresDataStore()(implicit val ec: ExecutionContext)
    extends PostgresDataStoreUtils[CatchupLock]
    with CatchupLockDataStore {

  override val tableName = "catchup_lock"

  val format: Format[CatchupLock] = CatchupLock.catchupLockFormats

  def createLock(tenant: String) = {
    findLock(tenant)
      .flatMap {
        case None =>
          insert(CatchupLock(tenant))
        case Some(_) =>
          Logger.debug(s"Stored collection already locked for tenant $tenant")
          FastFuture.successful(false)
      }
  }

  def findLock(tenant: String) = {
    findOneByQuery(Json.obj("tenant" -> tenant))
  }
}
