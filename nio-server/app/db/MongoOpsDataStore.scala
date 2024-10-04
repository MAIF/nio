package db

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.Seq

object MongoOpsDataStore {

  implicit class MongoDataStore(coll: BSONCollection)(implicit executionContext: ExecutionContext) {

    import play.api.libs.json._
    import reactivemongo.api.bson._
    import reactivemongo.play.json.compat._
    import reactivemongo.pekkostream._

    def insertOne[T](objToInsert: T)(implicit oformat: OFormat[T]): Future[Boolean] = {
      import json2bson._
      implicit val writer = implicitly[BSONDocumentWriter[T]]
      coll.insert.one[T](objToInsert).map(_.writeErrors.isEmpty)
    }

    def updateOne[T](id: String, objToUpdate: T)(implicit oformat: OFormat[T]): Future[Boolean]             =
      updateOne(Json.obj("_id" -> id), objToUpdate)

    def updateOneByQuery[T](query: JsObject, objToUpdate: T)(implicit oformat: OFormat[T]): Future[Boolean] =
      updateOne(query, objToUpdate)

    def updateByQuery[T](query: JsObject, update: JsObject)(implicit OFormat: OFormat[T]): Future[Boolean] = {
      import json2bson._
      import bson2json._
//      implicit val writer = implicitly[BSONDocumentWriter[T]]
      val builder = coll.update(ordered = false)
      val updates = builder.element(q = query, u = update, upsert = true, multi = false)
      updates.flatMap(d => builder.many(List(d))).map(_.writeErrors.isEmpty)
    }

    def updateOne[T](query: JsObject, objToUpdate: T)(implicit oformat: OFormat[T]): Future[Boolean] = {
      import json2bson._
      implicit val writer = implicitly[BSONDocumentWriter[T]]
      coll.update.one(query, objToUpdate).map(_.writeErrors.isEmpty)
    }

    def findOneById[T](id: String)(implicit oformat: OFormat[T]): Future[Option[T]] = {
      import json2bson._
      import bson2json._
      findOne(Json.obj("_id" -> id))
    }

    def findOneByQuery[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Option[T]] = {
      import json2bson._
      import bson2json._
      findOne(query)
    }

    private def findOne[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Option[T]] = {
      import json2bson._
      import bson2json._
      coll.find(query).one[JsValue].map(_.flatMap(_.validate[T].asOpt))
    }

    def findMany[T]()(implicit oformat: OFormat[T]): Future[Seq[T]] =
      findByQuery(Json.obj())

    def findManyByQuery[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Seq[T]] =
      findByQuery(query)

    def findManyByQueryPaginateCount[T](
        tenant: String,
        query: JsObject,
        sort: JsObject = Json.obj("_id" -> 1),
        page: Int,
        pageSize: Int
    )(implicit oformat: OFormat[T]): Future[(Seq[T], Long)] = {
      import json2bson._
      import bson2json._
      val search: Future[Seq[T]] = coll
        .find(query)
        .sort(sort)
        .skip(page * pageSize)
        .cursor[JsValue](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = pageSize, err = Cursor.FailOnError[Seq[JsValue]]())
        .map(_.flatMap(_.validate[T].asOpt))
      val count                  = coll.count(Option(query))
      for {
        c <- count
        q <- search
      } yield (q, c)
    }

    def findManyByQueryPaginate[T](
        tenant: String,
        query: JsObject,
        sort: JsObject = Json.obj("_id" -> -1),
        page: Int,
        pageSize: Int
    )(implicit oformat: OFormat[T]): Future[Seq[T]] = {
      import json2bson._
      import bson2json._
      coll
        .find(query)
        .sort(sort)
        .skip(page * pageSize)
        .cursor[JsValue](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = pageSize, Cursor.FailOnError[Seq[JsValue]]())
        .map(_.flatMap(_.validate[T].asOpt))
    }

    private def findByQuery[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Seq[T]] = {
      import json2bson._
      import bson2json._
      coll
        .find(query)
        .cursor[JsValue](ReadPreference.primaryPreferred)
        .collect[Seq](maxDocs = -1, Cursor.FailOnError[Seq[JsValue]]())
        .map(_.flatMap(_.validate[T].asOpt))
    }

    def deleteOneById[T](id: String)(implicit oformat: OFormat[T]): Future[Boolean] =
      delete(Json.obj("_id" -> id))

    def streamByQuery[T](query: JsObject)(implicit oformat: OFormat[T], mat: Materializer): Source[T, Future[State]] = {
      import json2bson._
      import bson2json._
      coll
        .find(query)
        .cursor[JsValue](ReadPreference.primaryPreferred)
        .documentSource()
        .mapConcat(_.validate[T].asOpt.toList)
    }

    def deleteByQuery[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Boolean] =
      delete(query)

    private def delete[T](query: JsObject)(implicit oformat: OFormat[T]): Future[Boolean] = {
      import json2bson._
      import bson2json._
      val builder = coll.delete(ordered = false)
      builder
        .element(q = query, limit = None, collation = None)
        .flatMap(d => builder.many(List(d)))
        .map(_.writeErrors.isEmpty)
    }
  }

}
