package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.akkastream.{State, cursorProducer}
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

object MongoOpsDataStore {

  implicit class MongoDataStore(coll: JSONCollection)(
      implicit executionContext: ExecutionContext) {

    def insertOne[T](objToInsert: T)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      coll.insert[T](objToInsert).map(_.ok)
    }

    def updateOne[T](id: String, objToUpdate: T)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      updateOne(Json.obj("_id" -> id), objToUpdate)
    }

    def updateOneByQuery[T](query: JsObject, objToUpdate: T)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      updateOne(query, objToUpdate)
    }

    def updateOne[T](query: JsObject, objToUpdate: T)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      coll.update(query, objToUpdate).map(_.ok)
    }

    def findOneById[T](id: String)(
        implicit oformat: OFormat[T]): Future[Option[T]] = {
      findOne(Json.obj("_id" -> id))
    }

    def findOneByQuery[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Option[T]] = {
      findOne(query)
    }

    private def findOne[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Option[T]] = {
      coll.find(query).one[T]
    }

    def findMany[T]()(implicit oformat: OFormat[T]): Future[Seq[T]] = {
      findByQuery(Json.obj())
    }

    def findManyByQuery[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Seq[T]] = {
      findByQuery(query)
    }

    def findManyByQueryPaginateCount[T](
        tenant: String,
        query: JsObject,
        sort: JsObject = Json.obj("_id" -> 1),
        page: Int,
        pageSize: Int)(implicit oformat: OFormat[T]): Future[(Seq[T], Int)] = {
      val options = QueryOpts(skipN = page * pageSize, pageSize)
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

    def findManyByQueryPaginate[T](
        tenant: String,
        query: JsObject,
        sort: JsObject = Json.obj("_id" -> -1),
        page: Int,
        pageSize: Int)(implicit oformat: OFormat[T]): Future[Seq[T]] = {
      val options = QueryOpts(skipN = page * pageSize, pageSize)
      coll
        .find(query)
        .sort(sort)
        .options(options)
        .cursor[T](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[T]]())
    }

    private def findByQuery[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Seq[T]] = {
      coll
        .find(query)
        .cursor[T](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = -1, Cursor.FailOnError[Seq[T]]())
    }

    def deleteOneById[T](id: String)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      delete(Json.obj("_id" -> id))
    }

    def streamByQuery[T](query: JsObject)(
        implicit oformat: OFormat[T],
        mat: Materializer): Source[T, Future[State]] = {
      coll
        .find(query)
        .cursor[T](ReadPreference.primaryPreferred)
        .documentSource()
    }

    def deleteByQuery[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      delete(query)
    }

    private def delete[T](query: JsObject)(
        implicit oformat: OFormat[T]): Future[Boolean] = {
      coll.remove(query).map(_.ok)
    }
  }

}
