package db

import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.Index
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractMongoDataStore[T](mongoApi: ReactiveMongoApi)(
    implicit val format: OFormat[T],
    executionContext: ExecutionContext) {

  protected def collectionName(tenant: String): String

  protected def indices: Seq[Index]

  protected def storedCollection(tenant: String): Future[JSONCollection] =
    mongoApi.database.map(_.collection(collectionName(tenant)))

  def init(tenant: String): Future[Unit] = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def ensureIndices(tenant: String): Future[Unit] =
    for {
      db <- mongoApi.database
      cName = collectionName(tenant)
      foundName <- db.collectionNames.map(names => names.contains(cName))
      _ <- if (foundName) {
        Logger.info(s"Ensuring indices for $cName")
        val col = db.collection[JSONCollection](cName)
        Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
      } else {
        Future.successful(Seq.empty[Boolean])
      }
    } yield {
      ()
    }

  def insertOne(tenant: String, objToInsert: T): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.insert[T](objToInsert).map(_.ok)
    )
  }

  def updateOne(tenant: String, id: String, objToUpdate: T): Future[Boolean] = {
    updateOne(tenant, Json.obj("_id" -> id), objToUpdate)
  }

  def updateOneByQuery(tenant: String,
                       query: JsObject,
                       objToUpdate: T): Future[Boolean] = {
    updateOne(tenant, query, objToUpdate)
  }

  private def updateOne(tenant: String,
                        query: JsObject,
                        objToUpdate: T): Future[Boolean] = {
    storedCollection(tenant).flatMap {
      _.update(query, objToUpdate).map(_.ok)
    }
  }

  def findOneById(tenant: String, id: String): Future[Option[T]] = {
    findOne(tenant, Json.obj("_id" -> id))
  }

  def findOneByQuery(tenant: String, query: JsObject): Future[Option[T]] = {
    findOne(tenant, query)
  }

  private def findOne(tenant: String, query: JsObject): Future[Option[T]] = {
    storedCollection(tenant).flatMap(
      _.find(query).one[T]
    )
  }

  def findMany(tenant: String): Future[Seq[T]] = {
    find(tenant, Json.obj())
  }

  def findManyByQuery(tenant: String, query: JsObject): Future[Seq[T]] = {
    find(tenant, query)
  }

  def findManyByQueryPaginateCount(tenant: String,
                                   query: JsObject,
                                   sort: JsObject = Json.obj("_id" -> 1),
                                   page: Int,
                                   pageSize: Int): Future[(Seq[T], Int)] = {
    val options = QueryOpts(skipN = page * pageSize, pageSize)
    storedCollection(tenant).flatMap { coll =>
      for {
        count <- coll.count(Some(query))
        queryRes <- coll
          .find(query)
          .sort(sort)
          .options(options)
          .cursor[T](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[T]]())
      } yield {
        (queryRes, count)
      }
    }
  }

  def findManyByQueryPaginate(tenant: String,
                              query: JsObject,
                              sort: JsObject = Json.obj("_id" -> -1),
                              page: Int,
                              pageSize: Int): Future[Seq[T]] = {
    val options = QueryOpts(skipN = page * pageSize, pageSize)
    storedCollection(tenant).flatMap {
      _.find(query)
        .sort(sort)
        .options(options)
        .cursor[T](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[T]]())
    }
  }

  private def find(tenant: String, query: JsObject): Future[Seq[T]] = {
    storedCollection(tenant).flatMap {
      _.find(query)
        .cursor[T](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = -1, Cursor.FailOnError[Seq[T]]())
    }
  }

  def deleteOneById(tenant: String, id: String): Future[Boolean] = {
    delete(tenant, Json.obj("_id" -> id))
  }

  def deleteByQuery(tenant: String, query: JsObject): Future[Boolean] = {
    delete(tenant, query)
  }

  private def delete(tenant: String, query: JsObject): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.remove(query).map(_.ok)
    )
  }
}
