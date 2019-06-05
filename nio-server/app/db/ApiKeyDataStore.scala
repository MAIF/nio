package db

import models.ApiKey

import scala.concurrent.Future

trait ApiKeyDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def findById(_id: String): Future[Option[ApiKey]]

  def findByClientId(clientId: String): Future[Option[ApiKey]]

  def findMany(): Future[Seq[ApiKey]]

  def insertOne(objToInsert: ApiKey): Future[Boolean]

  def updateOne(_id: String, objToInsert: ApiKey): Future[Boolean]

  def deleteOne(_id: String): Future[Boolean]

  def findManyPaginate(page: Int, pageSize: Int): Future[(Seq[ApiKey], Int)]

  def deleteAll(): Future[Boolean]

}
