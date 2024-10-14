package controllers

import java.io.FileInputStream
import java.util.Base64
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{Source, StreamConverters}
import org.apache.pekko.util.ByteString
import auth.SecuredAuthContext
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import configuration.Env
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult}
import db.{OrganisationMongoDataStore, UserExtractTaskDataStore}
import messaging.KafkaMessageBroker
import models._

import java.time.{Clock, LocalDateTime}
import utils.NioLogger
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc._
import service.MailService
import utils.Result.AppErrors
import utils.{FSUserExtractManager, S3ExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class UserExtractController(
    env: Env,
    actorSystem: ActorSystem,
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    userExtractTaskDataStore: UserExtractTaskDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    broker: KafkaMessageBroker,
    fSUserExtractManager: FSUserExtractManager,
    mailService: MailService
)(implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {
  implicit val readable: ReadableEntity[UserExtract] = UserExtract

  implicit val s3ExecutionContext: S3ExecutionContext = S3ExecutionContext(
    actorSystem.dispatchers.lookup("S3-dispatcher")
  )

  def extractData(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(bodyParser) { implicit req =>
      // Read body
      req.body.read[UserExtract] match {
        case Left(error)        =>
          NioLogger.error("Unable to parse user extract  " + error)
          Future.successful(error.badRequest())
        case Right(userExtract) =>
          // control if an organisation with the orgkey exist
          organisationMongoDataStore
            .findByKey(tenant, orgKey)
            .flatMap {
              case Some(_) =>
                // control if an extract task already exist
                userExtractTaskDataStore
                  .find(tenant, orgKey, userId)
                  .flatMap {
                    // if an extract has been already asked we send a conflict status
                    case Some(_) =>
                      Future.successful(s"extract.for.user.$userId.already.asked".conflict())
                    case None    =>
                      val userExtractTask: UserExtractTask =
                        UserExtractTask.instance(tenant, orgKey, userId, userExtract.email)

                      // Insert userExtractTask on Db
                      userExtractTaskDataStore
                        .create(userExtractTask)
                        .map { _ =>
                          // publish associate event to kafka
                          broker.publish(
                            UserExtractTaskAsked(
                              tenant = tenant,
                              author = req.authInfo.sub,
                              metadata = req.authInfo.metadatas,
                              payload = userExtractTask
                            )
                          )

                          renderMethod(userExtractTask, Ok)
                        }
                  }
              // if the organisation doesn't exist
              case None    =>
                Future.successful(s"organisation.$orgKey.not.found".notFound())
            }
      }
    }

  def extractedData(tenant: String, orgKey: String, page: Int, pageSize: Int) =
    AuthAction.async { implicit req =>
      // control if an organisation with the orgkey exist
      organisationMongoDataStore
        .findByKey(tenant, orgKey)
        .flatMap {
          // if the organisation doesn't exist
          case None    =>
            Future.successful(s"organisation.$orgKey.not.found".notFound())
          case Some(_) =>
            userExtractTaskDataStore
              .findByOrgKey(tenant, orgKey, page, pageSize)
              .map { res =>
                renderMethod(UserExtractTasks(page = page, pageSize = pageSize, count = res._2, items = res._1), Ok)
              }
        }

    }

  def userExtractedData(tenant: String, orgKey: String, userId: String, page: Int, pageSize: Int) =
    AuthAction.async { implicit req =>
      // control if an organisation with the orgkey exist
      organisationMongoDataStore
        .findByKey(tenant, orgKey)
        .flatMap {
          // if the organisation doesn't exist
          case None    =>
            Future.successful(s"organisation.$orgKey.not.found".notFound())
          case Some(_) =>
            userExtractTaskDataStore
              .findByOrgKeyAndUserId(tenant, orgKey, userId, page, pageSize)
              .map { res =>
                renderMethod(UserExtractTasks(page = page, pageSize = pageSize, count = res._2, items = res._1), Ok)
              }
        }

    }

  def uploadFile(tenant: String, orgKey: String, userId: String, name: String) =
    AuthAction.async(parse.multipartFormData) { implicit req =>
      val src: Source[ByteString, Future[IOResult]] =
        StreamConverters.fromInputStream { () =>
          new FileInputStream(req.body.files.head.ref)
        }

      // control if an extract task for this user/organisation/tenant exist
      userExtractTaskDataStore
        .find(tenant, orgKey, userId)
        .flatMap {
          case None              =>
            Future.successful(
              s"user.extract.task.for.user.$userId.and.organisation.$orgKey.not.found"
                .notFound()
            )
          case Some(extractTask) =>
            val startUploadAt = LocalDateTime.now(Clock.systemUTC)

            val future: Future[Either[AppErrors, String]] = for {
              downloadedFileUrl                    <- Future {
                                                        s"${env.config.downloadFileHost}/$name?uploadToken=${encryptToken(tenant, orgKey, userId, extractTask._id, name, req.body.files.head.contentType)}"
                                                      }
              _                                    <- fSUserExtractManager.userExtractUpload(tenant, orgKey, userId, extractTask._id, name, src)
              _                                    <- mailService.sendDownloadedFile(extractTask.email, downloadedFileUrl.toString)
              taskUpdateEndedDate: UserExtractTask <- Future {
                                                        extractTask.copy(
                                                          endedAt = Some(LocalDateTime.now(Clock.systemUTC)),
                                                          uploadStartedAt = Some(startUploadAt)
                                                        )
                                                      }
              _                                    <- userExtractTaskDataStore.update(extractTask._id, taskUpdateEndedDate)
              _                                    <- Future {
                                                        broker.publish(
                                                          UserExtractTaskCompleted(
                                                            tenant = tenant,
                                                            author = req.authInfo.sub,
                                                            metadata = req.authInfo.metadatas,
                                                            payload = taskUpdateEndedDate
                                                          )
                                                        )
                                                      }
            } yield Right(downloadedFileUrl)
            future.map {
              case Right(url) =>
                Ok(Json.obj("url" -> url))
              case Left(_)    =>
                "error.during.upload.file".internalServerError()
            }
        }
    }

  private def encryptToken(
      tenant: String,
      orgKey: String,
      userId: String,
      extractTaskId: String,
      name: String,
      maybeContentType: Option[String]
  ) = {
    val config    = env.config.filter.otoroshi
    val algorithm = Algorithm.HMAC512(config.sharedKey)

    Base64.getEncoder
      .encodeToString(
        JWT
          .create()
          .withIssuer(config.issuer)
          .withClaim("tenant", tenant)
          .withClaim("orgKey", orgKey)
          .withClaim("userId", userId)
          .withClaim("extractTaskId", extractTaskId)
          .withClaim("name", name)
          .withClaim("contentType", maybeContentType.getOrElse(""))
          .sign(algorithm)
          .getBytes()
      )
  }

  private def decryptToken(token: String): Either[AppErrors, (String, String, String, String, String, String)] = {
    val config    = env.config.filter.otoroshi
    val algorithm = Algorithm.HMAC512(config.sharedKey)
    val verifier  = JWT
      .require(algorithm)
      .withIssuer(config.issuer)
      .acceptLeeway(5000)
      .build()

    val decoded =
      verifier.verify(new String(Base64.getDecoder.decode(token.getBytes())))

    import scala.jdk.CollectionConverters._
    val claims = decoded.getClaims.asScala

    val maybeTuple: Option[(String, String, String, String, String, String)] =
      for {
        tenant        <- claims.get("tenant").map(_.asString)
        orgKey        <- claims.get("orgKey").map(_.asString)
        userId        <- claims.get("userId").map(_.asString)
        extractTaskId <- claims.get("extractTaskId").map(_.asString)
        name          <- claims.get("name").map(_.asString)
        contentType   <- claims.get("contentType").map(_.asString)
      } yield (
        tenant,
        orgKey,
        userId,
        extractTaskId,
        name,
        contentType
      )

    maybeTuple match {
      case Some(valid) => Right(valid)
      case None        => Left(AppErrors.error("invalid.download.token"))
    }
  }

  def downloadFile(filename: String, token: String) = AuthAction.async { implicit req =>
    decryptToken(token) match {
      case Right(data) =>
        val tenant        = data._1
        val orgKey        = data._2
        val userId        = data._3
        val extractTaskId = data._4
        val name          = data._5
        val contentType   = data._6

        val maybeSource: Future[Source[ByteString, Future[IOResult]]] =
          fSUserExtractManager.getUploadedFile(tenant, orgKey, userId, extractTaskId, name)

        maybeSource.map { source =>
          val src: Source[ByteString, Future[IOResult]] = source

          Result(
            header = ResponseHeader(
              OK,
              Map(
                CONTENT_DISPOSITION -> "attachment",
                "filename"          -> filename
              )
            ),
            body = HttpEntity.Streamed(src, None, Some(contentType))
          )
        }
      case Left(error) =>
        NioLogger.error("Unable to parse token  " + error)
        Future.successful(error.unauthorized())
    }

  }

  def streamFile: BodyParser[Source[ByteString, ?]] =
    BodyParser { _ =>
      Accumulator.source[ByteString].map(s => Right(s))
    }

}
