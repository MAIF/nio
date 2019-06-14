package db

import models.CatchupLock

import scala.concurrent.Future

trait CatchupLockDataStore {

  def init(): Future[Unit]

  def createLock(tenant: String): Future[Boolean]

  def findLock(tenant: String): Future[Option[CatchupLock]]
}
