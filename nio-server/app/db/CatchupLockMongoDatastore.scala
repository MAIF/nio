package db

import akka.http.scaladsl.util.FastFuture
import org.joda.time.DateTime
import utils.NioLogger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.collection.{immutable, Seq}

class CatchupLockMongoDatastore(val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext)
    extends DataStoreUtils {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import models.ExtractionTaskStatus._
  import lax._
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

  def init() = {
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

  def createLock(tenant: String) =
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
        case Some(catchupItem) =>
          NioLogger.debug(s"Stored collection already locked for tenant $tenant")
          FastFuture.successful(false)
      }

  def findLock(tenant: String) =
    storedCollection
      .flatMap(
        _.find(Json.obj("tenant" -> tenant))
          .one[CatchupLock]
      )
}

case class CatchupLock(tenant: String, expireAt: DateTime = DateTime.now)

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

  implicit val jodaDateFormat = new Format[org.joda.time.DateTime] {
    override def reads(d: JsValue): JsResult[DateTime] =
      JsSuccess(new DateTime(d.as[JsObject].\("$date").as[JsNumber].value.toLong))

    override def writes(d: DateTime): JsValue =
      JsObject(Seq("$date" -> JsNumber(d.getMillis)))
  }
}
