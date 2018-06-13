package controllers

import java.nio.file.Files

import akka.actor.ActorSystem
import auth.AuthActionWithEmail
import configuration.Env
import db.TenantMongoDataStore
import javax.inject.Inject
import messaging.KafkaSettings
import org.apache.kafka.clients.consumer.Consumer
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class HomeController @Inject()(val AuthAction: AuthActionWithEmail,
                               val cc: ControllerComponents,
                               val tenantStore: TenantMongoDataStore,
                               val env: Env,
                               val actorSystem: ActorSystem,
                               implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

  lazy val swaggerContent: String = Files
    .readAllLines(env.environment.getFile("conf/swagger/swagger.json").toPath)
    .asScala
    .mkString("\n")
    .replace("$CLIENT_ID",
             env.config.filter.otoroshi.headerGatewayHeaderClientId)
    .replace("$CLIENT_SECRET",
             env.config.filter.otoroshi.headerGatewayHeaderClientSecret)

  def index(tenant: String) = AuthAction.async { req =>
    if (req.authInfo.isAdmin) {
      tenantStore.findByKey(tenant).map {
        case Some(_) => Ok(views.html.index(env, tenant, req.email))
        case None    => NotFound("error.tenant.not.found")
      }
    } else {
      Future.successful(Forbidden("error.forbidden.backoffice.access"))
    }
  }

  def indexNoTenant = AuthAction { req =>
    if (req.authInfo.isAdmin) {
      Ok(views.html.indexNoTenant(env, req.email))
    } else {
      Forbidden("error.forbidden.backoffice.access")
    }
  }

  def indexOther(tenant: String) = index(tenant)

  def otherRoutes(tenant: String, route: String) = AuthAction.async { req =>
    if (req.authInfo.isAdmin) {
      tenantStore.findByKey(tenant).map {
        case Some(_) => Ok(views.html.index(env, tenant, req.email))
        case None    => NotFound("error.tenant.not.found")
      }
    } else {
      Future.successful(Forbidden("error.forbidden.backoffice.access"))
    }
  }

  def swagger() = AuthAction { req =>
    Ok(swaggerContent).as("application/json")
  }

  def healthCheck() = AuthAction.async { req =>
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
  }
}
