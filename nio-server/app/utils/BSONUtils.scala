package utils

import reactivemongo.bson.{
  BSONArray,
  BSONBoolean,
  BSONDateTime,
  BSONDecimal,
  BSONDocument,
  BSONDouble,
  BSONElement,
  BSONInteger,
  BSONLong,
  BSONMaxKey,
  BSONMinKey,
  BSONNull,
  BSONObjectID,
  BSONString,
  BSONTimestamp,
  BSONUndefined
}

import scala.util.{Failure, Success, Try}

object BSONUtils {
  private def stringify(i: Int,
                        it: Iterator[Try[BSONElement]],
                        f: String => String = { name =>
                          s""""${name}": """
                        }): String = {
    it.map {
        case Success(BSONElement(name, value)) => {
          val prefix = s"${f(name)}"

          value match {
            case array: BSONArray =>
              s"${prefix}[" + stringify(i + 1,
                                        array.elements.map(Success(_)).iterator,
                                        _ => "") + s"]"

            case BSONBoolean(b) =>
              s"${prefix}$b"

            case BSONDocument(elements) =>
              s"${prefix}{" + stringify(i + 1, elements.iterator) + s"}"

            case BSONDouble(d) =>
              s"""${prefix}$d"""

            case BSONInteger(i) =>
              s"${prefix}$i"

            case BSONLong(l) =>
              s"${prefix}$l"

            case d @ BSONDecimal(_, _) =>
              s"${prefix}$d"

            case BSONString(s) =>
              prefix + '"' + s.replaceAll("\"", "\\\"") + '"'

            case oid @ BSONObjectID(_) =>
              prefix + '"' + oid.stringify.replaceAll("\"", "\\\"") + '"'

            case dt @ BSONDateTime(_) =>
              s"${prefix}${dt.value}"

            case ts @ BSONTimestamp(_) =>
              prefix + s"""{"time":${ts.time},"ordinal":${ts.ordinal}"}"""

            case BSONUndefined => s"${prefix}undefined"
            case BSONNull      => s"${prefix}null"
            case _ =>
              prefix + '"' + value.toString.replaceAll("\"", "\\\"") + '"'
          }
        }
        case Failure(e) => s"ERROR[${e.getMessage()}]"
      }
      .mkString(",")
  }

  def stringify(doc: BSONDocument): String =
    "{" + stringify(0, doc.stream.iterator) + "}"
}
