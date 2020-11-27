package libs.xml

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import cats.data.Validated
import libs.xml.syntax.XmlResult
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import utils.DateUtils
import utils.Result.{AppErrors, ErrorMessage}

import scala.util.Try
import scala.xml.{Elem, NodeSeq}
import scala.collection.Seq

object syntax {
  import cats.implicits._

  type XmlResult[T] = Validated[AppErrors, T]

  implicit class Conversion[T](any: T) {
    def toXmlResult: XmlResult[T] = any.valid
  }

  implicit class PathConverter(val path: Option[String]) extends AnyVal {
    def convert(): String =
      path.map(p => s"$p.").getOrElse("")
  }

  implicit class XmlSyntax(val nodeSeq: NodeSeq) extends AnyVal {
    def validate[T](implicit read: XMLRead[T]): XmlResult[T] =
      read.read(nodeSeq, None)

    def validateNullable[T](implicit read: XMLRead[Option[T]]): XmlResult[Option[T]] =
      read.read(nodeSeq, None)

    def validateNullable[T](default: T)(implicit read: XMLRead[Option[T]]): XmlResult[T] =
      read.read(nodeSeq, None).map(_.getOrElse(default))

    def validate[T](path: Option[String])(implicit read: XMLRead[T]): XmlResult[T] =
      read.read(nodeSeq, path)

    def validateNullable[T](path: Option[String])(implicit read: XMLRead[Option[T]]): XmlResult[Option[T]] =
      read.read(nodeSeq, path)

    def validateNullable[T](default: T, path: Option[String])(implicit read: XMLRead[Option[T]]): XmlResult[T] =
      read.read(nodeSeq, path).map(_.getOrElse(default))
  }

}

trait XMLRead[T] {
  def read(xml: NodeSeq, path: Option[String] = None): XmlResult[T]
}

object implicits {

  import cats.implicits._

  private def toPath(path: Option[String]): String =
    path.map(p => s".$p").getOrElse("")

  private def buildError(path: Option[String]): AppErrors =
    AppErrors.error(s"unknow.path${toPath(path)}")

  implicit def readString: XMLRead[String] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head.text)
        .map(_.valid)
        .getOrElse(buildError(path).invalid)

  implicit def readBoolean: XMLRead[Boolean] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head.text.toBoolean)
        .map(_.valid)
        .getOrElse(buildError(path).invalid)

  implicit def readSeq[T](implicit read: XMLRead[T]): XMLRead[Seq[T]] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head)
        .map { n =>
          val incr = new AtomicInteger(0)
          n.child
            .collect { case e: Elem =>
              read.read(e, Some(s"${path.map(p => s"$p.${incr.getAndIncrement()}").getOrElse(incr.getAndIncrement())}"))
            }
            .toList
            .sequence
        }
        .getOrElse(buildError(path).invalid)

  implicit def readInt: XMLRead[Int] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head.text.toInt)
        .map(_.valid)
        .getOrElse(buildError(path).invalid)

  implicit def readLong: XMLRead[Long] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head.text.toLong)
        .map(_.valid)
        .getOrElse(buildError(path).invalid)

  implicit def defaultReadDateTime: XMLRead[DateTime] =
    readDateTime(DateUtils.utcDateFormatter)

  def readDateTime(dateTimeFormatter: DateTimeFormatter): XMLRead[DateTime] =
    (xml: NodeSeq, path: Option[String]) =>
      Try(xml.head.text)
        .map(_.valid)
        .getOrElse(buildError(path).invalid)
        .andThen { t =>
          Try(dateTimeFormatter.parseDateTime(t))
            .map(_.valid)
            .getOrElse(AppErrors.error(s"parse.error${toPath(path)}").invalid)
        }

  implicit def readOption[T](implicit read: XMLRead[T]): XMLRead[Option[T]] =
    (xml: NodeSeq, path: Option[String]) => {
      val option: Option[XmlResult[T]] = xml.headOption.map(read.read(_, path))
      val res: XmlResult[Option[T]]    = option.sequence[XmlResult, T]
      res
    }

}

object XmlUtil {

  implicit class XmlCleaner(val elem: Elem) extends AnyVal {

    def clean(): Elem =
      scala.xml.Utility.trim(elem) match {
        case res if res.isInstanceOf[Elem] =>
          val prettyPrinter = new scala.xml.PrettyPrinter(1000, 2)
          val prettyXml     = prettyPrinter.format(res.asInstanceOf[Elem])
          scala.xml.XML.loadString(prettyXml)
      }

  }

}
