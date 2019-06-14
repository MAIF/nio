package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import controllers.AppErrorWithStatus
import models._
import play.api.libs.json.JsValue
import utils.Result.AppErrors

import scala.concurrent.Future

trait OrganisationDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def insert(tenant: String, organisation: Organisation): Future[Boolean]

  def findById(tenant: String, id: String): Future[Option[Organisation]]

  def findByKey(tenant: String, key: String): Future[Option[Organisation]]

  def findAllReleasedByKey(tenant: String,
                           key: String): Future[Seq[Organisation]]

  def findLastReleasedByKey(tenant: String,
                            key: String): Future[Option[Organisation]]

  def findDraftByKey(tenant: String, key: String): Future[Option[Organisation]]

  def findReleasedByKeyAndVersionNum(
      tenant: String,
      key: String,
      versionNum: Int): Future[Option[Organisation]]

  def updateById(tenant: String,
                 id: String,
                 value: Organisation): Future[Boolean]

  def findAllLatestReleasesOrDrafts(tenant: String): Future[Seq[Organisation]]

  def findAll(tenant: String): Future[Seq[Organisation]]

  def deleteOrganisationByTenant(tenant: String): Future[Boolean]

  def removeByKey(tenant: String, orgKey: String): Future[Boolean]

  def findAllLatestReleasesOrDraftsByDate(tenant: String,
                                          from: String,
                                          to: String): Future[Seq[Organisation]]

  def streamAllLatestReleasesOrDraftsByDate(
      tenant: String,
      from: String,
      to: String)(implicit m: Materializer): Future[Source[JsValue, Any]]

  def findOffers(tenant: String,
                 orgKey: String): Future[Either[AppErrors, Option[Seq[Offer]]]]

  def findOffer(tenant: String,
                orgKey: String,
                offerKey: String): Future[Either[AppErrors, Option[Offer]]]

  def updateOffer(tenant: String,
                  orgKey: String,
                  offerKey: String,
                  offer: Offer): Future[Offer]

  def addOffer(tenant: String, orgKey: String, offer: Offer): Future[Offer]

  def deleteOffer(tenant: String,
                  orgKey: String,
                  offerKey: String): Future[Either[AppErrorWithStatus, Offer]]

}
