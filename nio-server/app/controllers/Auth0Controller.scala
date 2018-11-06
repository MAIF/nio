package controllers

import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID.randomUUID

import akka.http.scaladsl.util.FastFuture
import configuration.{Auth0Config, Env}
import controllers.ErrorManager.AppErrorManagerResult
import org.apache.commons.lang3.StringUtils
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, Result}
import play.mvc.Http.{HeaderNames, MimeTypes}
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}

class Auth0Controller(env: Env, wsClient: WSClient, cc: ControllerComponents)(
    implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  private lazy val auth0Config: Auth0Config = env.config.filter.auth0

  lazy val audience: String = auth0Config.audience match {
    case aud if StringUtils.isEmpty(aud) =>
      s"https://${auth0Config.domain}/userinfo"
    case aud => aud
  }

  def login = Action {
    // Generate random state parameter
    object RandomUtil {
      private val random = new SecureRandom()

      def alphanumeric(nrChars: Int = 24): String = {
        new BigInteger(nrChars * 5, random).toString(32)
      }
    }
    val state = RandomUtil.alphanumeric()

    val id = randomUUID().toString

    Redirect(
      String.format(
        s"https://${auth0Config.domain}/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=openid profile&audience=%s&state=%s",
        auth0Config.clientId,
        auth0Config.callbackUrl,
        audience,
        state
      )).withSession("id" -> id)
  }

  def logout = Action {
    Redirect(
      String.format(
        s"https://${auth0Config.domain}/v2/logout?client_id=%s&returnTo=%s",
        auth0Config.clientId,
        env.config.baseUrl
      )).withNewSession
  }

  def callback(maybeCode: Option[String] = None,
               maybeState: Option[String] = None) = Action.async {
    implicit request =>
      Logger.info("auth0 callback")

      val maybeSessionId = request.session.get("id")

      (maybeCode, maybeSessionId) match {
        case (Some(code), Some(sessionId)) =>
          getToken(code, sessionId)
            .flatMap {
              case Right((idToken, accessToken)) =>
                val future: Future[Result] = getUser(accessToken).map { user =>
                  Logger.info(s"auth0 callback user $user")
                  val email = (user \ "name").as[String]

                  Redirect(routes.HomeController.indexNoTenant())
                    .withSession(
                      "idToken" -> idToken,
                      "accessToken" -> accessToken,
                      "email" -> email
                    )
                }
                future
              case Left(e) =>
                FastFuture.successful(e.unauthorized())
            }

        case _ =>
          FastFuture.successful(BadRequest)
      }
  }

  private def getToken(
      code: String,
      sessionId: String): Future[Either[AppErrors, (String, String)]] = {

    wsClient
      .url(s"https://${auth0Config.domain}/oauth/token")
      .addHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(
        Json.obj(
          "client_id" -> auth0Config.clientId,
          "client_secret" -> auth0Config.clientSecret,
          "redirect_uri" -> auth0Config.callbackUrl,
          "code" -> code,
          "grant_type" -> "authorization_code",
          "audience" -> audience
        )
      )
      .map { response =>
        val maybeAccess: Option[(String, String)] = for {
          idToken <- (response.json \ "id_token").asOpt[String]
          accessToken <- (response.json \ "access_token").asOpt[String]
        } yield (idToken, accessToken)

        maybeAccess match {
          case Some(access) =>
            Right(access)
          case None =>
            Left(AppErrors(Seq(ErrorMessage("tokens.not.send"))))
        }
      }
  }

  private def getUser(accessToken: String): Future[JsValue] = {
    wsClient
      .url(s"https://${auth0Config.domain}/userinfo")
      .withQueryStringParameters("access_token" -> accessToken)
      .get()
      .map(_.json)
  }

}
