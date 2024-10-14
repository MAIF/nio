package db

import org.apache.pekko.http.scaladsl.util.FastFuture

import java.time.{Instant, LocalDateTime, ZoneId}
import utils.NioLogger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.collection.{Seq, immutable}

class CatchupLockMongoDatastore(val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext)
    extends DataStoreUtils {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import bson2json._
  import json2bson._

  override def collectionName(tenant: String): String = "catchupLock"

  override def indices: Seq[Index.Default] = Seq(
    Index(
      immutable.Seq("expireAt" -> IndexType.Ascending),
      name = Some("expire_at"),
      options = BSONDocument("expireAfterSeconds" -> 0)
    )
  )

  implicit def format: Format[CatchupLock] = CatchupLock.catchupLockFormats

  def init(): Future[Unit] = {
    NioLogger.debug("### init lock datastore ###")

    storedCollection.flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
        _ <- Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
      } yield ()
    }
  }

  def storedCollection: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection("catchupLock"))

  def createLock(tenant: String): Future[Boolean] =
    storedCollection
      .flatMap(
        _.find(Json.obj("tenant" -> tenant))
          .one[CatchupLock]
      )
      .flatMap {
        case None              =>
          storedCollection.flatMap { coll =>
            coll.insert.one(format.writes(CatchupLock(tenant)).as[JsObject]).map(_.writeErrors.isEmpty)
          }
        case Some(_) =>
          NioLogger.debug(s"Stored collection already locked for tenant $tenant")
          FastFuture.successful(false)
      }

  def findLock(tenant: String): Future[Option[CatchupLock]] =
    storedCollection
      .flatMap(
        _.find(Json.obj("tenant" -> tenant))
          .one[CatchupLock]
      )
}

case class CatchupLock(tenant: String, expireAt: LocalDateTime = LocalDateTime.now)

object CatchupLock {
  implicit val catchupLockFormats: Format[CatchupLock] =
    new Format[CatchupLock] {
      override def reads(json: JsValue): JsResult[CatchupLock] =
        (Try {
          JsSuccess(
            CatchupLock(
              tenant = (json \ "tenant").as[String],
              expireAt = (json \ "expireAt").as(jodaDateFormat)
            )
          )
        } recover { case e =>
          JsError(e.getMessage)
        }).get

      override def writes(c: CatchupLock): JsValue = Json.obj(
        "tenant"   -> c.tenant,
        "expireAt" -> jodaDateFormat.writes(c.expireAt)
      )
    }

  implicit val jodaDateFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    override def reads(d: JsValue): JsResult[LocalDateTime] =
      JsSuccess(Instant
        .ofEpochMilli(d.as[JsObject].\("$date").as[JsNumber].value.toLong)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime
      )

    override def writes(d: LocalDateTime): JsValue =
      JsObject(Seq("$date" -> JsNumber(d.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli)))
  }
}
