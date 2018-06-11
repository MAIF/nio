package db

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-organisations"))

  implicit def format: Format[Organisation] = Organisation.formats

  def insert(tenant: String, organisation: Organisation) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(organisation).as[JsObject]).map(_.ok))

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
    storedCollection(tenant).flatMap(_.find(query).one[Organisation])
  }

  def findByKey(tenant: String, key: String) = {
    val query = Json.obj("key" -> key)
    storedCollection(tenant).flatMap(_.find(query).one[Organisation])
  }

  def findAllReleasedByKey(tenant: String, key: String) = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED")))
    storedCollection(tenant).flatMap(
      _.find(query)
        .cursor[Organisation](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Organisation]]()))
  }

  def findLastReleasedByKey(tenant: String, key: String) = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED"),
                         Json.obj("version.latest" -> true)))
    storedCollection(tenant).flatMap(_.find(query).one[Organisation])
  }

  def findDraftByKey(tenant: String, key: String) = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "DRAFT")))
    storedCollection(tenant).flatMap(_.find(query).one[Organisation])
  }

  def findReleasedByKeyAndVersionNum(tenant: String,
                                     key: String,
                                     versionNum: Int) = {
    val query = Json.obj(
      "$and" -> Json.arr(Json.obj("key" -> key),
                         Json.obj("version.status" -> "RELEASED"),
                         Json.obj("version.num" -> versionNum)))
    storedCollection(tenant).flatMap(_.find(query).one[Organisation])
  }

  def updateById(tenant: String,
                 id: String,
                 value: Organisation): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("_id" -> id), format.writes(value).as[JsObject])
        .map(_.ok))
  }

  def findAllLatestReleasesOrDrafts(tenant: String) = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "RELEASED"),
                                    Json.obj("version.latest" -> true))),
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "DRAFT"),
                                    Json.obj("version.neverReleased" -> true)))
      ))
    storedCollection(tenant).flatMap(
      _.find(query)
        .cursor[Organisation](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Organisation]]()))
  }

  def findAll(tenant: String) =
    storedCollection(tenant).flatMap(
      _.find(Json.obj())
        .cursor[Organisation](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Organisation]]()))

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def deleteOrganisationByTenant(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByKey(tenant: String, orgKey: String) = {
    storedCollection(tenant).flatMap { col =>
      col.remove(Json.obj("key" -> orgKey))
    }
  }

  def findAllLatestReleasesOrDraftsByDate(tenant: String,
                                          from: String,
                                          to: String) = {
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
    storedCollection(tenant).flatMap(
      _.find(query)
        .cursor[Organisation](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Organisation]]()))
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

  def ensureIndices(tenant: String) = {
    reactiveMongoApi.database
      .map(_.collectionNames)
      .flatMap(collectionNames => {
        collectionNames.flatMap(
          cols =>
            cols.find(c => c == s"$tenant-organisations") match {
              case Some(_) =>
                storedCollection(tenant).flatMap {
                  col =>
                    Future.sequence(
                      Seq(
                        col.indexesManager.ensure(
                          Index(Seq("orgKey" -> IndexType.Ascending),
                                name = Some("orgKey"),
                                unique = false,
                                sparse = true)
                        ),
                        col.indexesManager.ensure(
                          Index(Seq("orgKey" -> IndexType.Ascending,
                                    "version.num" -> IndexType.Ascending),
                                name = Some("orgKey_versionNum"),
                                unique = false,
                                sparse = true)
                        )
                      )
                    )
                }
              case None =>
                Logger.error(s"unknow collection $tenant-organisations")
                Future {
                  Seq()
                }
          }
        )
      })

  }

}
