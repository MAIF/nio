package s3

import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.model.lifecycle.{
  LifecycleAndOperator,
  LifecycleFilter,
  LifecycleTagPredicate
}
import db.{ExtractionTaskMongoDataStore, TenantMongoDataStore}
import javax.inject.{Inject, Singleton}
import models.{ExtractionTask, ExtractionTaskStatus}
import org.joda.time.{DateTime, DateTimeZone, Days}
import play.api.Logger

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class S3 @Inject()(val conf: S3Configuration,
                   val system: ActorSystem,
                   val tenantStore: TenantMongoDataStore)(
    implicit ec: ExecutionContext,
    store: ExtractionTaskMongoDataStore) {

  lazy val client = {
    val opts = new ClientConfiguration()
    if (!conf.v4auth) {
      opts.setSignerOverride("S3SignerType")
    }

    AmazonS3ClientBuilder
      .standard()
      .withClientConfiguration(opts)
      .withEndpointConfiguration(
        new AwsClientBuilder.EndpointConfiguration(
          conf.endpoint,
          conf.region
        ))
      .withPathStyleAccessEnabled(true)
      .withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(conf.access, conf.secret)))
      .build()
  }

  def listFilesInNioBucket = {
    val listing = client.listObjects(conf.bucketName)
    listing.getObjectSummaries.asScala
  }

  def applyExpirationConfig = {
    println("Applying expiration configuration to bucket ")
    val configuration = client.getBucketLifecycleConfiguration(conf.bucketName)

    // Add a new rule
    configuration.getRules.add(
      new BucketLifecycleConfiguration.Rule()
        .withId("Expiration Rule")
        .withFilter(new LifecycleFilter(
          new LifecycleAndOperator(util.Arrays.asList(new LifecycleTagPredicate(
            new Tag("expire_after", conf.expirationInDays.toString))))))
        .withExpirationInDays(conf.expirationInDays)
        .withStatus(BucketLifecycleConfiguration.ENABLED))
    // Save configuration
    client.setBucketLifecycleConfiguration(conf.bucketName, configuration)
  }

  lazy val expirationTag = new ObjectTagging(
    java.util.Arrays
      .asList(new Tag("expire_after", conf.expirationInDays.toString)))

  // very simplistic cleaner of expired files
  def startExpiredFilesCleaner = {
    implicit val mat = ActorMaterializer()(system)
    case object Tick
    Source
      .tick(0.seconds, conf.expirationCheckInSeconds.seconds, Tick)
      .runForeach { _ =>
        // For all tenants
        for {
          tenants <- tenantStore.findAll()
          handledTenants <- Future.sequence(
            tenants.map { tenant =>
              store
                .streamAllByState(tenant.key, ExtractionTaskStatus.Done)
                .map {
                  _.runForeach {
                    taskJson =>
                      try {
                        val task = ExtractionTask.fmt.reads(taskJson).get
                        val days = Days
                          .daysBetween(DateTime.now(DateTimeZone.UTC),
                                       task.lastUpdate)
                          .getDays
                        if (days >= conf.expirationInDays) {
                          val keys = task.states.flatMap { state =>
                            state.files.map { f =>
                              new DeleteObjectsRequest.KeyVersion(
                                s"$tenant/${task.orgKey}/${task.userId}/${task._id}/${state.appId}/${f.name}"
                              )
                            }
                          }.toSeq
                          val deleteReq =
                            new DeleteObjectsRequest(conf.bucketName)
                              .withKeys(
                                scala.collection.JavaConverters
                                  .seqAsJavaList(keys)
                              )
                          client.deleteObjects(deleteReq)
                          task.expire(tenant.key)
                        }
                      } catch {
                        case e: Exception =>
                          Logger.error(
                            s"Unable to delete expired files due to: ${e.getMessage}")
                      }
                  }
                }
            }
          )
          handledTenantsResults <- Future.sequence(handledTenants)
        } yield {
          //Logger.info("Checked for expired files  " + handledTenantsResults)
        }
      }
  }

}
