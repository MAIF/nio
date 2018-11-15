package filters

import akka.stream.Materializer
import auth.AuthInfo
import com.auth0.jwt._
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import configuration._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util._

class OtoroshiFilter(env: Env, authInfoMock: AuthInfoMock)(
    implicit ec: ExecutionContext,
    val mat: Materializer)
    extends Filter {

  val config: OtoroshiFilterConfig = env.config.filter.otoroshi

  private val logger = Logger("filter")

  override def apply(nextFilter: RequestHeader => Future[Result])(
      requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    val maybeReqId = requestHeader.headers.get(config.headerRequestId)
    val maybeState = requestHeader.headers.get(config.headerGatewayState)
    val maybeClaim: Option[String] =
      requestHeader.headers.get(config.headerClaim)
    val excludeCheckingPath =
      Seq("/docs", "/assets", "/_healthCheck", "/_download")

    val t = Try(env.env match {
      case devOrTest if devOrTest == "dev" || devOrTest == "test" =>
        nextFilter(
          requestHeader
            .addAttr(FilterAttributes.Email, authInfoMock.getAuthInfo.sub)
            .addAttr(FilterAttributes.AuthInfo, Some(authInfoMock.getAuthInfo))
        ).map {
          result =>
            val requestTime = System.currentTimeMillis - startTime
            logger.debug(
              s"Request => ${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}"
            )
            result.withHeaders(
              config.headerGatewayStateResp -> maybeState.getOrElse("--")
            )
        }

      case "prod" if maybeClaim.isEmpty && maybeState.isEmpty =>
        Future.successful(
          Results.Unauthorized(
            Json.obj("error" -> "Bad request !!!")
          )
        )
      case "prod" if maybeClaim.isEmpty =>
        Future.successful(
          Results
            .Unauthorized(
              Json.obj("error" -> "Bad claim !!!")
            )
            .withHeaders(
              config.headerGatewayStateResp -> maybeState.getOrElse("--")
            )
        )
      case "prod" =>
        val tryDecode = Try {

          val algorithm = Algorithm.HMAC512(config.sharedKey)
          val verifier = JWT
            .require(algorithm)
            .withIssuer(config.issuer)
            .acceptLeeway(5000)
            .build()
          val decoded = verifier.verify(maybeClaim.get)

          import scala.collection.JavaConverters._
          val claims = decoded.getClaims.asScala

          val maybeSub = claims.get("sub").map(_.asString())
          val isAdmin = claims
            .get("nio_admin")
            .map(_.asString)
            .flatMap(str => Try(str.toBoolean).toOption)
            .getOrElse(false)

          val path: String = requestHeader.path

          maybeSub match {
            // with a user admin
            case Some(sub) if sub.startsWith("pa:") && isAdmin =>
              validateOtoroshiHeaders(claims,
                                      maybeReqId,
                                      maybeState,
                                      startTime,
                                      nextFilter)(requestHeader)
            // with api
            case Some(sub) if sub.startsWith("apikey:") =>
              validateOtoroshiHeaders(claims,
                                      maybeReqId,
                                      maybeState,
                                      startTime,
                                      nextFilter)(requestHeader)

            // with path to ignore
            case _
                if excludeCheckingPath.exists(
                  pathPrefix => path.startsWith(pathPrefix)) =>
              validateOtoroshiHeaders(claims,
                                      maybeReqId,
                                      maybeState,
                                      startTime,
                                      nextFilter)(requestHeader)

            // fail other case
            case _ =>
              Future.successful(
                Results
                  .Unauthorized(
                    Json.obj("error" -> "You are not admin !!!")
                  )
                  .withHeaders(
                    config.headerGatewayStateResp -> maybeState.getOrElse("--")
                  )
              )
          }

        } recoverWith {
          case e =>
            Success(
              Future.successful(
                Results
                  .InternalServerError(
                    Json.obj("error" -> "what !!!", "m" -> e.getMessage)
                  )
                  .withHeaders(
                    config.headerGatewayStateResp -> maybeState.getOrElse("--")
                  )
              )
            )
        }
        tryDecode.get

      case _ =>
        Future.successful(
          Results
            .InternalServerError(
              Json.obj("error" -> "Bad env !!!")
            )
            .withHeaders(
              config.headerGatewayStateResp -> maybeState.getOrElse("--")
            )
        )
    }) recoverWith {
      case e =>
        Success(
          Future.successful(
            Results
              .InternalServerError(
                Json.obj("error" -> e.getMessage)
              )
              .withHeaders(
                config.headerGatewayStateResp -> maybeState.getOrElse("--")
              )
          )
        )
    }
    val result: Future[Result] = t.get
    result.onComplete {
      case Success(resp) =>
        logger.debug(
          s" ${requestHeader.method} ${requestHeader.uri} resp : $resp")
      case Failure(e) =>
        logger.error(
          s"Error for request ${requestHeader.method} ${requestHeader.uri}",
          e)
        logger.error(
          s"Error for request ${requestHeader.method} ${requestHeader.uri}",
          e.getCause)
    }
    result
  }

  def validateOtoroshiHeaders(claims: mutable.Map[String, Claim],
                              maybeReqId: Option[String],
                              maybeState: Option[String],
                              startTime: Long,
                              nextFilter: RequestHeader => Future[Result])(
      requestHeader: RequestHeader): Future[Result] = {

    val requestWithAuthInfo = for {
      sub <- claims.get("sub").map(_.asString)
      name = claims.get("name").map(_.asString)
      email = claims.get("email").map(_.asString)
      isAdmin = claims
        .get("nio_admin")
        .map(_.asString)
        .flatMap(str => Try(str.toBoolean).toOption)
        .getOrElse(false)
      metadatas = claims
        .filterKeys(header => header.startsWith("metadata"))
        .map(header =>
          (header._1.replaceFirst("metadata.", ""), header._2.asString()))
        .toSeq
      maybeOfferRestrictionPatterns = if (isAdmin)
        Some(Seq("*")) // if admin, there is no restriction on offer keys
      else
        claims
          .get("offers")
          .map(_.asString()) // Claim to String
          .map(s => s.split(",")) // String to array[String]
          .map(_.map(_.trim)) // Trim all string into array
          .map(_.toSeq) // Array[String] to Seq[String]
    } yield {
      logger.info(s"Request from sub: $sub, name:$name, isAdmin:$isAdmin")
      email
        .map { email =>
          requestHeader.addAttr(FilterAttributes.Email, email)
        }
        .getOrElse(requestHeader)
        .addAttr(FilterAttributes.AuthInfo,
                 Some(
                   AuthInfo(sub,
                            isAdmin,
                            Some(metadatas),
                            maybeOfferRestrictionPatterns)))
    }

    nextFilter(requestWithAuthInfo.getOrElse(requestHeader)).map { result =>
      val requestTime = System.currentTimeMillis - startTime
      maybeReqId.foreach { id =>
        logger.debug(
          s"Request from Gateway with id : $id => ${requestHeader.method} ${requestHeader.uri} with request headers ${requestHeader.headers.headers
            .map(h => s"""   "${h._1}": "${h._2}"\n""")
            .mkString(",")} took ${requestTime}ms and returned ${result.header.status} hasBody ${requestHeader.hasBody}"
        )
      }
      result.withHeaders(
        config.headerGatewayStateResp -> maybeState.getOrElse("--")
      )
    }
  }
}
