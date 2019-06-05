package db.mongo

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import db.ConsentFactDataStore
import models.ConsentFact
import play.api.libs.json.{Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

class ConsentFactMongoDataStore(val mongoApi: ReactiveMongoApi)(
    implicit val executionContext: ExecutionContext)
    extends MongoDataStore[ConsentFact]
    with ConsentFactDataStore {

  val format: OFormat[ConsentFact] = models.ConsentFact.consentFactOFormats
  override def collectionName(tenant: String) = s"$tenant-consentFacts"

  override def indices = Seq(
    Index(Seq("orgKey" -> IndexType.Ascending, "userId" -> IndexType.Ascending),
          name = Some("orgKey_userId"),
          unique = false,
          sparse = true),
    Index(Seq("orgKey" -> IndexType.Ascending),
          name = Some("orgKey"),
          unique = false,
          sparse = true),
    Index(Seq("userId" -> IndexType.Ascending),
          name = Some("userId"),
          unique = false,
          sparse = true)
  )
  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean] =
    insertOne(tenant, consentFact)

  def findById(tenant: String, id: String): Future[Option[ConsentFact]] =
    findOneById(tenant, id)

  def updateById(tenant: String,
                 id: String,
                 objToUpdate: ConsentFact): Future[Boolean] =
    updateOne(tenant, id, objToUpdate)

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ConsentFact], Int)] =
    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj("userId" -> userId),
                                 page = page,
                                 pageSize = pageSize)

  def findAll(tenant: String): Future[Seq[ConsentFact]] =
    findMany(tenant)

  def deleteConsentFactByTenant(tenant: String): Future[Boolean] = {
    storedCollection(tenant).flatMap { col =>
      col.drop(failIfNotFound = false)
    }
  }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))
  }

  override def markAsSendToKafka(tenant: String,
                                 ids: Seq[String]): Future[Boolean] = {
    updateByQuery(tenant,
                  Json.obj("_id" -> Json.obj("$in" -> ids)),
                  Json.obj("$set" -> Json.obj("sendToKafka" -> true)))
  }

  override def streamUnSendToKafka(tenant: String)(
      implicit m: Materializer): Future[Source[ConsentFact, Future[Any]]] = {
    streamByQuery(tenant, Json.obj("sendToKafka" -> false))
  }
}
