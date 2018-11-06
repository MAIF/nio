package filters

import akka.stream.Materializer
import auth.AuthInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
    Logger.info(s"idToken = ${requestHeader.session.get("idToken")}")

    val allowedPaths: Seq[String] = auth0Config.allowedPaths
    Try((env.env, maybeSessionEmail) match {
      case (test, _) if Seq("test").contains(test) =>
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

      case (_, Some(email)) =>
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
      case (_, _)
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
