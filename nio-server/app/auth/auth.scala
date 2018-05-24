package auth

import play.api.Logger
import play.api.mvc._
import filters.OtoroshiFilter
import play.api.mvc.Results.Unauthorized

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

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
