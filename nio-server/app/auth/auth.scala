package auth

import org.apache.pekko.http.scaladsl.util.FastFuture
import configuration.Env
import db.ExtractionTaskMongoDataStore
import utils.NioLogger
import play.api.mvc._
import filters.FilterAttributes
import play.api.mvc.Results.{Forbidden, Unauthorized}

import scala.concurrent.{ExecutionContext, Future}
import models.ExtractionTask
import scala.collection.Seq

case class AuthInfo(
    sub: String,
    isAdmin: Boolean,
    metadatas: Option[Seq[(String, String)]] = None,
    offerRestrictionPatterns: Option[Seq[String]] = None
)

case class AuthContextWithEmail[A](request: Request[A], email: String, authInfo: Option[AuthInfo])
    extends WrappedRequest[A](request)

class AuthActionWithEmail(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthContextWithEmail, AnyContent]
    with ActionFunction[Request, AuthContextWithEmail] {

  override def invokeBlock[A](request: Request[A], block: AuthContextWithEmail[A] => Future[Result]): Future[Result] = {

    val maybeMaybeInfo = request.attrs.get(FilterAttributes.AuthInfo)

    maybeMaybeInfo.map { authInfo =>
      AuthContextWithEmail(request, request.attrs.get(FilterAttributes.Email).getOrElse(""), authInfo)
    } match {
      case Some(ctx) => block(ctx)
      case None      =>
        NioLogger.info("Auth info is missing => Unauthorized")
        Future.successful(Unauthorized)
    }
  }
}

case class SecuredAuthContext[A](request: Request[A], authInfo: AuthInfo) extends WrappedRequest[A](request)

case class AuthContext[A](request: Request[A], authInfo: Option[AuthInfo]) extends WrappedRequest[A](request)

class AuthAction(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthContext, AnyContent]
    with ActionFunction[Request, AuthContext] {

  override def invokeBlock[A](request: Request[A], block: AuthContext[A] => Future[Result]): Future[Result] = {
    val maybeMaybeInfo: Option[Option[AuthInfo]] =
      request.attrs.get(FilterAttributes.AuthInfo)
    maybeMaybeInfo.map { auth =>
      AuthContext(request, auth)
    } match {
      case Some(ctx) => block(ctx)
      case None      =>
        NioLogger.info("Auth info is missing => Unauthorized")
        FastFuture.successful(Unauthorized)
    }
  }
}

class SecuredAction(val env: Env, val parser: BodyParser[AnyContent])(implicit
    val executionContext: ExecutionContext
) extends ActionBuilder[SecuredAuthContext, AnyContent]
    with ActionFunction[Request, SecuredAuthContext] {

  override def invokeBlock[A](request: Request[A], block: SecuredAuthContext[A] => Future[Result]): Future[Result] = {
    val maybeMaybeInfo: Option[Option[AuthInfo]] =
      request.attrs.get(FilterAttributes.AuthInfo)

    maybeMaybeInfo match {
      case Some(Some(info)) => block(SecuredAuthContext(request, info))
      case Some(None)       =>
        NioLogger.debug("Auth info is empty => Forbidden")
        FastFuture.successful(Forbidden)
      case _                =>
        NioLogger.debug("Auth info is missing => Unauthorized")
        FastFuture.successful(Unauthorized)
    }
  }
}

case class ReqWithExtractionTask[A](task: ExtractionTask, request: Request[A], authInfo: AuthInfo)
    extends WrappedRequest[A](request)

class ExtractionAction[A](val tenant: String, val taskId: String, val parser: BodyParser[A])(implicit
    val executionContext: ExecutionContext,
    store: ExtractionTaskMongoDataStore
) extends ActionBuilder[ReqWithExtractionTask, A]
    with ActionFunction[Request, ReqWithExtractionTask] {

  override def invokeBlock[A](request: Request[A], block: ReqWithExtractionTask[A] => Future[Result]): Future[Result] =
    store.findById(tenant, taskId).flatMap {
      case Some(task) =>
        val maybeMaybeInfo: Option[Option[AuthInfo]] =
          request.attrs.get(FilterAttributes.AuthInfo)

        maybeMaybeInfo match {
          case Some(Some(info)) =>
            block(ReqWithExtractionTask[A](task, request, info))
          case Some(None)       =>
            NioLogger.debug("Auth info is empty => Forbidden")
            FastFuture.successful(Forbidden)
          case _                =>
            NioLogger.debug("Auth info is missing => Unauthorized")
            FastFuture.successful(Unauthorized)
        }
      case _          =>
        Future.successful(Results.NotFound("error.extraction.task.not.found"))
    }
}

object ExtractionAction {
  def apply[A](tenant: String, taskId: String, parser: BodyParser[A])(implicit
      executionContext: ExecutionContext,
      store: ExtractionTaskMongoDataStore
  ): ExtractionAction[A] =
    new ExtractionAction(tenant, taskId, parser)(executionContext, store)
}
