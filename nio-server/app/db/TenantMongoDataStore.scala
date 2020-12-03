package db

import models._
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.Index
import reactivemongo.api.{Cursor, ReadPreference}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.Seq

class TenantMongoDataStore(val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext)
    extends DataStoreUtils {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import reactivemongo.akkastream._
  import lax._
  import bson2json._
  import json2bson._

  override def collectionName(tenant: String) = "tenants"

  override def indices = Seq.empty[Index.Default]

  implicit def format: Format[Tenant] = Tenant.tenantFormats

  def init() =
    storedCollection.flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }

  def storedCollection: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection("tenants"))

  def insert(tenant: Tenant) =
    storedCollection.flatMap(_.insert.one(format.writes(tenant).as[JsObject]).map(_.writeErrors.isEmpty))

  def findAll() =
    storedCollection.flatMap(
      _.find(Json.obj())
        .cursor[Tenant](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Tenant]]())
    )

  def findByKey(key: String): Future[Option[Tenant]] =
    storedCollection.flatMap(
      _.find(Json.obj("key" -> key))
        .one[Tenant]
    )

  def removeByKey(key: String) =
    storedCollection.flatMap { col =>
      col.delete.one(Json.obj("key" -> key))
    }

}
