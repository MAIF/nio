package db

import akka.NotUsed
import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl.Source
import akka.stream._
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class LastConsentFactMongoDataStore @Inject()(
    val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext)
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

  // def streamAll(tenant: String)(implicit m: Materializer) = {
  //   storedCollection(tenant).map { col =>
  //     col
  //       .find(Json.obj())
  //       .cursor[JsValue]()
  //       .documentSource()
  //   }
  // }

  def streamAll(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[JsValue, akka.NotUsed] = {
    val start = System.currentTimeMillis()
    val options = QueryOpts(batchSizeN = pageSize, flagsN = 0)
    Source.fromFuture(storedCollection(tenant)).flatMapConcat {
      implicit collection =>
        collection
          .find(Json.obj())
          .options(options)
          .cursor[JsValue](ReadPreference.primary)
          .documentSource()
    }

    // FastFuture.successful(
    //   Source
    //     .fromFuture(storedCollection(tenant))
    //     .mapAsync(1)(col => col.count())
    //     .flatMapConcat { count =>
    //       val nbPages = count / pageSize
    //       Source(1 to nbPages)
    //         .mapAsyncUnordered(parallelisation) { p =>
    //           readPage(tenant, pageSize, p.toInt, start, nbPages)
    //         }
    //         .mapConcat(_.toList)
    //     })
  }

  import scala.concurrent.duration._

  private def durationToHumanReadable(fdur: FiniteDuration): String = {
    val duration = fdur.toMillis
    val milliseconds = duration % 1000L
    val seconds = (duration / 1000L) % 60L
    val minutes = (duration / (1000L * 60L)) % 60L
    val hours = (duration / (1000L * 3600L)) % 24L
    val days = (duration / (1000L * 86400L)) % 7L
    val weeks = (duration / (1000L * 604800L)) % 4L
    val months = (duration / (1000L * 2592000L)) % 52L
    val years = (duration / (1000L * 31556952L)) % 10L
    val decades = (duration / (1000L * 31556952L * 10L)) % 10L
    val centuries = (duration / (1000L * 31556952L * 100L)) % 100L
    val millenniums = (duration / (1000L * 31556952L * 1000L)) % 1000L
    val megaannums = duration / (1000L * 31556952L * 1000000L)

    val sb = new scala.collection.mutable.StringBuilder()

    if (megaannums > 0) sb.append(megaannums + " megaannums ")
    if (millenniums > 0) sb.append(millenniums + " millenniums ")
    if (centuries > 0) sb.append(centuries + " centuries ")
    if (decades > 0) sb.append(decades + " decades ")
    if (years > 0) sb.append(years + " years ")
    if (months > 0) sb.append(months + " months ")
    if (weeks > 0) sb.append(weeks + " weeks ")
    if (days > 0) sb.append(days + " days ")
    if (hours > 0) sb.append(hours + " hours ")
    if (minutes > 0) sb.append(minutes + " minutes ")
    if (seconds > 0) sb.append(seconds + " seconds ")
    if (minutes < 1 && hours < 1 && days < 1) {
      if (sb.nonEmpty) sb.append(" ")
      sb.append(milliseconds + " milliseconds")
    }
    sb.toString().trim
  }

  def readPage(tenant: String,
               pageSize: Int,
               page: Int,
               start: Long,
               nbPages: Int) = {
    val from = (page - 1) * pageSize
    val options =
      QueryOpts( /*skipN = from, */ batchSizeN = pageSize, flagsN = 0)

    val chunkTime = System.currentTimeMillis()
    storedCollection(tenant)
      .flatMap { implicit collection =>
        collection
          .find(Json.obj())
          .options(options)
          .cursor[JsValue](ReadPreference.primary)
          .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[JsValue]]())
      }
      .andThen {
        case _ => {
          val duration = (System.currentTimeMillis() - chunkTime).millis
          val currentDuration = durationToHumanReadable(duration)
          val finishingIn = Try {
            durationToHumanReadable(
              ((nbPages - page) * duration.toMillis).millis)
          }.recover { case e => e.getMessage }.getOrElse("--")
          Logger.info(
            s"====> $page/$nbPages ($currentDuration ms. per chunk, finishing in $finishingIn ms.)")
        }
      }
  }

}
