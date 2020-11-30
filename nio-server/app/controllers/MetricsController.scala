package controllers

import akka.actor.ActorSystem
import auth.{AuthAction, SecuredAction, SecuredAuthContext}
import com.fasterxml.jackson.databind.ObjectMapper
import configuration.Env
import controllers.ErrorManager.ErrorManagerResult
import db.TenantMongoDataStore
import messaging.KafkaSettings
import org.apache.kafka.clients.consumer.Consumer
import play.api.mvc.{ActionBuilder, AnyContent, ControllerComponents}

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class MetricsController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    tenantStore: TenantMongoDataStore,
    env: Env,
    actorSystem: ActorSystem,
    val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  val mapper = new ObjectMapper()

  val blockingExecutionContext =
    actorSystem.dispatchers.lookup("blocking-dispatcher")

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

            Future {
              kafkaConsumer
                .partitionsFor(kafka.topic)
                .asScala
            }(blockingExecutionContext)

            kafkaConsumer.close()

            Ok
          }
      case None                                                   =>
        Future.successful("error.missing.secret".unauthorized())
    }
  }

}
