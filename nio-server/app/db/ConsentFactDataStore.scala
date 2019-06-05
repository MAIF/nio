package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import models.ConsentFact

import scala.concurrent.Future

trait ConsentFactDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def markAsSendToKafka(tenant: String, ids: Seq[String]): Future[Boolean]

  def updateById(tenant: String,
                 id: String,
                 objToUpdate: ConsentFact): Future[Boolean]

  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean]

  def findById(tenant: String, id: String): Future[Option[ConsentFact]]

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ConsentFact], Int)]

  def findAll(tenant: String): Future[Seq[ConsentFact]]

  def deleteConsentFactByTenant(tenant: String): Future[Boolean]

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean]

  def streamUnSendToKafka(tenant: String)(
      implicit m: Materializer): Future[Source[ConsentFact, Future[Any]]]

}
