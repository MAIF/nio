package db

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import models.ExtractionTask
import models.ExtractionTaskStatus.ExtractionTaskStatus
import play.api.libs.json._

import scala.concurrent.Future

trait ExtractionTaskDataStore {

  def init(tenant: String): Future[Unit]

  def ensureIndices(key: String): Future[Unit]

  def insert(tenant: String, extractionTask: ExtractionTask): Future[Boolean]

  def updateById(tenant: String,
                 id: String,
                 extractionTask: ExtractionTask): Future[Boolean]

  def findById(tenant: String, id: String): Future[Option[ExtractionTask]]

  def findAll(tenant: String,
              page: Int,
              pageSize: Int): Future[(Seq[ExtractionTask], Int)]

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ExtractionTask], Int)]

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int): Future[(Seq[ExtractionTask], Int)]

  def streamAllByState(tenant: String, status: ExtractionTaskStatus)(
      implicit m: Materializer): Future[Source[JsValue, Any]]

  def deleteExtractionTaskByTenant(tenant: String): Future[Boolean]
}
