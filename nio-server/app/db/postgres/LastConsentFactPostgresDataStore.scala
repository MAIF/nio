package db.postgres

import akka.http.scaladsl.util.FastFuture
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import controllers.AppErrorWithStatus
import db.LastConsentFactDataStore
import models._
import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, Json, OFormat}
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scalikejdbc._
import async._

class LastConsentFactPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[ConsentFact]
    with LastConsentFactDataStore {

  val format: OFormat[ConsentFact] = models.ConsentFact.consentFactOFormats

  override val tableName = "last_consent_facts"

  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean] =
    insertOne(tenant, consentFact)

  def findById(tenant: String, id: String): Future[Option[ConsentFact]] = {
    findOneById(tenant, id)
  }

  def findOneByUserId(tenant: String,
                      userId: String): Future[Option[ConsentFact]] = {
    findOneByQuery(tenant, Json.obj("userId" -> userId))
  }

  def findByOrgKeyAndUserId(tenant: String,
                            orgKey: String,
                            userId: String): Future[Option[ConsentFact]] = {
    findOneByQuery(tenant, Json.obj("orgKey" -> orgKey, "userId" -> userId))
  }

  def findAllByOrgKey(
      tenant: String,
      orgKey: String,
      page: Int,
      pageSize: Int,
      query: Option[String]): Future[(Seq[ConsentFact], Int)] = {

    val _query = query match {
      case Some(str) => Json.parse(str).as[JsArray]
      case None      => Json.arr()
    }

    findManyByQueriesPaginateCount(tenant = tenant,
                                   rootQuery = Json.obj("orgKey" -> orgKey),
                                   query = _query,
                                   page = page,
                                   pageSize = pageSize)
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ConsentFact], Int)] = {

    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj("userId" -> userId),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAll(tenant: String): Future[Seq[ConsentFact]] =
    findMany(tenant)

  def deleteConsentFactByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))
  }

  def removeById(tenant: String, id: String): Future[Boolean] = {
    deleteOneById(tenant, id)
  }

  import play.api.mvc.Results._

  def removeOffer(
      tenant: String,
      orgKey: String,
      offerKey: String): Future[Either[AppErrorWithStatus, Boolean]] = {
    findManyByQuery(tenant,
                    Json.obj("orgKey" -> orgKey,
                             "offers" -> Json.arr(Json.obj("key" -> offerKey))))
      .flatMap {
        case Nil =>
          FastFuture.successful(Right(true))
        case consentFacts =>
          Future
            .sequence(
              consentFacts.map(c =>
                removeOfferById(tenant, orgKey, c.userId, offerKey))
            )
            .map(sequence)
            .map { e =>
              e.left.map(
                m =>
                  controllers.AppErrorWithStatus(
                    AppErrors(
                      m.flatMap(_.appErrors.errors)
                    ),
                    BadRequest
                ))
            }
            .map { e =>
              e.right.map(_ => true)
            }
      }
  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[Seq[A], B] =
    s.foldLeft(Left(Nil): Either[List[A], B]) { (acc, e) =>
      for (xs <- acc.left; x <- e.left) yield x :: xs
    }

  def findConsentOffer(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrors, Option[ConsentOffer]]] = {
    findByOrgKeyAndUserId(tenant, orgKey, userId).map {
      case Some(consentFact) =>
        Right(consentFact.offers.flatMap(offers =>
          offers.find(p => p.key == offerKey)))
      case None =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }
  }

  private def setOffers(tenant: String,
                        query: JsObject,
                        offers: Seq[ConsentOffer]): Future[Boolean] = {
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

  def removeOfferById(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrorWithStatus, ConsentOffer]] = {
    findByOrgKeyAndUserId(tenant, orgKey, userId).flatMap {
      case Some(consentFact) =>
        val maybeOffer = consentFact.offers.flatMap(offers =>
          offers.find(p => p.key == offerKey))

        maybeOffer match {
          case Some(offer) =>
            setOffers(tenant,
                      Json.obj("orgKey" -> orgKey, "userId" -> userId),
                      consentFact.offers.get.filter(p => p.key != offerKey))
              .map(_ => Right(offer))
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
              AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))),
              NotFound)))
    }
  }

  def update(tenant: String,
             orgKey: String,
             userId: String,
             consentFact: ConsentFact): Future[Boolean] = {
    updateOneByQuery(tenant,
                     Json.obj("orgKey" -> orgKey, "userId" -> userId),
                     consentFact)
  }

  def streamAllByteString(tenant: String)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed] = {
    streamAsJsValue(tenant)
      .grouped(300)
      .mapAsync(10) { d =>
        Future {
          val bld = new StringBuilder()
          d.foreach { doc =>
            bld.append(s"${doc.toString()}\n")
          }
          ByteString(bld.toString())
        }
      }
  }

  def streamAllByteString(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed] = {

    Source
      .fromFuture(count(tenant, Json.obj()))
      .flatMapConcat { count =>
        Logger.info(s"Will stream a total of $count consents")
        if (count < parallelisation)
          streamAsJsValue(tenant, pageSize, 0)
            .map(doc => ByteString(s"${doc.toString()}\n"))
            .alsoTo(Sink.onComplete {
              case Failure(e) =>
                Logger.error("Error while streaming consents", e)
            })
        else
          (0 until parallelisation)
            .map { idx =>
              val items = count / parallelisation
              val from = items * idx
              val to = from + items
              Logger.info(
                s"Consuming $items consents with worker $idx: $from => $to")
              streamAsJsValue(tenant, pageSize, from)
                .grouped(items)
            }
            .reduce(_.merge(_))
            .map(docs => ByteString(docs.map(Json.stringify).mkString("\n")))
            .alsoTo(Sink.onComplete {
              case Failure(e) =>
                Logger.error("Error while streaming consents", e)
            })
      }
  }
}
