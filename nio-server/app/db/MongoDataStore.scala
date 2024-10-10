package db

import org.apache.pekko.http.scaladsl.util.FastFuture
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.Index
import reactivemongo.api.bson.collection.BSONCollection
import utils.NioLogger
import scala.collection.Seq

import scala.concurrent.{ExecutionContext, Future}

object Request {
  implicit class ImprovedRequest(val tenant: String) extends AnyVal {
    def request[A](
        f: BSONCollection => Future[A]
    )(implicit ex: ExecutionContext, req: String => (BSONCollection => Future[A]) => Future[A]): Future[A] =
      req(tenant)(f)
  }
}

trait MongoDataStore[T] {
  import db.MongoOpsDataStore.MongoDataStore
  import Request.ImprovedRequest
  import reactivemongo.api.bson._
  import reactivemongo.play.json.compat._
  import reactivemongo.pekkostream._
  import bson2json._

  def mongoApi: ReactiveMongoApi
  implicit def format: OFormat[T]
  implicit def executionContext: ExecutionContext

  protected def collectionName(tenant: String): String

  protected def indices: Seq[Index.Default]

  protected def storedCollection(tenant: String): Future[BSONCollection] =
    mongoApi.database.map(_.collection(collectionName(tenant)))

  def init(tenant: String): Future[Unit] =
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }

  def ensureIndices(tenant: String): Future[Unit] =
    for {
      db        <- mongoApi.database
      cName      = collectionName(tenant)
      foundName <- db.collectionNames.map(names => names.contains(cName))
      _         <- if (foundName) {
                     NioLogger.info(s"Ensuring indices for $cName")
                     val col = db.collection[BSONCollection](cName)
                     Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
                   } else {
                     Future.successful(Seq.empty[Boolean])
                   }
    } yield ()

  implicit def request[A](tenant: String)(function: BSONCollection => Future[A]): Future[A] =
    storedCollection(tenant).flatMap { col =>
      function(col)
    }

  def insertOne(tenant: String, objToInsert: T): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.insertOne[T](objToInsert)
    }

  def updateOne(tenant: String, id: String, objToUpdate: T): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.updateOne(id, objToUpdate)
    }

  def updateOneByQuery(tenant: String, query: JsObject, objToUpdate: T): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.updateOneByQuery(query, objToUpdate)
    }

  def updateByQuery(tenant: String, query: JsObject, update: JsObject): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.updateByQuery(query, update)
    }

  def findOneById(tenant: String, id: String): Future[Option[T]] =
    tenant.request[Option[T]] { col =>
      col.findOneById(id)
    }

  def findOneByQuery(tenant: String, query: JsObject): Future[Option[T]] =
    tenant.request[Option[T]] { col =>
      col.findOneByQuery(query)
    }

  def findMany(tenant: String): Future[Seq[T]] =
    tenant.request[Seq[T]] { col =>
      col.findMany()
    }

  def streamByQuery(tenant: String, query: JsObject)(implicit mat: Materializer): Future[Source[T, Future[State]]] =
    tenant.request[Source[T, Future[State]]] { col =>
      FastFuture.successful(col.streamByQuery(query))
    }

  def findManyByQuery(tenant: String, query: JsObject): Future[Seq[T]] =
    tenant.request[Seq[T]] { col =>
      col.findManyByQuery(query)
    }

  def findManyByQueryPaginateCount(
      tenant: String,
      query: JsObject,
      sort: JsObject = Json.obj("_id" -> 1),
      page: Int,
      pageSize: Int
  ): Future[(Seq[T], Long)] =
    tenant.request[(Seq[T], Long)] { col =>
      col.findManyByQueryPaginateCount(tenant, query, sort, page, pageSize)
    }

  def findManyByQueryPaginate(
      tenant: String,
      query: JsObject,
      sort: JsObject = Json.obj("_id" -> -1),
      page: Int,
      pageSize: Int
  ): Future[Seq[T]] =
    tenant.request[Seq[T]] { col =>
      col.findManyByQueryPaginate(tenant, query, sort, page, pageSize)
    }

  def deleteOneById(tenant: String, id: String): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.deleteOneById(id)
    }

  def deleteByQuery(tenant: String, query: JsObject): Future[Boolean] =
    tenant.request[Boolean] { col =>
      col.deleteByQuery(query)
    }

}
