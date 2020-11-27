package db

import akka.http.scaladsl.util.FastFuture
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import controllers.AppErrorWithStatus
import models._
import utils.NioLogger
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.Results.NotFound
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import utils.BSONUtils
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.collection.{immutable, Seq}

class LastConsentFactMongoDataStore(val mongoApi: ReactiveMongoApi)(implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ConsentFact] {

  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import reactivemongo.akkastream._
  import lax._
  import bson2json._
  import json2bson._
  val format: OFormat[ConsentFact] = models.ConsentFact.consentFactOFormats

  override def collectionName(tenant: String) = s"$tenant-lastConsentFacts"

  override def indices = Seq(
    Index(
      immutable.Seq("orgKey" -> IndexType.Ascending, "userId" -> IndexType.Ascending),
      name = Some("orgKey_userId"),
      unique = true,
      sparse = true
    ),
    Index(immutable.Seq("orgKey" -> IndexType.Ascending), name = Some("orgKey"), unique = false, sparse = true),
    Index(immutable.Seq("userId" -> IndexType.Ascending), name = Some("userId"), unique = false, sparse = true)
  )

  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean] =
    insertOne(tenant, consentFact)

  def findById(tenant: String, id: String): Future[Option[ConsentFact]] =
    findOneById(tenant, id)

  def findByOrgKeyAndUserId(tenant: String, orgKey: String, userId: String): Future[Option[ConsentFact]]          =
    findOneByQuery(tenant, Json.obj("orgKey" -> orgKey, "userId" -> userId))

  def findAllByUserId(tenant: String, userId: String, page: Int, pageSize: Int): Future[(Seq[ConsentFact], Long)] =
    findManyByQueryPaginateCount(
      tenant = tenant,
      query = Json.obj("userId" -> userId),
      page = page,
      pageSize = pageSize
    )

  def findAll(tenant: String): Future[Seq[ConsentFact]] =
    findMany(tenant)

  def deleteConsentFactByTenant(tenant: String): Future[Boolean] =
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] =
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))

  def removeById(tenant: String, id: String): Future[Boolean]         =
    deleteOneById(tenant, id)

  import play.api.mvc.Results._

  def removeOffer(tenant: String, orgKey: String, offerKey: String): Future[Either[AppErrorWithStatus, Boolean]] =
    findManyByQuery(tenant, Json.obj("orgKey" -> orgKey, "offers.key" -> offerKey))
      .flatMap {
        case Nil          =>
          FastFuture.successful(Right(true))
        case consentFacts =>
          Future
            .sequence(
              consentFacts.map(c => removeOfferById(tenant, orgKey, c.userId, offerKey))
            )
            .map(sequence)
            .map { e =>
              e.left.map(m =>
                controllers.AppErrorWithStatus(
                  AppErrors(
                    m.flatMap(_.appErrors.errors)
                  ),
                  BadRequest
                )
              )
            }
            .map { e =>
              e.right.map(_ => true)
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
      offerKey: String
  ): Future[Either[AppErrors, Option[ConsentOffer]]] =
    findByOrgKeyAndUserId(tenant, orgKey, userId).map {
      case Some(consentFact) =>
        Right(consentFact.offers.flatMap(offers => offers.find(p => p.key == offerKey)))
      case None              =>
        Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
    }

  def removeOfferById(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String
  ): Future[Either[AppErrorWithStatus, ConsentOffer]] =
    findConsentOffer(tenant, orgKey, userId, offerKey).flatMap {
      case Right(maybeOffer) =>
        maybeOffer match {
          case Some(offer) =>
            storedCollection(tenant).flatMap { coll =>
              val builder = coll.update(ordered = false)
              val updates = builder.element(
                q = Json.obj("orgKey" -> orgKey, "userId" -> userId, "offers.key" -> offerKey),
                u = Json.obj(
                  "$pull" -> Json.obj(
                    "offers" -> offer.asJson()
                  )
                ),
                upsert = false,
                multi = true
              )

              updates
                .flatMap(u => builder.many(List(u)))
                .map(_ => Right(offer))
            }

          case None =>
            FastFuture.successful(
              Left(AppErrorWithStatus(AppErrors(Seq(ErrorMessage(s"offer.$offerKey.not.found"))), NotFound))
            )
        }
      case Left(e)           =>
        FastFuture.successful(Left(AppErrorWithStatus(e, NotFound)))
    }

  def update(tenant: String, orgKey: String, userId: String, consentFact: ConsentFact): Future[Boolean] =
    updateOneByQuery(tenant, Json.obj("orgKey" -> orgKey, "userId" -> userId), consentFact)

  def streamAll(tenant: String, pageSize: Int, parallelisation: Int)(implicit
      m: Materializer
  ): Source[JsValue, akka.NotUsed]                                                                      =
    Source
      .future(storedCollection(tenant))
      .mapAsync(1)(coll => coll.count().map(c => (coll, c)))
      .flatMapConcat { case (collection, count) =>
        NioLogger.info(s"Will stream a total of $count consents")
        (0 until parallelisation)
          .map { idx =>
            val items     = (count / parallelisation).toInt
            val from: Int = items * idx
            val to        = from + items
            NioLogger.info(s"Consuming $items consents with worker $idx: $from => $to")

            collection
              .find(Json.obj())
              .skip(from)
              .cursor[JsValue](ReadPreference.primary)
              .documentSource(
                maxDocs = items,
                err = Cursor.FailOnError((_, e) => NioLogger.error(s"Error while streaming worker $idx", e))
              )
          }
          .reduce(_.merge(_))
          .alsoTo(Sink.onComplete { case Failure(e) =>
            NioLogger.error("Error while streaming consents", e)
          })
      }

  def storedBSONCollection(tenant: String): Future[BSONCollection] =
    mongoApi.database.map(_.collection[BSONCollection](collectionName(tenant)))

  def streamAllBSON(tenant: String, pageSize: Int, parallelisation: Int)(implicit
      m: Materializer
  ): Source[ByteString, akka.NotUsed] =
    Source
      .future(storedBSONCollection(tenant))
      .mapAsync(1)(coll => coll.count().map(c => (coll, c)))
      .flatMapConcat { case (collection, count) =>
        NioLogger.info(s"Will stream a total of $count consents")
        (0 until parallelisation)
          .map { idx =>
            val items = (count / parallelisation).toInt
            val from  = items * idx
            val to    = from + items
            NioLogger.info(s"Consuming $items consents with worker $idx: $from => $to")

            collection
              .find(BSONDocument())
              .skip(from)
              .cursor[BSONDocument](ReadPreference.primary)
              .bulkSource(
                maxDocs = items,
                err = Cursor.FailOnError((_, e) => NioLogger.error(s"Error while streaming worker $idx", e))
              )
          }
          .reduce(_.merge(_))
          .map(docs => docs.map(d => BSONUtils.stringify(d)).mkString("\n"))
          .map(docs => ByteString(docs))
          .alsoTo(Sink.onComplete { case Failure(e) =>
            NioLogger.error("Error while streaming consents", e)
          })
      }
}
