package db.mongo

import akka.http.scaladsl.util.FastFuture
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import controllers.AppErrorWithStatus
import db.LastConsentFactDataStore
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.bson.BSONDocument
import utils.BSONUtils
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class LastConsentFactMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ConsentFact]
    with LastConsentFactDataStore {

  val format: OFormat[ConsentFact] = models.ConsentFact.consentFactOFormats

  override def collectionName(tenant: String) = s"$tenant-lastConsentFacts"

  override def indices = Seq(
    Index(Seq("orgKey" -> IndexType.Ascending, "userId" -> IndexType.Ascending),
          name = Some("orgKey_userId"),
          unique = true,
          sparse = true),
    Index(Seq("orgKey" -> IndexType.Ascending),
          name = Some("orgKey"),
          unique = false,
          sparse = true),
    Index(Seq("userId" -> IndexType.Ascending),
          name = Some("userId"),
          unique = false,
          sparse = true)
  )

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
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
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
                    Json.obj("orgKey" -> orgKey, "offers.key" -> offerKey))
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

  def removeOfferById(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrorWithStatus, ConsentOffer]] = {
    findConsentOffer(tenant, orgKey, userId, offerKey).flatMap {
      case Right(maybeOffer) =>
        maybeOffer match {
          case Some(offer) =>
            storedCollection(tenant).flatMap {
              _.update(
                Json.obj("orgKey" -> orgKey,
                         "userId" -> userId,
                         "offers.key" -> offerKey),
                Json.obj(
                  "$pull" -> Json.obj(
                    "offers" -> offer.asJson()
                  ))
              ).map(_ => Right(offer))
            }

          case None =>
            FastFuture.successful(
              Left(
                AppErrorWithStatus(
                  AppErrors(Seq(ErrorMessage(s"offer.$offerKey.not.found"))),
                  NotFound)))
        }
      case Left(e) =>
        FastFuture.successful(Left(AppErrorWithStatus(e, NotFound)))
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
    Source
      .fromFuture(storedBSONCollection(tenant))
      .flatMapConcat { col =>
        col
          .find(reactivemongo.bson.BSONDocument())
          .options(QueryOpts(batchSizeN = 300))
          .cursor[BSONDocument]()
          .bulkSource(err = Cursor.FailOnError((_, e) =>
            Logger.error(s"Error while streaming worker", e)))
      }
      .mapAsync(10) { d =>
        Future {
          val bld = new StringBuilder()
          d.foreach { doc =>
            bld.append(s"${BSONUtils.stringify(doc)}\n")
          }
          ByteString(bld.toString())
        }
      }
  }

  private def storedBSONCollection(tenant: String): Future[BSONCollection] =
    mongoApi.database.map(_.collection[BSONCollection](collectionName(tenant)))

  def streamAllByteString(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed] = {

    Source
      .fromFuture(storedBSONCollection(tenant))
      .mapAsync(1)(coll => coll.count().map(c => (coll, c)))
      .flatMapConcat {
        case (collection, count) =>
          Logger.info(s"Will stream a total of $count consents")
          if (count < parallelisation)
            collection
              .find(BSONDocument())
              .options(QueryOpts(skipN = 0, batchSizeN = pageSize, flagsN = 0))
              .cursor[BSONDocument](ReadPreference.primary)
              .bulkSource(maxDocs = count,
                          err = Cursor.FailOnError((_, e) =>
                            Logger.error(s"Error while streaming worker 0", e)))
              .map(docs =>
                ByteString(docs.map(BSONUtils.stringify).mkString("\n")))
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
                val options =
                  QueryOpts(skipN = from, batchSizeN = pageSize, flagsN = 0)
                collection
                  .find(BSONDocument())
                  .options(options)
                  .cursor[BSONDocument](ReadPreference.primary)
                  .bulkSource(
                    maxDocs = items,
                    err = Cursor.FailOnError((_, e) =>
                      Logger.error(s"Error while streaming worker $idx", e)))
              }
              .reduce(_.merge(_))
              .map(docs =>
                ByteString(docs.map(BSONUtils.stringify).mkString("\n")))
              .alsoTo(Sink.onComplete {
                case Failure(e) =>
                  Logger.error("Error while streaming consents", e)
              })
      }
  }
}
