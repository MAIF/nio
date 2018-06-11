package db

import javax.inject.{Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import scala.concurrent.{ExecutionContext, Future}
import models._
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts}

@Singleton
class AccountMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  protected def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-accounts"))

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

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def ensureIndices(tenant: String) = {
    reactiveMongoApi.database
      .map(_.collectionNames)
      .flatMap(collectionNames => {
        collectionNames.flatMap(
          cols =>
            cols.find(c => c == s"$tenant-accounts") match {
              case Some(_) =>
                storedCollection(tenant).flatMap { col =>
                  Future.sequence(
                    Seq(
                      col.indexesManager.ensure(
                        Index(Seq("accountId" -> IndexType.Ascending),
                              name = Some("accountId"),
                              unique = false,
                              sparse = true)
                      )
                    )
                  )
                }
              case None =>
                Logger.error(s"unknow collection $tenant-accounts")
                Future {
                  Seq()
                }
          }
        )
      })

  }
}
