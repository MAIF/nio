package db

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class LastConsentFactMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ConsentFact] {

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

  def update(tenant: String,
             orgKey: String,
             userId: String,
             consentFact: ConsentFact): Future[Boolean] = {
    updateOneByQuery(tenant,
                     Json.obj("orgKey" -> orgKey, "userId" -> userId),
                     consentFact)
  }

  def streamAll(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[JsValue, akka.NotUsed] = {

    Source
      .fromFuture(storedCollection(tenant))
      .mapAsync(1)(coll => coll.count().map(c => (coll, c)))
      .flatMapConcat {
        case (collection, count) =>
          Logger.info(s"Will stream a total of $count consents")
          (0 until parallelisation)
            .map { idx =>
              val items = count / parallelisation
              val from = (items * idx)
              val to = from + items
              Logger.info(
                s"Consuming $items consents with worker $idx: $from => $to")
              val options =
                QueryOpts(skipN = from, batchSizeN = pageSize, flagsN = 0)
              collection
                .find(Json.obj())
                .options(options)
                .cursor[JsValue](ReadPreference.primary)
                .documentSource(
                  maxDocs = items,
                  err = Cursor.FailOnError((_, e) =>
                    Logger.error(s"Error while streaming worker $idx", e)))
            }
            .reduce(_.merge(_))
            .alsoTo(Sink.onComplete {
              case Failure(e) =>
                Logger.error("Error while streaming consents", e)
            })
      }
  }

  def storedBSONCollection(tenant: String): Future[BSONCollection] =
    mongoApi.database.map(_.collection[BSONCollection](collectionName(tenant)))

  def streamAllBSON(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed] = {

    Source
      .fromFuture(storedBSONCollection(tenant))
      .mapAsync(1)(coll => coll.count().map(c => (coll, c)))
      .flatMapConcat {
        case (collection, count) =>
          Logger.info(s"Will stream a total of $count consents")
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
