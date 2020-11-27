package filters

import java.util.Base64

import akka.stream.Materializer
import auth.AuthInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.common.base.Charsets
import configuration.{DefaultFilterConfig, Env}
import db.{ApiKeyMongoDataStore, NioAccountMongoDataStore}
import play.api.Logger
import utils.NioLogger
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class NioDefaultFilter(
    env: Env,
    authInfoMock: AuthInfoMock,
    userAccountMongoDataStore: NioAccountMongoDataStore,
    apiKeyMongoDataStore: ApiKeyMongoDataStore
)(implicit ec: ExecutionContext, val mat: Materializer)
    extends Filter {

  val config: DefaultFilterConfig = env.config.filter.default
  private val decoder             = Base64.getDecoder
  private val logger              = Logger("filter")

  override def apply(next: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val maybeClaim = Try(requestHeader.cookies.get(config.cookieClaim).get.value).toOption

    val maybeAuthorization = requestHeader.headers
      .get("Authorization")
      .map(_.replace("Basic ", ""))
      .map(a => new String(decoder.decode(a), Charsets.UTF_8))
      .filter(_.contains(":"))
      .map(_.split(":").toList)
      .collect { case user :: password :: Nil =>
        (user, password)
      }

    val maybeClientId     = requestHeader.headers.get(config.apiKeys.headerClientId)
    val maybeClientSecret =
      requestHeader.headers.get(config.apiKeys.headerClientSecret)

    val allowedPaths: Seq[String] = config.allowedPaths

    Try((env.env, maybeClaim, maybeAuthorization, maybeClientId, maybeClientSecret) match {
      case (test, _, _, _, _) if Seq("test").contains(test)                                           =>
//      case (devOrTest, _, _, _, _) if Seq("test", "dev").contains(devOrTest) =>
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
      //      case ("prod", _, _, Some(clientId), Some(clientSecret)) =>
      case (_, _, _, Some(clientId), Some(clientSecret))                                              =>
        validateByApiKey(next, requestHeader, clientId, clientSecret)
      //      case ("prod", _, Some((clientId, clientSecret)), _, _) =>
      case (_, _, Some((clientId, clientSecret)), _, _)                                               =>
        validateByApiKey(next, requestHeader, clientId, clientSecret)
      //      case ("prod", Some(claim), _, _, _) if allowedPaths.exists(path => requestHeader.path.matches(path)) =>
      case (_, Some(claim), _, _, _) if allowedPaths.exists(path => requestHeader.path.matches(path)) =>
        val tryDecode = Try {
          val algorithm           = Algorithm.HMAC512(config.sharedKey)
          val verifier            =
            JWT.require(algorithm).withIssuer(config.issuer).build()
          val decoded: DecodedJWT = verifier.verify(claim)
          next(
            requestHeader.addAttr(FilterAttributes.Email, decodeJWTTokenEmail(decoded))
            addAttr (FilterAttributes.AuthInfo,
            decodeJWTToken(decoded))
          ).map { result =>
            logger.debug(
              s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                .map(h => s"""   "${h._1}": "${h._2}"\n""")
                .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
            )
            result
          }
        } recoverWith { case e =>
          Success(
            Future.successful(
              Results
                .Unauthorized(
                  Json.obj("error" -> "Claim error !!!", "m" -> e.getMessage)
                )
            )
          )
        }
        tryDecode.get
      //      case ("prod", Some(claim), _, _, _) =>
      case (_, Some(claim), _, _, _)                                                                  =>
        val tryDecode = Try {
          val algorithm           = Algorithm.HMAC512(config.sharedKey)
          val verifier            =
            JWT.require(algorithm).withIssuer(config.issuer).build()
          val decoded: DecodedJWT = verifier.verify(claim)
          next(
            requestHeader.addAttr(FilterAttributes.Email, decodeJWTTokenEmail(decoded))
            addAttr (FilterAttributes.AuthInfo,
            decodeJWTToken(decoded))
          ).map { result =>
            logger.debug(
              s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
                .map(h => s"""   "${h._1}": "${h._2}"\n""")
                .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
            )
            result
          }
        } recoverWith { case e =>
          Success(
            Future.successful(
              Results
                .Unauthorized(
                  Json.obj("error" -> "Claim error !!!", "m" -> e.getMessage)
                )
            )
          )
        }
        tryDecode.get
      //      case ("prod", _, _, _, _) if allowedPaths.exists(path => requestHeader.path.matches(path)) =>
      case (_, _, _, _, _) if allowedPaths.exists(path => requestHeader.path.matches(path))           =>
        next(requestHeader.addAttr(FilterAttributes.AuthInfo, None)).map { result =>
          logger.debug(
            s"Request claim with exclusion => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
              .map(h => s"""   "${h._1}": "${h._2}"\n""")
              .mkString(",")} returned ${result.header.status} hasBody ${requestHeader.hasBody}"
          )
          result
        }
      case _                                                                                          =>
        Future.successful(
          Results
            .Unauthorized(
              Json.obj("error" -> "Bad env !!!")
            )
        )
    }).recoverWith { case e =>
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

  private def decodeJWTToken(jwt: DecodedJWT): Option[AuthInfo] = {
    import scala.collection.JavaConverters._
    val claims = jwt.getClaims.asScala

    for {
      email                        <- claims.get("email").map(_.asString())
      isAdmin                      <- claims.get("isAdmin").map(_.asBoolean())
      metadatas                     = claims
                                        .filterKeys(header => header.startsWith("metadata"))
                                        .map(header => (header._1.replaceFirst("metadata.", ""), header._2.asString()))
                                        .toSeq
      maybeOfferRestrictionPatterns = claims
                                        .get("offerRestrictionPatterns")
                                        .map(_.asString())      // Claim to String
                                        .map(s => s.split(",")) // String to array[String]
                                        .map(_.map(_.trim))     // Trim all string into array
                                        .map(_.toSeq)
    } yield AuthInfo(email, isAdmin, Some(metadatas), maybeOfferRestrictionPatterns)
  }

  private def decodeJWTTokenEmail(jwt: DecodedJWT): String = {
    import scala.collection.JavaConverters._
    val claims = jwt.getClaims.asScala

    claims.get("email").map(_.asString()).get
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
