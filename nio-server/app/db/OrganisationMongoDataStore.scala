package db

import org.apache.pekko.http.scaladsl.util.FastFuture
import org.apache.pekko.stream.Materializer
import controllers.AppErrorWithStatus
import models._
import org.apache.pekko.stream.scaladsl.Source
import utils.NioLogger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.Index.Default
import reactivemongo.api.indexes.{Index, IndexType}
import utils.Result.{AppErrors, ErrorMessage}

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.{Seq, immutable}

@nowarn("msg=Will be removed when provided by Play-JSON itself")
class OrganisationMongoDataStore(val mongoApi: ReactiveMongoApi)(implicit val executionContext: ExecutionContext)
    extends MongoDataStore[Organisation] {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import reactivemongo.pekkostream._
  import bson2json._
  import json2bson._

  val format: OFormat[Organisation] = models.Organisation.oFormats

  override def collectionName(tenant: String) = s"$tenant-organisations"

  override def indices: Seq[Default] = Seq(
    Index(immutable.Seq("orgKey" -> IndexType.Ascending), name = Some("orgKey"), unique = false, sparse = true),
    Index(
      immutable.Seq("orgKey" -> IndexType.Ascending, "version.num" -> IndexType.Ascending),
      name = Some("orgKey_versionNum"),
      unique = false,
      sparse = true
    )
  )

  def insert(tenant: String, organisation: Organisation): Future[Boolean] =
    insertOne(tenant, organisation)

  def findById(tenant: String, id: String): Future[Option[Organisation]] =
    findOneById(tenant, id)

  def findByKey(tenant: String, key: String): Future[Option[Organisation]] =
    findOneByQuery(tenant, Json.obj("key" -> key))

  def findAllReleasedByKey(tenant: String, key: String): Future[Seq[Organisation]] = {
    val query = Json.obj("$and" -> Json.arr(Json.obj("key" -> key), Json.obj("version.status" -> "RELEASED")))
    findManyByQuery(tenant, query)
  }

  def findLastReleasedByKey(tenant: String, key: String): Future[Option[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(
        Json.obj("key"            -> key),
        Json.obj("version.status" -> "RELEASED"),
        Json.obj("version.latest" -> true)
      )
    )
    findOneByQuery(tenant, query)
  }

  def findDraftByKey(tenant: String, key: String): Future[Option[Organisation]] = {
    val query = Json.obj("$and" -> Json.arr(Json.obj("key" -> key), Json.obj("version.status" -> "DRAFT")))
    findOneByQuery(tenant, query)
  }

  def findReleasedByKeyAndVersionNum(tenant: String, key: String, versionNum: Int): Future[Option[Organisation]] = {
    val query = Json.obj(
      "$and" -> Json.arr(
        Json.obj("key"            -> key),
        Json.obj("version.status" -> "RELEASED"),
        Json.obj("version.num"    -> versionNum)
      )
    )
    findOneByQuery(tenant, query)
  }

  def updateById(tenant: String, id: String, value: Organisation): Future[Boolean] =
    updateOne(tenant, id, value)

  def findAllLatestReleasesOrDrafts(tenant: String): Future[Seq[Organisation]] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "RELEASED"), Json.obj("version.latest" -> true))),
        Json.obj("$and" -> Json.arr(Json.obj("version.status" -> "DRAFT"), Json.obj("version.neverReleased" -> true)))
      )
    )

    findManyByQuery(tenant, query)
  }

  def findAll(tenant: String): Future[Seq[Organisation]] =
    findMany(tenant)

  def deleteOrganisationByTenant(tenant: String): Future[Boolean] =
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }

  def removeByKey(tenant: String, orgKey: String): Future[Boolean] =
    deleteByQuery(tenant, Json.obj("key" -> orgKey))

  def findAllLatestReleasesOrDraftsByDate(tenant: String, from: String, to: String): Future[Seq[Organisation]] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj(
          "$and" -> Json.arr(
            Json.obj("version.status" -> "RELEASED"),
            Json.obj("version.latest" -> true),
            Json.obj(
              "version.lastUpdate"    -> Json
                .obj("$gte" -> from, "$lte" -> to)
            )
          )
        ),
        Json.obj(
          "$and" -> Json.arr(
            Json.obj("version.status"        -> "DRAFT"),
            Json.obj("version.neverReleased" -> true),
            Json.obj(
              "version.lastUpdate"           -> Json
                .obj("$gte" -> from, "$lte" -> to)
            )
          )
        )
      )
    )

    findManyByQuery(tenant, query)
  }

  def streamAllLatestReleasesOrDraftsByDate(tenant: String, from: String, to: String)(implicit m: Materializer): Future[Source[JsValue, Future[State]]] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj(
          "$and" -> Json.arr(
            Json.obj("version.status" -> "RELEASED"),
            Json.obj("version.latest" -> true),
            Json.obj(
              "version.lastUpdate"    -> Json
                .obj("$gte" -> from, "$lte" -> to)
            )
          )
        ),
        Json.obj(
          "$and" -> Json.arr(
            Json.obj("version.status"        -> "DRAFT"),
            Json.obj("version.neverReleased" -> true),
            Json.obj(
              "version.lastUpdate"           -> Json
                .obj("$gte" -> from, "$lte" -> to)
            )
          )
        )
      )
    )
    storedCollection(tenant).map { col =>
      col
        .find(query, Some(Json.obj("_id" -> 0)))
        .cursor[JsValue]()
        .documentSource()
    }
  }

  // OFFERS
  import play.api.mvc.Results._

  def findOffers(tenant: String, orgKey: String): Future[Either[AppErrors, Option[Seq[Offer]]]] =
    findLastReleasedByKey(tenant, orgKey).map {
      case Some(organisation) => Right(organisation.offers)
      case None               =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }

  def findOffer(tenant: String, orgKey: String, offerKey: String): Future[Either[AppErrors, Option[Offer]]] =
    findLastReleasedByKey(tenant, orgKey).map {
      case Some(organisation) =>
        Right(organisation.offers.flatMap(offers => offers.find(p => p.key == offerKey)))
      case None               =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }

  def updateOffer(tenant: String, orgKey: String, offerKey: String, offer: Offer): Future[Offer] =
    storedCollection(tenant).flatMap { coll =>
      val builder = coll.update(ordered = false)
      val updates = builder.element(
        q = Json.obj("key" -> orgKey, "offers.key" -> offerKey, "version.latest" -> true),
        u = Json.obj(
          "$set" -> Json.obj(
            "offers.$" -> offer.asJson()
          )
        ),
        upsert = false,
        multi = true
      )
      updates.flatMap(u => builder.many(List(u))).map(_ => offer)
    }

  def addOffer(tenant: String, orgKey: String, offer: Offer): Future[Offer] =
    storedCollection(tenant).flatMap { coll =>
      val builder = coll.update(ordered = false)
      val updates = builder.element(
        q = Json.obj("key" -> orgKey, "version.latest" -> true),
        u = Json.obj(
          "$push" -> Json.obj(
            "offers" -> offer.asJson()
          )
        ),
        upsert = false,
        multi = true
      )
      updates.flatMap(u => builder.many(List(u))).map { result =>
        NioLogger.info(s"$result")
        offer
      }
    }

  def deleteOffer(tenant: String, orgKey: String, offerKey: String): Future[Either[AppErrorWithStatus, Offer]] =
    findOffer(tenant, orgKey, offerKey).flatMap {
      case Right(maybeOffer) =>
        maybeOffer match {
          case Some(offer) =>
            storedCollection(tenant).flatMap { coll =>
              val builder = coll.update(ordered = false)
              val updates = builder.element(
                q = Json.obj("key" -> orgKey, "offers.key" -> offerKey, "version.latest" -> true),
                u = Json.obj(
                  "$pull" -> Json.obj(
                    "offers" -> offer.asJson()
                  )
                ),
                upsert = false,
                multi = true
              )
              updates.flatMap(u => builder.many(List(u))).map(_ => Right(offer))
            }

          case None =>
            FastFuture.successful(
              Left(AppErrorWithStatus(AppErrors(Seq(ErrorMessage(s"offer.$offerKey.not.found"))), NotFound))
            )
        }
      case Left(e)           =>
        FastFuture.successful(Left(AppErrorWithStatus(e, NotFound)))
    }

}
