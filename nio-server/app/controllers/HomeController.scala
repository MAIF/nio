package controllers

import akka.actor.ActorSystem
import configuration.Env
import javax.inject.Inject
import auth.AuthActionWithEmail
import db.TenantMongoDataStore
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class HomeController @Inject()(val AuthAction: AuthActionWithEmail,
                               val cc: ControllerComponents,
                               val tenantStore: TenantMongoDataStore,
                               val env: Env,
                               val actorSystem: ActorSystem,
                               implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

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

  def indexOther(tenant: String) = index(tenant)

  def otherRoutes(tenant: String, route: String) = AuthAction { req =>
    if (req.authInfo.isAdmin) {
      Ok(views.html.index(env, tenant, req.email))
    } else {
      Forbidden("error.forbidden.backoffice.access")
    }
  }
}
