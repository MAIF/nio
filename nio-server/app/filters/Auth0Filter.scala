package filters

import java.util.Base64

import akka.stream.Materializer
import auth.AuthInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import configuration.Env
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class Auth0Filter(env: Env, authInfoMock: AuthInfoMock)(
    implicit ec: ExecutionContext,
    val mat: Materializer)
    extends Filter {
  private val logger = Logger("filter")

  private lazy val auth0Config = env.config.filter.auth0

  override def apply(next: RequestHeader => Future[Result])(
      requestHeader: RequestHeader): Future[Result] = {

    val maybeSessionEmail = requestHeader.session.get("email")
    val maybeSessionAccessToken = requestHeader.session.get("idToken")

    val maybeHeaderAccessToken = requestHeader.headers.get("Authorization")

    val allowedPaths: Seq[String] = auth0Config.allowedPaths
    Try((env.env,
         maybeSessionEmail,
         maybeSessionAccessToken,
         maybeHeaderAccessToken) match {
      case (test, _, _, _) if Seq("test").contains(test) =>
        next(
          requestHeader
            .addAttr(FilterAttributes.Email, authInfoMock.getAuthInfo.sub)
            .addAttr(FilterAttributes.AuthInfo, Some(authInfoMock.getAuthInfo))
        ).map { result =>
          Logger.debug(
            s"Request => ${requestHeader.method} ${requestHeader.uri} returned ${result.header.status}"
          )
          result
        }

      case (_, _, _, Some(headerAccessToken)) =>
        val tryDecode = Try {
          val token = headerAccessToken.replace("Bearer ", "")

          val verifier = JWT
            .require(Algorithm.HMAC256(auth0Config.signInSecret))
            .build()

          val decoded = verifier.verify(token)

          import scala.collection.JavaConverters._
          val claims = decoded.getClaims.asScala

          val email: String = claims.get("sub").map(_.asString()).get

          next(
            requestHeader.addAttr(FilterAttributes.Email, email)
              addAttr (FilterAttributes.AuthInfo, Some(
                AuthInfo(
                  sub = email,
                  isAdmin = true,
                  metadatas = None,
                  offerRestrictionPatterns = None
                ))))
            .map {
              result =>
                logger.debug(
                  s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                    .map(h => s"""   "${h._1}": "${h._2}"\n""")
                    .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
                )
                result
            }

        } recoverWith {
          case e =>
            Success(
              Future.successful(
                Results
                  .InternalServerError(
                    Json.obj("error" -> "what !!!", "m" -> e.getMessage)
                  )
              )
            )
        }
        tryDecode.get

      case (_, Some(email), Some(token), _) =>
        Logger.info(s"idToken = $token")
//        Logger.info(s"idToken = ${Base64.getDecoder.decode(token)}")
        // TODO : récupérer les informations en session
        next(
          requestHeader.addAttr(FilterAttributes.Email, email)
            addAttr (FilterAttributes.AuthInfo, Some(
              AuthInfo(
                sub = email,
                isAdmin = true,
                metadatas = None,
                offerRestrictionPatterns = None
              ))))
          .map {
            result =>
              logger.debug(
                s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                  .map(h => s"""   "${h._1}": "${h._2}"\n""")
                  .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
              )
              result
          }
      case (_, _, _, _)
          if allowedPaths.exists(path => requestHeader.path.matches(path)) =>
        next(requestHeader.addAttr(FilterAttributes.AuthInfo, None)).map {
          result =>
            logger.debug(
              s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                .map(h => s"""   "${h._1}": "${h._2}"\n""")
                .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
            )
            result
        }
      case _ =>
        Future.successful(
          Results
            .Unauthorized(
              Json.obj("error" -> "Bad env !!!")
            )
        )
    }).recoverWith {
      case e =>
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

}
