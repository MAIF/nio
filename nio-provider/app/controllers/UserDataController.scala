package controllers

import java.io.FileInputStream

import actor.EventActor
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import auth.AuthActionWithEmail
import configuration.Env
import play.Logger
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import service.NioService

import scala.concurrent.{ExecutionContext, Future}

class UserDataController(val AuthAction: AuthActionWithEmail,
                         val cc: ControllerComponents,
                         val env: Env,
                         nioService: NioService,
                         implicit val actorSystem: ActorSystem,
                         implicit val ec: ExecutionContext)
    extends AbstractController(cc) {
  implicit val mat = ActorMaterializer()(actorSystem)

  def listen() = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      EventActor.props(out)
    }
  }

  def uploadFile(tenant: String, orgKey: String, userId: String, name: String) =
    AuthAction.async(parse.multipartFormData) { implicit req =>
      Logger.info(s"upload file $name")
      val src: Source[ByteString, _] =
        StreamConverters.fromInputStream(() => {
          new FileInputStream(req.body.files.head.ref)
        })

      nioService
        .uploadFileToNio(tenant,
                         orgKey,
                         userId,
                         name,
                         src,
                         req.headers.get("Content-Type"))
        .map(Ok(_))
    }

}
