package db.postgres

import db.ApiKeyDataStore
import models.ApiKey
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}

class ApiKeyPostgresDataStore()(implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[ApiKey]
    with ApiKeyDataStore {

  val format: OFormat[ApiKey] = models.ApiKey.oformats

  override val tableName = "api_keys"

  def findById(_id: String): Future[Option[ApiKey]] = {
    findOneById("", _id)
  }

  def findByClientId(clientId: String): Future[Option[ApiKey]] = {
    findOneByQuery("", Json.obj("clientId" -> clientId))
  }

  def findMany(): Future[Seq[ApiKey]] =
    findMany("")

  def insertOne(objToInsert: ApiKey): Future[Boolean] =
    insertOne("", objToInsert)

  def updateOne(_id: String, objToInsert: ApiKey): Future[Boolean] =
    updateOne("", _id, objToInsert)

  def deleteOne(_id: String): Future[Boolean] =
    deleteOneById("", _id)

  def findManyPaginate(page: Int, pageSize: Int): Future[(Seq[ApiKey], Int)] =
    findManyByQueryPaginateCount("",
                                 Json.obj(),
                                 Json.obj("_id" -> -1),
                                 page,
                                 pageSize)

  def deleteAll(): Future[Boolean] = deleteByQuery("", Json.obj())
}
