package db.mongo

import db.ApiKeyDataStore
import models.ApiKey
import play.api.libs.json.{Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class ApiKeyMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ApiKey]
    with ApiKeyDataStore {

  val format: OFormat[ApiKey] = models.ApiKey.oformats

  override protected def collectionName(tenant: String = ""): String = {
    s"ApiKey"
  }

  override protected def indices: Seq[Index] = Seq(
    Index(Seq("clientId" -> IndexType.Ascending),
          name = Some("clientId"),
          unique = true,
          sparse = true)
  )

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

  override def deleteAll(): Future[Boolean] = deleteByQuery("", Json.obj())
}
