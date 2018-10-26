package db

import models.UserAccount
import play.api.libs.json.{Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class UserAccountMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[UserAccount] {

  val format: OFormat[UserAccount] = models.UserAccount.oformats

  override protected def collectionName(tenant: String = ""): String = {
    s"UserAccount"
  }

  override protected def indices: Seq[Index] = Seq(
    Index(Seq("email" -> IndexType.Ascending),
          name = Some("email"),
          unique = true,
          sparse = true),
    Index(Seq("clientId" -> IndexType.Ascending),
          name = Some("clientId"),
          unique = true,
          sparse = true)
  )

  def findByClientId(clientId: String): Future[Option[UserAccount]] = {
    findOneByQuery("", Json.obj("clientId" -> clientId))
  }

  def findMany(): Future[Seq[UserAccount]] =
    findMany("")

  def insertOne(objToInsert: UserAccount): Future[Boolean] =
    insertOne("", objToInsert)
}
