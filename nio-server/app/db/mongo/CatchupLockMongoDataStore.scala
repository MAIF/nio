package db.mongo

import akka.http.scaladsl.util.FastFuture
import db.CatchupLockDataStore
import models.CatchupLock
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class CatchupLockMongoDataStore(val reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext)
    extends MongoDataStoreUtils
    with CatchupLockDataStore {

  override def collectionName(tenant: String): String = "catchupLock"

  override def indices: Seq[Index] = Seq(
    Index(
      Seq("expireAt" -> IndexType.Ascending),
      name = Some("expire_at"),
      options = BSONDocument("expireAfterSeconds" -> 0)
    )
  )

  implicit def format: Format[CatchupLock] = CatchupLock.catchupLockFormats

  def init() = {
    Logger.debug("### init lock datastore ###")

    storedCollection.flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
        _ <- Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
      } yield ()
    }
  }

  def storedCollection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("catchupLock"))

  def createLock(tenant: String) = {
    storedCollection
      .flatMap(
        _.find(Json.obj("tenant" -> tenant))
          .one[CatchupLock]
      )
      .flatMap {
        case None =>
          storedCollection.flatMap(
            _.insert(format.writes(CatchupLock(tenant)).as[JsObject]).map(_.ok))
        case Some(catchupItem) =>
          Logger.debug(s"Stored collection already locked for tenant $tenant")
          FastFuture.successful(false)
      }
  }

  def findLock(tenant: String) = {
    storedCollection
      .flatMap(
        _.find(Json.obj("tenant" -> tenant))
          .one[CatchupLock]
      )
  }
}
