package controllers

import java.io.StringWriter

import akka.actor.ActorSystem
import auth.AuthAction
import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import configuration.Env
import db.TenantMongoDataStore
import messaging.KafkaSettings
import org.apache.kafka.clients.consumer.Consumer
import play.api.mvc.ControllerComponents

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

import ErrorManager.ErrorManagerResult
class MetricsController(
    val AuthAction: AuthAction,
    tenantStore: TenantMongoDataStore,
    env: Env,
    actorSystem: ActorSystem,
    metricRegistry: MetricRegistry,
    val cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  def metrics = AuthAction { req =>
    Ok(toJson())
  }

  val mapper = new ObjectMapper()

  def toJson(): String = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, metricRegistry)
    stringWriter.toString
  }

  def healthCheck() = AuthAction.async { implicit req =>
    req.headers.get(env.healthCheckConfig.header) match {
      case Some(secret) if secret == env.healthCheckConfig.secret =>
        tenantStore
          .findAll()
          .map { _ =>
            val kafka = env.config.kafka

            val kafkaConsumer: Consumer[Array[Byte], String] = KafkaSettings
              .consumerSettings(actorSystem, kafka)
              .createKafkaConsumer()

            kafkaConsumer
              .partitionsFor(kafka.topic)
              .asScala

            kafkaConsumer.close()

            Ok
          }
      case None =>
        Future.successful("error.missing.secret".unauthorized())
    }
  }

}
