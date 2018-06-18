package controllers

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, FileIO, Source}
import akka.util.ByteString
import javax.inject.Inject
import auth.AuthAction
import db.UserMongoDataStore
import models.PagedUsers
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
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

  def storeAsTmp(tenant: String) = AuthAction.async { implicit req =>
    val tempFi = new File(s"$tenant-cs-output.gzip")
    tempFi.createNewFile()
    println("-----> " + tempFi.getAbsolutePath)

    import reactivemongo.akkastream.cursorProducer
    import play.modules.reactivemongo.json.ImplicitBSONHandlers._

    ds.storedCollection(tenant).flatMap { col =>
      col
        .find(Json.obj())
        .cursor[JsValue]()
        .documentSource()
        .map(d => ByteString(s"${d.toString()}\n"))
        .via(Compression.gzip)
        .runWith(
          FileIO.toPath(tempFi.toPath)
        )
        .map { res =>
          Logger.info("-----> " + res)
          Ok(tempFi.getAbsolutePath)
        }
    }
  //Future.successful(Ok("asdf"))
//    ds.streamAll(tenant).map { source =>
//      val src = source
//        .map(Json.stringify)
//        .intersperse("", "\n", "\n")
//        .map(ByteString.apply)
//      Result(
//        header = ResponseHeader(OK,
//          Map(CONTENT_DISPOSITION -> "attachment",
//            "filename" -> "users.ndjson")),
//        body = HttpEntity.Streamed(src, None, Some("application/json"))
//      )
//    }
  }

}
