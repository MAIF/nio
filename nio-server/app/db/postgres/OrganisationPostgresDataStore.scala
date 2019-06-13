package db.postgres

import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import controllers.AppErrorWithStatus
import db.OrganisationDataStore
import models._
import play.api.libs.json._
import scalikejdbc.async.AsyncDB
import utils.Result.{AppErrors, ErrorMessage}
import scalikejdbc._
import async._
import scalikejdbc.streams.DatabasePublisher
import scalikejdbc.streams._

import scala.concurrent.{ExecutionContext, Future}

class OrganisationPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[Organisation]
    with OrganisationDataStore {

  val format: OFormat[Organisation] = models.Organisation.oFormats

  override val tableName = "organisations"

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

    val query =
      Json.obj("key" -> key, "version" -> Json.obj("status" -> "RELEASED"))

    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb order by payload->>'_id' asc"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def findLastReleasedByKey(tenant: String,
                            key: String): Future[Option[Organisation]] = {
    val query = Json.obj(
      "key" -> key,
      "version" -> Json.obj("status" -> "RELEASED", "latest" -> true))

    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
        .map(_.headOption)
    }
  }

  def findDraftByKey(tenant: String,
                     key: String): Future[Option[Organisation]] = {

    val query =
      Json.obj("key" -> key, "version" -> Json.obj("status" -> "DRAFT"))

    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query
        .toString()}::jsonb"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
        .map(_.headOption)
    }
  }

  def findReleasedByKeyAndVersionNum(
      tenant: String,
      key: String,
      versionNum: Int): Future[Option[Organisation]] = {
    val query = Json.obj(
      "key" -> key,
      "version" -> Json.obj("num" -> versionNum, "status" -> "RELEASED"))

    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query
        .toString()}::jsonb"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
        .map(_.headOption)
    }
  }

  def updateById(tenant: String,
                 id: String,
                 value: Organisation): Future[Boolean] = {
    updateOne(tenant, id, value)
  }

  def findAllLatestReleasesOrDrafts(
      tenant: String): Future[Seq[Organisation]] = {

    val query1 =
      Json.obj("version" -> Json.obj("latest" -> true, "status" -> "RELEASED"))
    val query2 = Json.obj(
      "version" -> Json.obj("neverReleased" -> true, "status" -> "DRAFT"))

    AsyncDB withPool { implicit session =>
      sql"""select * from ${table} where tenant=${tenant}
            and (payload @> ${query1.toString()}::jsonb
            or payload @> ${query2
        .toString()}::jsonb) order by payload->>'_id' asc"""
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def findAll(tenant: String): Future[Seq[Organisation]] =
    findMany(tenant)

  def deleteOrganisationByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }

  def removeByKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("key" -> orgKey))
  }

  def findAllLatestReleasesOrDraftsByDate(
      tenant: String,
      from: String,
      to: String): Future[Seq[Organisation]] = {

    val query1 =
      Json.obj("version" -> Json.obj("latest" -> true, "status" -> "RELEASED"))
    val query2 = Json.obj(
      "version" -> Json.obj("neverReleased" -> true, "status" -> "DRAFT"))

    AsyncDB withPool { implicit session =>
      sql"""select * from ${table} where tenant=${tenant}
            and ((payload @> ${query1
        .toString()}::jsonb and payload#>>'{version,lastUpdate}' >= ${from} and payload#>>'{version,lastUpdate}' <= ${to} )
            or (payload @> ${query2
        .toString()}::jsonb and payload#>>'{version,lastUpdate}' >= ${from} and payload#>>'{version,lastUpdate}' <= ${to} ))
         order by payload->>'_id' asc"""
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }

  }

  def streamAllLatestReleasesOrDraftsByDate(
      tenant: String,
      from: String,
      to: String)(implicit m: Materializer) = {

    val query1 =
      Json.obj("version" -> Json.obj("latest" -> true, "status" -> "RELEASED"))
    val query2 = Json.obj(
      "version" -> Json.obj("neverReleased" -> true, "status" -> "DRAFT"))

    val publisher: DatabasePublisher[JsValue] = DB readOnlyStream {
      sql"""select * from ${table} where tenant=${tenant}
            and ((payload @> ${query1
        .toString()}::jsonb and payload#>>'{version,lastUpdate}' >= ${from} and payload#>>'{version,lastUpdate}' <= ${to} )
            or (payload @> ${query2
        .toString()}::jsonb and payload#>>'{version,lastUpdate}' >= ${from} and payload#>>'{version,lastUpdate}' <= ${to} )) """
        .map(rs => Json.toJson(fromResultSet(rs)))
        .iterator
    }
    FastFuture.successful(
      Source.fromFutureSource(
        FastFuture.successful(
          Source
            .fromPublisher(publisher)
            .map(_.as[JsObject] - "_id")
        )
      )
    )
  }

  // OFFERS
  import play.api.mvc.Results._

  def findOffers(
      tenant: String,
      orgKey: String): Future[Either[AppErrors, Option[Seq[Offer]]]] = {
    findLastReleasedByKey(tenant, orgKey).map {
      case Some(organisation) => Right(organisation.offers)
      case None =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }
  }

  def findOffer(tenant: String,
                orgKey: String,
                offerKey: String): Future[Either[AppErrors, Option[Offer]]] = {
    findLastReleasedByKey(tenant, orgKey).map {
      case Some(organisation) =>
        Right(organisation.offers.flatMap(offers =>
          offers.find(p => p.key == offerKey)))
      case None =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }
  }

  private def setOffers(tenant: String,
                        query: JsObject,
                        offers: Seq[Offer]): Future[Boolean] = {
    for {
      newJson <- AsyncDB withPool { implicit session =>
        sql"""select jsonb_set(payload, '{offers}', ${Json
          .toJson(offers)
          .toString()}, true) as js
             from ${table} where tenant = ${tenant} and  payload @> ${query
          .toString()}::jsonb"""
          .map(rs => rs.get[String]("js"))
          .single()
          .future()
      }
      res <- AsyncDB withPool { implicit session =>
        sql"update ${table} set payload = ${newJson} where tenant = ${tenant} and  payload @> ${query.toString()}::jsonb"
          .update()
          .future()
          .map(_ > 0)
      }
    } yield res
  }

  def updateOffer(tenant: String,
                  orgKey: String,
                  offerKey: String,
                  offer: Offer): Future[Offer] = {
    val query = Json.obj("key" -> orgKey,
                         "offers" -> Json.arr(Json.obj("key" -> offerKey)),
                         "version" -> Json.obj("latest" -> true))

    findOneByQuery(tenant, query)
      .flatMap {
        case Some(organisation) =>
          val maybeOffer = organisation.offers.flatMap(offers =>
            offers.find(p => p.key == offerKey))
          maybeOffer match {
            case Some(_) =>
              setOffers(
                tenant,
                query,
                organisation.offers.get.map(p => {
                  if (p.key == offerKey) offer else p
                })
              ).map(_ => offer)
            case None =>
              Future.failed(new RuntimeException("offer not found"))
          }
        case None =>
          Future.failed(new RuntimeException("org not found"))
      }
  }

  def addOffer(tenant: String, orgKey: String, offer: Offer): Future[Offer] = {
    val query =
      Json.obj("key" -> orgKey, "version" -> Json.obj("latest" -> true))
    findOneByQuery(tenant, query)
      .flatMap {
        case Some(organisation) =>
          setOffers(
            tenant,
            query,
            organisation.offers.getOrElse(Seq.empty).:+(offer)
          ).map(_ => offer)
        case None =>
          Future.failed(new RuntimeException("org not found"))
      }
  }

  def deleteOffer(
      tenant: String,
      orgKey: String,
      offerKey: String): Future[Either[AppErrorWithStatus, Offer]] = {

    val query = Json.obj("key" -> orgKey,
                         "offers" -> Json.arr(Json.obj("key" -> offerKey)),
                         "version" -> Json.obj("latest" -> true))

    findOneByQuery(tenant, query).flatMap {
      case Some(organisation) =>
        val maybeOffer = organisation.offers.flatMap(offers =>
          offers.find(p => p.key == offerKey))
        maybeOffer match {
          case Some(offer) =>
            setOffers(
              tenant,
              query,
              organisation.offers.get.filter(p => p.key != offerKey)
            ).map(_ => Right(offer))
          case None =>
            FastFuture.successful(
              Left(
                AppErrorWithStatus(
                  AppErrors(Seq(ErrorMessage(s"offer.$offerKey.not.found"))),
                  NotFound)))
        }
      case None =>
        FastFuture.successful(
          Left(
            AppErrorWithStatus(
              AppErrors(
                Seq(ErrorMessage(s"organisation.$orgKey.not.found"))
              ),
              NotFound)
          )
        )
    }
  }
}
