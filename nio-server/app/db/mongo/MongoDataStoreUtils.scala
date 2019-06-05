package db.mongo

import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.Index
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

trait MongoDataStoreUtils {

  val reactiveMongoApi: ReactiveMongoApi
  implicit val ec: ExecutionContext

  def collectionName(tenant: String): String

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(collectionName(tenant)))

  def storedBSONCollection(tenant: String): Future[BSONCollection] =
    reactiveMongoApi.database.map(
      _.collection[BSONCollection](collectionName(tenant)))

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def indices: Seq[Index]

  def ensureIndices(tenant: String) =
    for {
      db <- reactiveMongoApi.database
      cName = collectionName(tenant)
      foundName <- db.collectionNames.map(names => names.contains(cName))
      _ <- if (foundName) {
        Logger.info(s"Ensuring indices for $cName")
        val col = db.collection[JSONCollection](cName)
        Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
      } else {
        Future.successful(Seq.empty[Boolean])
      }
    } yield { () }

}
