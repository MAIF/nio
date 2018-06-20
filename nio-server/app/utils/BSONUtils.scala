package utils

import reactivemongo.bson.{
  BSONArray,
  BSONBoolean,
  BSONDecimal,
  BSONDocument,
  BSONDouble,
  BSONElement,
  BSONInteger,
  BSONLong,
  BSONMaxKey,
  BSONMinKey,
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
              s"${prefix}NumberLong($l)"

            case d @ BSONDecimal(_, _) =>
              s"${prefix}NumberDecimal($d)"

            case BSONString(s) =>
              prefix + '"' + s.replaceAll("\"", "\\\"") + '"'

            case oid @ BSONObjectID(_) =>
              s"${prefix}Object(${oid.stringify})"

            case ts @ BSONTimestamp(_) =>
              s"${prefix}Timestamp(${ts.time}, ${ts.ordinal})"

            case BSONUndefined => s"${prefix}undefined"
            case BSONMinKey    => s"${prefix}MinKey"
            case BSONMaxKey    => s"${prefix}MaxKey"

            case _ =>
              s"${prefix}$value"
          }
        }
        case Failure(e) => s"ERROR[${e.getMessage()}]"
      }
      .mkString(",")
  }

  def stringify(doc: BSONDocument): String =
    "{" + stringify(0, doc.stream.iterator) + "}"
}
