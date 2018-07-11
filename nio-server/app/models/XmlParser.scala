package models

import cats.data.Validated
import models.XmlParser.XmlResult
import org.joda.time.DateTime
import utils.DateUtils
import utils.Result.AppErrors

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

object XmlParser {

  type XmlResult[T] = Validated[AppErrors, T]

  implicit class XmlSyntax(val nodeSeq: NodeSeq) extends AnyVal {
    def validate[T](implicit read: XMLRead[T]): XmlResult[T] =
      read.read(nodeSeq)

    def validateNullable[T](default: Option[T] = None)(
        implicit readNullable: XMLReadNullable[T]): XmlResult[Option[T]] =
      readNullable.readNullable(nodeSeq, default)
  }

}

trait XMLRead[T] {
  def read(xml: NodeSeq): XmlResult[T]
}

trait XMLReadNullable[T] {
  def readNullable(xml: NodeSeq,
                   default: Option[T] = None): XmlResult[Option[T]]
}

object XMLReads {
  import cats._
  import cats.implicits._
  import cats.data.Validated._

  implicit def readString: XMLRead[String] =
    (xml: NodeSeq) =>
      Try(xml.head.text)
        .map(Right(_))
        .getOrElse(Left(AppErrors.error("invalid.path")))
        .toValidated

  implicit def readInt: XMLRead[Int] =
    (xml: NodeSeq) =>
      Try(xml.head.text.toInt)
        .map(Right(_))
        .getOrElse(Left(AppErrors.error("invalid.path")))
        .toValidated

  implicit def readNullableString: XMLReadNullable[String] =
    (xml: NodeSeq, default: Option[String]) =>
      xml.headOption
        .map(v => Valid(Some(v.text)))
        .getOrElse(Valid(default))

  implicit def readNullableInt: XMLReadNullable[Int] =
    (xml: NodeSeq, default: Option[Int]) =>
      xml.headOption
        .map(v => Valid(Some(v.text.toInt)))
        .getOrElse(Valid(default))

  implicit def readNullableDateTime: XMLReadNullable[DateTime] =
    (xml: NodeSeq, default: Option[DateTime]) =>
      xml.headOption
        .map(v =>
          Valid(Option(DateUtils.utcDateFormatter.parseDateTime(v.text))))
        .getOrElse(Valid(default))

}

object XmlUtil {

  implicit class XmlCleaner(val elem: Elem) extends AnyVal {

    def clean(): Elem =
      scala.xml.Utility.trim(elem) match {
        case res if res.isInstanceOf[Elem] => res.asInstanceOf[Elem]
      }
  }
}
