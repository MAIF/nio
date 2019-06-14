package db

import models.UserExtractTask

import scala.concurrent.Future

trait UserExtractTaskDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def create(userExtractTask: UserExtractTask): Future[Boolean]

  def update(_id: String, userExtractTask: UserExtractTask): Future[Boolean]

  def delete(tenant: String, orgKey: String, userId: String): Future[Boolean]

  def find(tenant: String,
           orgKey: String,
           userId: String): Future[Option[UserExtractTask]]

  def findByOrgKey(tenant: String,
                   orgKey: String,
                   page: Int,
                   pageSize: Int): Future[(Seq[UserExtractTask], Int)]

  def findByOrgKeyAndUserId(tenant: String,
                            orgKey: String,
                            userId: String,
                            page: Int,
                            pageSize: Int): Future[(Seq[UserExtractTask], Int)]

  def deleteUserExtractTaskByTenant(tenant: String): Future[Boolean]
}
