package filters

import java.util.Base64

import akka.stream.Materializer
import auth.AuthInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.common.base.Charsets
import configuration.Env
import db.ApiKeyMongoDataStore
import play.api.Logger
import utils.NioLogger
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class Auth0Filter(env: Env, authInfoMock: AuthInfoMock, apiKeyMongoDataStore: ApiKeyMongoDataStore)(implicit
    ec: ExecutionContext,
    val mat: Materializer
) extends Filter {
  private val logger = Logger("filter")

  private lazy val auth0Config = env.config.filter.auth0
  private val decoder          = Base64.getDecoder

  override def apply(next: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val maybeSessionEmail       = requestHeader.session.get("email")
    val maybeSessionAccessToken = requestHeader.session.get("idToken")

    val maybeClientId     =
      requestHeader.headers.get(auth0Config.apiKeys.headerClientId)
    val maybeClientSecret =
      requestHeader.headers.get(auth0Config.apiKeys.headerClientSecret)

    val maybeAuthorization        = requestHeader.headers
      .get("Authorization")
      .map(_.replace("Basic ", ""))
      .map(a => new String(decoder.decode(a), Charsets.UTF_8))
      .filter(_.contains(":"))
      .map(_.split(":").toList)
      .collect { case user :: password :: Nil =>
        (user, password)
      }
    val allowedPaths: Seq[String] = auth0Config.allowedPaths
    Try(
      (
        env.env,
        maybeSessionEmail,
        maybeSessionAccessToken,
        maybeAuthorization,
        maybeClientId,
        maybeClientSecret
      ) match {
        case (test, _, _, _, _, _) if Seq("test").contains(test) =>
          next(
            requestHeader
              .addAttr(FilterAttributes.Email, authInfoMock.getAuthInfo.sub)
              .addAttr(FilterAttributes.AuthInfo, Some(authInfoMock.getAuthInfo))
          ).map { result =>
            NioLogger.debug(
              s"Request => ${requestHeader.method} ${requestHeader.uri} returned ${result.header.status}"
            )
            result
          }

        case (_, _, _, _, Some(clientId), Some(clientSecret)) =>
          validateByApiKey(next, requestHeader, clientId, clientSecret)

        case (_, _, _, Some((clientId, clientSecret)), _, _) =>
          validateByApiKey(next, requestHeader, clientId, clientSecret)

        case (_, Some(email), Some(token), _, _, _)                                              =>
          // TODO : récupérer les informations en session
          next(
            requestHeader.addAttr(FilterAttributes.Email, email)
            addAttr (FilterAttributes.AuthInfo, Some(
              AuthInfo(
                sub = email,
                isAdmin = true,
                metadatas = None,
                offerRestrictionPatterns = None
              )
            ))
          )
            .map { result =>
              NioLogger.debug(
                s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                  .map(h => s"""   "${h._1}": "${h._2}"\n""")
                  .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
              )
              result
            }
        case (_, _, _, _, _, _) if allowedPaths.exists(path => requestHeader.path.matches(path)) =>
          next(requestHeader.addAttr(FilterAttributes.AuthInfo, None)).map { result =>
            NioLogger.debug(
              s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                .map(h => s"""   "${h._1}": "${h._2}"\n""")
                .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
            )
            result
          }
        case _                                                                                   =>
          Future.successful(
            Results
              .Unauthorized(
                Json.obj("error" -> "Bad env !!!")
              )
          )
      }
    ).recoverWith { case e =>
      Success(
        Future.successful(
          Results
            .InternalServerError(
              Json.obj("error" -> e.getMessage)
            )
        )
      )
    }.get
  }

  private def validateByApiKey(
      next: RequestHeader => Future[Result],
      requestHeader: RequestHeader,
      clientId: String,
      clientSecret: String
  ): Future[Result] =
    apiKeyMongoDataStore.findByClientId(clientId).flatMap {
      case Some(apiKey) if apiKey.clientSecret == clientSecret =>
        next(
          requestHeader
            .addAttr(FilterAttributes.AuthInfo, Some(apiKey.toAuthInfo()))
        )
      case _                                                   =>
        Future.successful(
          Results.Unauthorized
        )
    }
}
