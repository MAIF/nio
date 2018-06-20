package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import javax.inject.Inject
import auth.AuthAction
import db.UserMongoDataStore
import models.PagedUsers
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}

import scala.concurrent.ExecutionContext

class UserController @Inject()(val AuthAction: AuthAction,
                               val cc: ControllerComponents,
                               val ds: UserMongoDataStore)(
    implicit val ec: ExecutionContext,
    system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val materializer = ActorMaterializer()(system)

  def listByOrganisation(tenant: String,
                         orgKey: String,
                         page: Int = 0,
                         pageSize: Int = 10,
                         maybeUserId: Option[String]) = AuthAction.async {
    implicit req =>
      ds.findAllByOrgKey(tenant, orgKey, page, pageSize, maybeUserId).map {
        case (users, count) =>
          val pagedUsers = PagedUsers(page, pageSize, count, users)

          renderMethod(pagedUsers)
      }
  }

  def listAll(tenant: String,
              page: Int = 0,
              pageSize: Int = 10,
              maybeUserId: Option[String]) =
    AuthAction.async { implicit req =>
      ds.findAll(tenant, page, pageSize, maybeUserId).map {
        case (users, count) =>
          val pagedUsers = PagedUsers(page, pageSize, count, users)

          renderMethod(pagedUsers)
      }
    }

  def download(tenant: String) = AuthAction.async { implicit req =>
    ds.streamAll(tenant).map { source =>
      val src = source
        .map(Json.stringify)
        .intersperse("", "\n", "\n")
        .map(ByteString.apply)
      Result(
        header = ResponseHeader(OK,
                                Map(CONTENT_DISPOSITION -> "attachment",
                                    "filename" -> "users.ndjson")),
        body = HttpEntity.Streamed(src, None, Some("application/json"))
      )
    }
  }
}
