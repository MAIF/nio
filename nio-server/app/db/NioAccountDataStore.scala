package db

import models.NioAccount

import scala.concurrent.Future

trait NioAccountDataStore {

  def deleteAll(): Future[Boolean]

  def ensureIndices(key: String): Future[Unit]

  def findById(_id: String): Future[Option[NioAccount]]

  def findByEmail(email: String): Future[Option[NioAccount]]

  def findMany(): Future[Seq[NioAccount]]

  def insertOne(objToInsert: NioAccount): Future[Boolean]

  def updateOne(_id: String, objToInsert: NioAccount): Future[Boolean]

  def deleteOne(_id: String): Future[Boolean]

  def findManyPaginate(page: Int, pageSize: Int): Future[(Seq[NioAccount], Int)]

}
