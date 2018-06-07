package db

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.{JSONBatchCommands, JSONCollection}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.play.json.collection.JSONBatchCommands.JSONUpdateCommand

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConsentFactMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-consentFacts"))

  implicit def format: Format[ConsentFact] = ConsentFact.consentFactFormats

  def insert(tenant: String, consentFact: ConsentFact) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(consentFact).as[JsObject]).map(_.ok))

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
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

  def updateByUserId(tenant: String,
                     id: String,
                     consentFact: ConsentFact): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("userId" -> id),
               format.writes(consentFact).as[JsObject])
        .map(_.ok))
  }

  def findAll(tenant: String) =
    storedCollection(tenant).flatMap(
      _.find(Json.obj())
        .cursor[ConsentFact](ReadPreference.primaryPreferred)
        .collect[Seq](-1, Cursor.FailOnError[Seq[ConsentFact]]()))

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

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

  def bulkInsert(tenant: String, bulkSize: Int, parallelisation: Int): Flow[ConsentFact, NioEvent, NotUsed] =
    Flow[ConsentFact]
      .grouped(bulkSize)
      .flatMapMerge(10, { group =>

        Source.fromFuture(storedCollection(tenant))
        .flatMapConcat { collection =>
          val userIds = group.map(_.userId)
          Source.fromFuture(
            collection.find(Json.obj("userId" -> Json.obj("$in" -> userIds)))
              .cursor[ConsentFact](ReadPreference.primaryPreferred)
              .collect[Seq](-1, Cursor.FailOnError[Seq[ConsentFact]]())
          )
            .map { existing =>
              val existingIds = existing.map(_.userId)

              val toUpdate = group.filter(c => existingIds.contains(c.userId))
              val toInsert = group.filter(c => !existingIds.contains(c.userId))

              (toUpdate, toInsert)
            }
            .flatMapConcat {
              case (toUpdate, toInsert) =>

                val inserted = Source
                  .fromFuture(collection.insert(false).many(toInsert))
                  .flatMapConcat { _ =>
                    Source(toInsert.map(i => ConsentFactCreated(tenant, ...)))
                  }

                val update = collection.update(false)

                val updated =
                  Source(toUpdate)
                    .mapAsync(4) { u =>
                      update.element(Json.obj("userId" -> u.userId), Json.obj("$set" -> format.writes(u)), upsert = false, multi = false)
                    }
                    .fold(Seq.empty) { _ :+ _ }
                    .mapAsync(1) { update.many }
                    .flatMapConcat { _ =>

                      Source(toUpdate.map { u => ConsentFactUpdated() })
                    }

                inserted.merge(updated)
            }
        }
      })

}
