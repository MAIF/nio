package db

import models._
import play.api.libs.json.{Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection._

class AccountMongoDataStore(val mongoApi: ReactiveMongoApi)(implicit val executionContext: ExecutionContext)
    extends MongoDataStore[Account] {

  val format: OFormat[Account] = models.Account.oformat

  override def collectionName(tenant: String) = s"$tenant-accounts"

  override def indices = Seq(
    Index(
      key = immutable.Seq("accountId" -> IndexType.Ascending),
      name = Some("accountId"),
      unique = false,
      sparse = true
    )
  )

  def findByAccountId(tenant: String, accountId: String): Future[Option[Account]] =
    findOneByQuery(tenant, Json.obj("accountId" -> accountId))

  def findAll(tenant: String, page: Int, pageSize: Int): Future[Seq[Account]]     =
    findManyByQueryPaginate(tenant = tenant, query = Json.obj(), sort = Json.obj("accountId" -> 1), page = page, pageSize = pageSize)

  def update(tenant: String, accountId: String, account: Account): Future[Boolean] =
    updateOneByQuery(tenant, Json.obj("accountId" -> accountId), account)

  def create(tenant: String, account: Account): Future[Boolean]                    =
    insertOne(tenant, account)

  def delete(tenant: String, accountId: String): Future[Boolean] =
    deleteByQuery(tenant, Json.obj("accountId" -> accountId))

  def deleteAccountByTenant(tenant: String): Future[Boolean]     =
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
}
