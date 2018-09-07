package db

import models.UserExtractTask
import play.api.libs.json.{Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class UserExtractTaskDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[UserExtractTask] {
  override implicit def format: OFormat[UserExtractTask] =
    UserExtractTask.oformat

  override protected def collectionName(tenant: String): String =
    s"$tenant-userExtractTask"

  override protected def indices: Seq[Index] = Seq(
    Index(
      Seq("tenant" -> IndexType.Ascending,
          "orgKey" -> IndexType.Ascending,
          "userId" -> IndexType.Ascending),
      name = Some("tenant_orgKey_userId"),
      unique = true,
      sparse = true
    ),
    Index(
      Seq("orgKey" -> IndexType.Ascending, "userId" -> IndexType.Ascending),
      name = Some("orgKey_userId"),
      unique = false,
      sparse = true
    )
  )

  def create(userExtractTask: UserExtractTask): Future[Boolean] = {
    insertOne(userExtractTask.tenant, userExtractTask)
  }

  def update(_id: String, userExtractTask: UserExtractTask) = {
    updateOne(userExtractTask.tenant, _id, userExtractTask)
  }

  def delete(tenant: String,
             orgKey: String,
             userId: String): Future[Boolean] = {
    deleteByQuery(
      tenant,
      Json.obj("tenant" -> tenant, "orgKey" -> orgKey, "userId" -> userId))
  }

  def find(tenant: String,
           orgKey: String,
           userId: String): Future[Option[UserExtractTask]] =
    findOneByQuery(
      tenant,
      Json.obj("tenant" -> tenant, "orgKey" -> orgKey, "userId" -> userId))

  def findByOrgKey(tenant: String,
                   orgKey: String): Future[Seq[UserExtractTask]] =
    findManyByQuery(tenant, Json.obj("tenant" -> tenant, "orgKey" -> orgKey))

  def deleteUserExtractTaskByTenant(tenant: String): Future[Boolean] =
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
}