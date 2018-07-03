package db

import akka.stream.Materializer
import models._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, ReadPreference}

import scala.concurrent.{ExecutionContext, Future}

class OrganisationMongoDataStore(val reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext)
    extends AbstractMongoDataStore[Organisation](reactiveMongoApi) {

  override def collectionName(tenant: String) = s"$tenant-organisations"

  override def indices = Seq(
    Index(Seq("orgKey" -> IndexType.Ascending),
          name = Some("orgKey"),
          unique = false,
          sparse = true),
    Index(Seq("orgKey" -> IndexType.Ascending,
              "version.num" -> IndexType.Ascending),
          name = Some("orgKey_versionNum"),
          unique = false,
          sparse = true)
  )

  def insert(tenant: String, organisation: Organisation): Future[Boolean] =
    insertOne(tenant, organisation)

  def findById(tenant: String, id: String): Future[Option[Organisation]] = {
    findOneById(tenant, id)
  }

  def findByKey(tenant: String, key: String): Future[Option[Organisation]] = {
    findOneByQuery(tenant, Json.obj("key" -> key))
  }

  def findAllReleasedByKey(tenant: String,
                           key: String): Future[Seq[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED")))
    findManyByQuery(tenant, query)
  }

  def findLastReleasedByKey(tenant: String,
                            key: String): Future[Option[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED"),
                         Json.obj("version.latest" -> true)))
    findOneByQuery(tenant, query)
  }

  def findDraftByKey(tenant: String,
                     key: String): Future[Option[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "DRAFT")))
    findOneByQuery(tenant, query)
  }

  def findReleasedByKeyAndVersionNum(
      tenant: String,
      key: String,
      versionNum: Int): Future[Option[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED"),
                         Json.obj("version.num" -> versionNum)))
    findOneByQuery(tenant, query)
  }

  def updateById(tenant: String,
                 id: String,
                 value: Organisation): Future[Boolean] = {
    updateOne(tenant, id, value)
  }

  def findAllLatestReleasesOrDrafts(
      tenant: String): Future[Seq[Organisation]] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "RELEASED"),
                                    Json.obj("version.latest" -> true))),
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "DRAFT"),
                                    Json.obj("version.neverReleased" -> true)))
      ))

    findManyByQuery(tenant, query)
  }

  def findAll(tenant: String): Future[Seq[Organisation]] =
    findMany(tenant)

  def deleteOrganisationByTenant(tenant: String): Future[Boolean] = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("key" -> orgKey))
  }

  def findAllLatestReleasesOrDraftsByDate(
      tenant: String,
      from: String,
      to: String): Future[Seq[Organisation]] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj(
          "$and" -> Json.arr(Json.obj("version.status" -> "RELEASED"),
                             Json.obj("version.latest" -> true),
                             Json.obj("version.lastUpdate" -> Json
                               .obj("$gte" -> from, "$lte" -> to)))),
        Json.obj(
          "$and" -> Json.arr(Json.obj("version.status" -> "DRAFT"),
                             Json.obj("version.neverReleased" -> true),
                             Json.obj("version.lastUpdate" -> Json
                               .obj("$gte" -> from, "$lte" -> to))))
      ))

    findManyByQuery(tenant, query)
  }

  def streamAllLatestReleasesOrDraftsByDate(
      tenant: String,
      from: String,
      to: String)(implicit m: Materializer) = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj(
          "$and" -> Json.arr(Json.obj("version.status" -> "RELEASED"),
                             Json.obj("version.latest" -> true),
                             Json.obj("version.lastUpdate" -> Json
                               .obj("$gte" -> from, "$lte" -> to)))),
        Json.obj(
          "$and" -> Json.arr(Json.obj("version.status" -> "DRAFT"),
                             Json.obj("version.neverReleased" -> true),
                             Json.obj("version.lastUpdate" -> Json
                               .obj("$gte" -> from, "$lte" -> to))))
      ))
    storedCollection(tenant).map { col =>
      col
        .find(query,
              Json.obj(
                "_id" -> 0
              ))
        .cursor[JsValue]()
        .documentSource()
    }
  }
}
