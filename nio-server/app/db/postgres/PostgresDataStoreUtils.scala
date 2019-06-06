package db.postgres

import play.api.libs.json.{Format, JsObject, Json}
import scalikejdbc.WrappedResultSet
import scalikejdbc._
import async._
import scalikejdbc.async.AsyncDB

import scala.concurrent.ExecutionContext

trait PostgresDataStoreUtils[T] extends SQLSyntaxSupport[T] {

  implicit val ec: ExecutionContext

  implicit def format: Format[T]

  def fromResultSet(rs: WrappedResultSet): T =
    Json
      .parse(rs.get[String]("payload"))
      .validate[T]
      .get

  override val columns = Seq("payload")

  def init() = {
    for {
      _ <- deleteAll()
    } yield ()
  }

  def deleteAll() = {
    AsyncDB withPool { implicit session =>
      sql"delete from ${table}"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def insert(objectToInsert: T) = {
    AsyncDB withPool { implicit session =>
      sql"insert into ${table} (${column.payload}) values (${format.writes(objectToInsert).as[JsObject].toString()})"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def findOneByQuery(query: JsObject) = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where payload @> ${query.toString()}"
        .map(rs => fromResultSet(rs))
        .single()
        .future()
    }
  }

  def deleteByQuery(query: JsObject) = {
    AsyncDB withPool { implicit session =>
      sql"delete from ${table} where payload @> ${query.toString()}"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def findAll() = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table}"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

}
