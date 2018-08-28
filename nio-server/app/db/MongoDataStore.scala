package db

import db.MongoOpsDataStore.MongoDataStore
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.Index
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

object Request {
  implicit class ImprovedRequest(val tenant: String) extends AnyVal {
    def request[A](f: JSONCollection => Future[A])(
        implicit ex: ExecutionContext,
        req: String => (JSONCollection => Future[A]) => Future[A])
      : Future[A] = {
      req(tenant)(f)
    }
  }
}

trait MongoDataStore[T] {

  def mongoApi: ReactiveMongoApi
  implicit def format: OFormat[T]
  implicit def executionContext: ExecutionContext

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

  implicit def request[A](tenant: String)(
      function: JSONCollection => Future[A]): Future[A] = {
    storedCollection(tenant).flatMap { col =>
      function(col)
    }
  }

  import Request.ImprovedRequest

  def insertOne(tenant: String, objToInsert: T): Future[Boolean] = {
    tenant.request[Boolean] { col =>
      col.insertOne[T](objToInsert)
    }
  }

  def updateOne(tenant: String, id: String, objToUpdate: T): Future[Boolean] = {
    tenant.request[Boolean] { col =>
      col.updateOne(id, objToUpdate)
    }
  }

  def updateOneByQuery(tenant: String,
                       query: JsObject,
                       objToUpdate: T): Future[Boolean] = {
    tenant.request[Boolean] { col =>
      col.updateOneByQuery(query, objToUpdate)
    }
  }

  def findOneById(tenant: String, id: String): Future[Option[T]] = {
    tenant.request[Option[T]] { col =>
      col.findOneById(id)
    }
  }

  def findOneByQuery(tenant: String, query: JsObject): Future[Option[T]] = {
    tenant.request[Option[T]] { col =>
      col.findOneByQuery(query)
    }
  }

  def findMany(tenant: String): Future[Seq[T]] = {
    tenant.request[Seq[T]] { col =>
      col.findMany()
    }
  }

  def findManyByQuery(tenant: String, query: JsObject): Future[Seq[T]] = {
    tenant.request[Seq[T]] { col =>
      col.findManyByQuery(query)
    }
  }

  def findManyByQueryPaginateCount(tenant: String,
                                   query: JsObject,
                                   sort: JsObject = Json.obj("_id" -> 1),
                                   page: Int,
                                   pageSize: Int): Future[(Seq[T], Int)] = {
    tenant.request[(Seq[T], Int)] { col =>
      col.findManyByQueryPaginateCount(tenant, query, sort, page, pageSize)
    }

  }

  def findManyByQueryPaginate(tenant: String,
                              query: JsObject,
                              sort: JsObject = Json.obj("_id" -> -1),
                              page: Int,
                              pageSize: Int): Future[Seq[T]] = {
    tenant.request[Seq[T]] { col =>
      col.findManyByQueryPaginate(tenant, query, sort, page, pageSize)
    }
  }

  def deleteOneById(tenant: String, id: String): Future[Boolean] = {
    tenant.request[Boolean] { col =>
      col.deleteOneById(id)
    }
  }

  def deleteByQuery(tenant: String, query: JsObject): Future[Boolean] = {
    tenant.request[Boolean] { col =>
      col.deleteByQuery(query)
    }

  }

}
