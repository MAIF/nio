package controllers

import java.nio.file.Files

import akka.actor.ActorSystem
import auth.AuthActionWithEmail
import configuration.Env
import db.TenantMongoDataStore
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

import ErrorManager.ErrorManagerResult

class HomeController(val AuthAction: AuthActionWithEmail,
                     val cc: ControllerComponents,
                     val tenantStore: TenantMongoDataStore,
                     val env: Env,
                     val actorSystem: ActorSystem,
                     implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {

  lazy val swaggerContent: String = Files
    .readAllLines(env.environment.getFile("conf/swagger/swagger.json").toPath)
    .asScala
    .mkString("\n")
    .replace("$CLIENT_ID",
             env.config.filter.otoroshi.headerGatewayHeaderClientId)
    .replace("$CLIENT_SECRET",
             env.config.filter.otoroshi.headerGatewayHeaderClientSecret)

  def index(tenant: String) = AuthAction.async { implicit req =>
    if (req.authInfo.isAdmin) {
      tenantStore.findByKey(tenant).map {
        case Some(_) => Ok(views.html.index(env, tenant, req.email))
        case None    => "error.tenant.not.found".notFound()
      }
    } else {
      Future.successful("error.forbidden.backoffice.access".forbidden())
    }
  }

  def indexNoTenant = AuthAction { implicit req =>
    if (req.authInfo.isAdmin) {
      Ok(views.html.indexNoTenant(env, req.email))
    } else {
      "error.forbidden.backoffice.access".forbidden()
    }
  }

  def indexOther(tenant: String) = index(tenant)

  def otherRoutes(tenant: String, route: String) = AuthAction.async {
    implicit req =>
      if (req.authInfo.isAdmin) {
        tenantStore.findByKey(tenant).map {
          case Some(_) => Ok(views.html.index(env, tenant, req.email))
          case None    => "error.tenant.not.found".notFound()
        }
      } else {
        Future.successful("error.forbidden.backoffice.access".forbidden())
      }
  }

  def swagger() = AuthAction { req =>
    Ok(swaggerContent).as("application/json")
  }
}
