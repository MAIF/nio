package db

import models._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts}

import scala.concurrent.{ExecutionContext, Future}

class AccountMongoDataStore(val reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext)
    extends DataStoreUtils {

  override def collectionName(tenant: String) = s"$tenant-accounts"

  override def indices = Seq(
    Index(Seq("accountId" -> IndexType.Ascending),
          name = Some("accountId"),
          unique = false,
          sparse = true)
  )

  def findByAccountId(tenant: String,
                      accountId: String): Future[Option[Account]] = {
    storedCollection(tenant).flatMap(
      _.find(Json.obj("accountId" -> accountId))
        .one[Account]
    )
  }

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[Seq[Account]] = {
    storedCollection(tenant).flatMap(
      _.find(Json.obj())
        .options(QueryOpts(skipN = page * pageSize, pageSize))
        .cursor[Account]()
        .collect[Seq](pageSize, Cursor.FailOnError[Seq[Account]]())
    )
  }

  def update(tenant: String,
             accountId: String,
             account: Account): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("accountId" -> accountId), account)
        .map(_.ok)
    )
  }

  def create(tenant: String, account: Account): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.insert(account)
        .map(_.ok)
    )
  }

  def delete(tenant: String, accountId: String): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.remove(Json.obj("accountId" -> accountId))
        .map(_.ok)
    )
  }
}
