package db

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import models._
import play.api.Logger
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.bson.BSONDocument
import utils.BSONUtils

import scala.concurrent.ExecutionContext
import scala.util.Failure

class LastConsentFactMongoDataStore(val reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext)
    extends DataStoreUtils {

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

  implicit def format: Format[ConsentFact] = ConsentFact.consentFactFormats

  def insert(tenant: String, consentFact: ConsentFact) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(consentFact).as[JsObject]).map(_.ok))

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
    storedCollection(tenant).flatMap(_.find(query).one[ConsentFact])
  }

  def findByOrgKeyAndUserId(tenant: String, orgKey: String, userId: String) = {
    val query = Json.obj("orgKey" -> orgKey, "userId" -> userId)
    storedCollection(tenant).flatMap(_.find(query).one[ConsentFact])
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("userId" -> userId), page, pageSize)
  }

  private def findAllByQuery(tenant: String,
                             query: JsObject,
                             page: Int,
                             pageSize: Int) = {
    val options = QueryOpts(skipN = page * pageSize, pageSize)
    storedCollection(tenant).flatMap { coll =>
      for {
        count <- coll.count(Some(query))
        queryRes <- coll
          .find(query)
          .sort(Json.obj("lastUpdate" -> -1))
          .options(options)
          .cursor[ConsentFact](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize,
                        Cursor.FailOnError[Seq[ConsentFact]]())
      } yield {
        (queryRes, count)
      }
    }
  }

  def findAll(tenant: String) =
    storedCollection(tenant).flatMap(
      _.find(Json.obj())
        .cursor[ConsentFact](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[ConsentFact]]()))

  def deleteConsentFactByTenant(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByOrgKey(tenant: String, orgKey: String) = {
    storedCollection(tenant).flatMap { col =>
      col.remove(Json.obj("orgKey" -> orgKey))
    }
  }

  def removeById(tenant: String, id: String) = {
    storedCollection(tenant).flatMap { col =>
      col.remove(Json.obj("_id" -> id))
    }
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
              val from = (items * idx)
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
