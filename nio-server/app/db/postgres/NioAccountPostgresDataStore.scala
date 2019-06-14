package db.postgres

import db.NioAccountDataStore
import models.NioAccount
import play.api.libs.json.{JsObject, Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}

class NioAccountPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[NioAccount]
    with NioAccountDataStore {

  val format: OFormat[NioAccount] = models.NioAccount.oformats

  override val tableName = "nio_accounts"

  def findById(_id: String): Future[Option[NioAccount]] = {
    findOneById("", _id)
  }
  def findByEmail(email: String): Future[Option[NioAccount]] = {
    findOneByQuery("", Json.obj("email" -> email))
  }
  def findMany(): Future[Seq[NioAccount]] =
    findMany("")

  def insertOne(objToInsert: NioAccount): Future[Boolean] =
    insertOne("", objToInsert)

  def updateOne(_id: String, objToInsert: NioAccount): Future[Boolean] =
    updateOne("", _id, objToInsert)

  def deleteOne(_id: String): Future[Boolean] =
    deleteOneById("", _id)

  def findManyPaginate(page: Int,
                       pageSize: Int): Future[(Seq[NioAccount], Int)] =
    findManyByQueryPaginateCount("",
                                 Json.obj(),
                                 Json.obj("_id" -> -1),
                                 page,
                                 pageSize)

  def deleteAll(): Future[Boolean] = deleteByQuery("", Json.obj())
}
