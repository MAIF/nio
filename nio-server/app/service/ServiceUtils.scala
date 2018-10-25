package service

import akka.http.scaladsl.util.FastFuture
import controllers.AppErrorWithStatus
import play.api.mvc.Results._
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.Future

trait ServiceUtils {

  protected def toErrorWithStatus[T](
      errorMessage: String,
      status: Status = BadRequest): Future[Either[AppErrorWithStatus, T]] = {
    toErrorWithStatus(AppErrors(Seq(ErrorMessage(errorMessage))), status)
  }

  protected def toErrorWithStatus[T](
      appErrors: AppErrors,
      status: Status): Future[Either[AppErrorWithStatus, T]] = {
    toErrorWithStatus(AppErrorWithStatus(appErrors, status))
  }

  protected def toErrorWithStatus[T](
      appErrors: AppErrorWithStatus): Future[Either[AppErrorWithStatus, T]] = {
    FastFuture.successful(Left(appErrors))
  }

  protected def sequence[A, B](s: Seq[Either[A, B]]): Either[Seq[A], B] =
    s.foldLeft(Left(Nil): Either[List[A], B]) { (acc, e) =>
      for (xs <- acc.left; x <- e.left) yield x :: xs
    }
}
