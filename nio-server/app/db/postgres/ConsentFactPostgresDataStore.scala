package db.postgres

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import db.ConsentFactDataStore
import models.ConsentFact
import play.api.libs.json.{Json, OFormat}
import scalikejdbc.async.AsyncDB
import scalikejdbc._
import async._

import scala.concurrent.{ExecutionContext, Future}

class ConsentFactPostgresDataStore()(
    implicit val executionContext: ExecutionContext)
    extends PostgresDataStore[ConsentFact]
    with ConsentFactDataStore {

  val format: OFormat[ConsentFact] = models.ConsentFact.consentFactOFormats

  override val tableName = "consent_facts"

  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean] =
    insertOne(tenant, consentFact)

  def updateById(tenant: String,
                 id: String,
                 objToUpdate: ConsentFact): Future[Boolean] =
    updateOne(tenant, id, objToUpdate)

  def findById(tenant: String, id: String): Future[Option[ConsentFact]] = {
    findOneById(tenant, id)
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ConsentFact], Int)] = {
    findManyByQueryPaginateCount(tenant = tenant,
                                 query = Json.obj("userId" -> userId),
                                 page = page,
                                 pageSize = pageSize)
  }

  def findAll(tenant: String): Future[Seq[ConsentFact]] =
    findMany(tenant)

  def deleteConsentFactByTenant(tenant: String): Future[Boolean] = {
    deleteByTenant(tenant)
  }

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("orgKey" -> orgKey))
  }

  def markAsSendToKafka(tenant: String, ids: Seq[String]): Future[Boolean] = {
    AsyncDB withPool { implicit session =>
      sql"select jsonb_set(payload, '{sendToKafka}', jsonb 'true') from ${table} where tenant = ${tenant} and  payload->>'_id' IN (${ids})"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def streamUnSendToKafka(tenant: String)(
      implicit m: Materializer): Future[Source[ConsentFact, Future[Any]]] = {
    streamAsyncByQuery(tenant, Json.obj("sendToKafka" -> false))
  }
}
