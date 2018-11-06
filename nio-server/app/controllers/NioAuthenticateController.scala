package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import configuration.{DefaultFilterConfig, Env}
import controllers.ErrorManager.AppErrorManagerResult
import db.NioAccountMongoDataStore
import models.{Auth, NioAccount}
import play.api.mvc.{ControllerComponents, Cookie}
import utils.Sha

import scala.concurrent.ExecutionContext

class NioAuthenticateController(
    env: Env,
    cc: ControllerComponents,
    nioAccountMongoDataStore: NioAccountMongoDataStore,
    actorSystem: ActorSystem)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Auth] = Auth

  val config: DefaultFilterConfig = env.config.filter.default

  lazy val cookieName: String = config.cookieClaim
  lazy val algorithm: Algorithm = Algorithm.HMAC512(config.sharedKey)

  def login = Action(bodyParser).async { implicit req =>
    req.body.read[Auth] match {
      case Left(e) =>
        FastFuture.successful(e.forbidden())
      case Right(auth) =>
        nioAccountMongoDataStore.findByEmail(auth.email).map {
          case Some(authStored)
              if authStored.password == Sha.hexSha512(auth.password) =>
            Ok.withCookies(
              Cookie(name = cookieName, value = buildCookies(authStored)))
          case _ =>
            Forbidden
        }
    }
  }

  private def buildCookies(nioAccount: NioAccount): String = {
    JWT
      .create()
      .withIssuer(config.issuer)
      .withClaim("email", nioAccount.email)
      .withClaim("isAdmin", nioAccount.isAdmin)
      .withClaim(
        "offerRestrictionPatterns",
        nioAccount.offerRestrictionPatterns.getOrElse(Seq.empty).mkString(","))
      .sign(algorithm)
  }

  def logout = Action { _ =>
    Redirect(s"${env.config.baseUrl}")
      .withCookies(Cookie(name = cookieName, value = "", maxAge = Some(0)))
  }

}
