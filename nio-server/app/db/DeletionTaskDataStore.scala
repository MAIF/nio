package db

import models._

import scala.concurrent.Future

trait DeletionTaskDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def insert(tenant: String, deletionTask: DeletionTask): Future[Boolean]

  def updateById(tenant: String,
                 id: String,
                 deletionTask: DeletionTask): Future[Boolean]

  def findById(tenant: String, id: String): Future[Option[DeletionTask]]

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[(Seq[DeletionTask], Int)]

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[DeletionTask], Int)]

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[DeletionTask], Int)]

  def deleteDeletionTaskByTenant(tenant: String): Future[Boolean]
}
