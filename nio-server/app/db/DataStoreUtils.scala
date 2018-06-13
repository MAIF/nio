package db

import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.Index
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

trait DataStoreUtils {

  val reactiveMongoApi: ReactiveMongoApi
  implicit val ec: ExecutionContext

  def collectionName(tenant: String): String

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(collectionName(tenant)))

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
      foundName <- db.collectionNames.filter(_.contains(cName))
      xxx = db.collection[JSONCollection](cName) if foundName.nonEmpty
      _ = Logger.info(s"Ensuring indices for $cName")
      _ <- Future.sequence(indices.map(i => xxx.indexesManager.ensure(i)))
    } yield { () }

}
