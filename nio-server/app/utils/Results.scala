package utils

import cats.Semigroup
import cats.kernel.Monoid
import play.api.libs.json._
import utils.Result.{AppErrors, ErrorMessage, Result}
import libs.xml.XmlUtil.XmlCleaner

import scala.collection.Seq
import scala.xml.Elem

object Result {

  case class ErrorMessage(message: String, args: String*) {
    def asXml(): Elem =
      <errorMessage>
        <message>
          {message}
        </message>
        <args>
          {args.mkString(", ")}
        </args>
      </errorMessage>.clean()
  }

  object ErrorMessage {
    implicit val format: OFormat[ErrorMessage] = Json.format[ErrorMessage]
  }

  case class AppErrors(
      errors: Seq[ErrorMessage] = Seq.empty,
      fieldErrors: Map[String, List[ErrorMessage]] = Map.empty
  ) {

    def ++(s: AppErrors): AppErrors =
      this.copy(errors = errors ++ s.errors, fieldErrors = fieldErrors ++ s.fieldErrors)

    def addFieldError(field: String, errors: List[ErrorMessage]): AppErrors =
      fieldErrors.get(field) match {
        case Some(err) =>
          AppErrors(errors, fieldErrors + (field -> (err ++ errors)))
        case None      => AppErrors(errors, fieldErrors + (field -> errors))
      }

    def asJson(): JsValue = Json.toJson(this)(AppErrors.format)

    def asXml(): Elem =
      <appErrors>
        <errors>
          {errors.map(_.asXml())}
        </errors>
        <fieldErrors>
          {
        fieldErrors.map(fieldErrorMessage => <fieldError>
            <field>
              {fieldErrorMessage._1}
            </field>
            <errorMessage>
              {fieldErrorMessage._2.map(_.asXml())}
            </errorMessage>
          </fieldError>)
      }
        </fieldErrors>
      </appErrors>.clean()

    def isEmpty = errors.isEmpty && fieldErrors.isEmpty
  }

  object AppErrors {

    import cats.instances.all._
    import cats.syntax.semigroup._

    implicit val format: OFormat[AppErrors] = Json.format[AppErrors]

    def fromJsError(jsError: Seq[(JsPath, Seq[JsonValidationError])]): AppErrors = {
      val fieldErrors = jsError.map { case (k, v) =>
        (k.toJsonString, v.map(err => ErrorMessage(err.message, err.args.map(_.toString): _*)).toList)
      }.toMap
      AppErrors(fieldErrors = fieldErrors)
    }

    def fromXmlError(throwable: Throwable): AppErrors =
      AppErrors(Seq(ErrorMessage(throwable.getMessage)))

    def error(messages: String*): AppErrors =
      AppErrors(messages.map(m => ErrorMessage(m)))

    private def optionCombine[A: Semigroup](a: A, opt: Option[A]): A =
      opt.map(a |+| _).getOrElse(a)

    private def mergeMap[K, V: Semigroup](lhs: Map[K, V], rhs: Map[K, V]): Map[K, V] =
      lhs.foldLeft(rhs) { case (acc, (k, v)) =>
        acc.updated(k, optionCombine(v, acc.get(k)))
      }

    implicit val monoid: Monoid[AppErrors] = new Monoid[AppErrors] {
      override def empty: AppErrors = AppErrors()

      override def combine(x: AppErrors, y: AppErrors): AppErrors = {
        val errors      = x.errors ++ y.errors
        val fieldErrors = mergeMap(x.fieldErrors, y.fieldErrors)
        AppErrors(errors, fieldErrors)
      }
    }

  }

  type Result[+E] = Either[AppErrors, E]

  def ok[E](event: E): Result[E] = Right(event)

  def fromJsResult[E](result: JsResult[E]): Result[E] = result.fold(
    err => Left(AppErrors.fromJsError(err)),
    ok => Result.ok(ok)
  )

  def error[E](error: AppErrors): Result[E] = Left(error)

  def error[E](messages: String*): Result[E] =
    Left(AppErrors(messages.map(m => ErrorMessage(m))))

  def errors[E](errs: ErrorMessage*): Result[E] = Left(AppErrors(errs))

  def fieldError[E](field: String, errs: ErrorMessage*): Result[E] =
    Left(AppErrors(fieldErrors = Map(field -> errs.toList)))

}

case class ImportResult(success: Int = 0, errors: AppErrors = AppErrors()) {
  def isError: Boolean = !errors.isEmpty

  def toJson: JsValue = ImportResult.format.writes(this)
}

object ImportResult {

  import cats.syntax.semigroup._

  implicit val format: OFormat[ImportResult] = Json.format[ImportResult]

  implicit val monoid: Monoid[ImportResult] = new Monoid[ImportResult] {
    override def empty: ImportResult = ImportResult()

    override def combine(x: ImportResult, y: ImportResult): ImportResult = (x, y) match {
      case (ImportResult(s1, e1), ImportResult(s2, e2)) =>
        ImportResult(s1 + s2, e1 |+| e2)
    }
  }

  def error(e: ErrorMessage): ImportResult = ImportResult(errors = AppErrors(errors = Seq(e)))

  def fromResult[T](r: Result[T]): ImportResult = r match {
    case Right(_)  => ImportResult(success = 1)
    case Left(err) => ImportResult(errors = err)
  }

}
