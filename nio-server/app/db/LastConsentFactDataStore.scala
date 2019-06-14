package db

import akka.stream._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.AppErrorWithStatus
import models._
import utils.Result.AppErrors

import scala.concurrent.Future

trait LastConsentFactDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def insert(tenant: String, consentFact: ConsentFact): Future[Boolean]

  def findById(tenant: String, id: String): Future[Option[ConsentFact]]

  def findOneByUserId(tenant: String,
                      userId: String): Future[Option[ConsentFact]]

  def findByOrgKeyAndUserId(tenant: String,
                            orgKey: String,
                            userId: String): Future[Option[ConsentFact]]

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ConsentFact], Int)]

  def findAll(tenant: String): Future[Seq[ConsentFact]]

  def deleteConsentFactByTenant(tenant: String): Future[Boolean]

  def removeByOrgKey(tenant: String, orgKey: String): Future[Boolean]

  def removeById(tenant: String, id: String): Future[Boolean]

  def removeOffer(tenant: String,
                  orgKey: String,
                  offerKey: String): Future[Either[AppErrorWithStatus, Boolean]]

  def findConsentOffer(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrors, Option[ConsentOffer]]]

  def removeOfferById(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrorWithStatus, ConsentOffer]]

  def update(tenant: String,
             orgKey: String,
             userId: String,
             consentFact: ConsentFact): Future[Boolean]

  def streamAllByteString(tenant: String)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed]

  def streamAllByteString(tenant: String, pageSize: Int, parallelisation: Int)(
      implicit m: Materializer): Source[ByteString, akka.NotUsed]
}
