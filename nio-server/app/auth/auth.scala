package auth

import db.ExtractionTaskMongoDataStore
import play.api.Logger
import play.api.mvc._
import filters.OtoroshiFilter
import play.api.mvc.Results.Unauthorized

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import models.{DataStores, DestroyTask, ExtractTask, ExtractionTask}
import play.api.libs.json.Json

case class AuthInfo(sub: String, isAdmin: Boolean)

case class AuthContextWithEmail[A](request: Request[A],
                                   email: String,
                                   authInfo: AuthInfo)
    extends WrappedRequest[A](request)

class AuthActionWithEmail @Inject()(val parser: BodyParsers.Default)(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthContextWithEmail, AnyContent]
    with ActionFunction[Request, AuthContextWithEmail] {

  override def invokeBlock[A](
      request: Request[A],
      block: (AuthContextWithEmail[A]) => Future[Result]): Future[Result] = {
    (
      request.attrs.get(OtoroshiFilter.Email),
      request.attrs.get(OtoroshiFilter.AuthInfo)
    ) match {
      case (Some(email), Some(authInfo)) =>
        block(AuthContextWithEmail(request, email, authInfo))
      case _ =>
        Logger.info("Auth info is missing => Unauthorized")
        Future.successful(Unauthorized)
    }
  }
}

case class AuthContext[A](request: Request[A], authInfo: AuthInfo)
    extends WrappedRequest[A](request)

class AuthAction @Inject()(val parser: BodyParsers.Default)(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthContext, AnyContent]
    with ActionFunction[Request, AuthContext] {

  override def invokeBlock[A](
      request: Request[A],
      block: (AuthContext[A]) => Future[Result]): Future[Result] = {

    request.attrs
      .get(OtoroshiFilter.AuthInfo)
      .map { e =>
        block(AuthContext(request, e))
      }
      .getOrElse {
        Logger.info("Auth info is missing => Unauthorized")
        Future.successful(Unauthorized)
      }
  }
}

case class ReqWithExtractionTask[A](task: ExtractionTask, request: Request[A], authInfo: AuthInfo) extends WrappedRequest[A](request)

class ExtractionAction[A](val tenant: String,
                          val taskId: String,
                          val parser: BodyParser[A])(implicit val executionContext: ExecutionContext, store: ExtractionTaskMongoDataStore)
  extends ActionBuilder[ReqWithExtractionTask, A]
    with ActionFunction[Request, ReqWithExtractionTask] {

  override def invokeBlock[A](request: Request[A], block: (ReqWithExtractionTask[A]) => Future[Result]): Future[Result] = {
    store.findById(tenant, taskId).flatMap {
      case Some(task) =>
        request.attrs
          .get(OtoroshiFilter.AuthInfo)
          .map { e =>
            block(ReqWithExtractionTask[A](task, request, e))
          }
          .getOrElse {
            Logger.info("Auth info is missing => Unauthorized")
            Future.successful(Unauthorized)
          }

      case _ =>
        Future.successful(Results.NotFound("error.extraction.task.not.found"))
    }
  }
}

object ExtractionAction {
  def apply[A](tenant: String, taskId: String, parser: BodyParser[A])(implicit executionContext: ExecutionContext, store: ExtractionTaskMongoDataStore): ExtractionAction[A] =
    new ExtractionAction(tenant, taskId, parser)(executionContext, store)
}