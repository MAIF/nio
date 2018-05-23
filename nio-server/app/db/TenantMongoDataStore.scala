package db

import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TenantMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("tenants"))

  implicit def format: Format[Tenant] = Tenant.tenantFormats

  def insert(tenant: Tenant) =
    storedCollection.flatMap(
      _.insert(format.writes(tenant).as[JsObject]).map(_.ok))

  def findAll() =
    storedCollection.flatMap(
      _.find(Json.obj())
        .cursor[Tenant](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Tenant]]()))

  def findByKey(key: String): Future[Option[Tenant]] =
    storedCollection.flatMap(
      _.find(Json.obj("key" -> key))
        .one[Tenant]
    )

  def init() = {
    storedCollection.flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def removeByKey(key: String) = {
    storedCollection.flatMap { col =>
      col.remove(Json.obj("key" -> key))
    }
  }

}
