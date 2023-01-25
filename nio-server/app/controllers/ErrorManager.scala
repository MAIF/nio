package controllers

import cats.kernel.Monoid
import play.api.mvc.Results.BadRequest
import utils.Result.{AppErrors, ErrorMessage}

import scala.collection.Seq

object ErrorManager {

  import utils.Result.AppErrors
  import play.api.mvc.Results._
  import play.api.mvc._

  implicit class ErrorManagerResult(val error: String) extends AnyVal {

    def badRequest()(implicit req: Request[Any],
                     f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), BadRequest)
    }

    def notFound()(implicit req: Request[Any], f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), NotFound)
    }

    def internalServerError()(implicit req: Request[Any],
                              f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), InternalServerError)
    }

    def conflict()(implicit req: Request[Any], f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), Conflict)
    }

    def forbidden()(implicit req: Request[Any],
                    f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), Forbidden)
    }

    def unauthorized()(implicit req: Request[Any],
                       f: ErrorManagerSuite): Result = {
      f.convert(AppErrors.error(error), Unauthorized)
    }
  }

  implicit class AppErrorManagerResult(val appErrors: AppErrors)
      extends AnyVal {

    def badRequest()(implicit req: Request[Any],
                     f: ErrorManagerSuite): Result = {
      f.convert(appErrors, BadRequest)
    }
    def unauthorized()(implicit req: Request[Any],
                       f: ErrorManagerSuite): Result = {
      f.convert(appErrors, Unauthorized)
    }
    def forbidden()(implicit req: Request[Any],
                    f: ErrorManagerSuite): Result = {
      f.convert(appErrors, Forbidden)
    }
    def notFound()(implicit req: Request[Any], f: ErrorManagerSuite): Result = {
      f.convert(appErrors, NotFound)
    }

    def convert(status: Status)(implicit req: Request[Any],
                                f: ErrorManagerSuite): Result = {
      f.convert(appErrors, status)
    }
  }

  implicit class ErrorWithStatusManagerResult(val error: AppErrorWithStatus)
      extends AnyVal {

    def renderError()(implicit req: Request[Any],
                      f: ErrorManagerSuite): Result = {
      error.appErrors.convert(error.status)
    }
  }

}
import play.api.mvc.Results.Status

case class AppErrorWithStatus(appErrors: AppErrors = AppErrors(), status: Status = BadRequest)

object AppErrorWithStatus {

  implicit val monoidInstance = Monoid.instance[AppErrorWithStatus](AppErrorWithStatus(), (err1, err2) =>
    AppErrorWithStatus(err1.appErrors ++ err2.appErrors, err1.status)
  )
  def apply(message: String): AppErrorWithStatus = AppErrorWithStatus(
    AppErrors(Seq(ErrorMessage(message))),
    BadRequest
  )
  def apply(message: String, status: Status): AppErrorWithStatus = AppErrorWithStatus(
    AppErrors(Seq(ErrorMessage(message))),
    status
  )
}
