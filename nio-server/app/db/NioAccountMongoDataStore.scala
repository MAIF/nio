package db

import models.NioAccount
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class NioAccountMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[NioAccount] {

  val format: OFormat[NioAccount] = models.NioAccount.oformats

  override protected def collectionName(tenant: String = ""): String = {
    s"UserAccount"
  }

  override protected def indices: Seq[Index] = Seq(
    Index(Seq("email" -> IndexType.Ascending),
          name = Some("email"),
          unique = true,
          sparse = true),
    Index(Seq("clientId" -> IndexType.Ascending),
          name = Some("clientId"),
          unique = true,
          sparse = true)
  )

  def findById(_id: String): Future[Option[NioAccount]] = {
    findOneById("", _id)
  }
  def findByEmail(email: String): Future[Option[NioAccount]] = {
    findOneByQuery("", Json.obj("email" -> email))
  }

  def findByClientId(clientId: String): Future[Option[NioAccount]] = {
    findOneByQuery("", Json.obj("clientId" -> clientId))
  }

  def findMany(): Future[Seq[NioAccount]] =
    findMany("")

  def insertOne(objToInsert: NioAccount): Future[Boolean] =
    insertOne("", objToInsert)

  def updateOne(_id: String, objToInsert: NioAccount): Future[Boolean] =
    updateOne("", _id, objToInsert)

  def deleteOne(_id: String): Future[Boolean] =
    deleteOneById("", _id)

  def findManyPaginate(query: JsObject = Json.obj(),
                       sort: JsObject = Json.obj("_id" -> -1),
                       page: Int,
                       pageSize: Int): Future[(Seq[NioAccount], Int)] =
    findManyByQueryPaginateCount("", query, sort, page, pageSize)
}
