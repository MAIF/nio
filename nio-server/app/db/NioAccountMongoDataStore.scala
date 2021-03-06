package db

import models.NioAccount
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.{immutable, Seq}

class NioAccountMongoDataStore(val mongoApi: ReactiveMongoApi)(implicit val executionContext: ExecutionContext)
    extends MongoDataStore[NioAccount] {

  val format: OFormat[NioAccount] = models.NioAccount.oformats

  override protected def collectionName(tenant: String = ""): String =
    s"NioAccount"

  override protected def indices: Seq[Index.Default] = Seq(
    Index(immutable.Seq("email" -> IndexType.Ascending), name = Some("email"), unique = true, sparse = true),
    Index(immutable.Seq("clientId" -> IndexType.Ascending), name = Some("clientId"), unique = true, sparse = true)
  )

  def findById(_id: String): Future[Option[NioAccount]]      =
    findOneById("", _id)
  def findByEmail(email: String): Future[Option[NioAccount]] =
    findOneByQuery("", Json.obj("email" -> email))
  def findMany(): Future[Seq[NioAccount]]                    =
    findMany("")

  def insertOne(objToInsert: NioAccount): Future[Boolean] =
    insertOne("", objToInsert)

  def updateOne(_id: String, objToInsert: NioAccount): Future[Boolean] =
    updateOne("", _id, objToInsert)

  def deleteOne(_id: String): Future[Boolean] =
    deleteOneById("", _id)

  def findManyPaginate(
      query: JsObject = Json.obj(),
      sort: JsObject = Json.obj("email" -> 1),
      page: Int,
      pageSize: Int
  ): Future[(Seq[NioAccount], Long)] =
    findManyByQueryPaginateCount("", query, sort, page, pageSize)
}
