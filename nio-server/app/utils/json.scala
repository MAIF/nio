package utils

import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess, JsonValidationError}

object json {

  implicit class JsResultOps[A](error: JsResult[A]) {
    def toEither(): Either[collection.Seq[(JsPath, collection.Seq[JsonValidationError])], A] = error match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => Left(errors)
    }
    def toEither[E](handleError: collection.Seq[(JsPath, collection.Seq[JsonValidationError])] => E): Either[E, A] = error match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => Left(handleError(errors))
    }
  }

}
