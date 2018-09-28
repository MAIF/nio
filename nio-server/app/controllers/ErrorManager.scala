package controllers

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
  }

}
