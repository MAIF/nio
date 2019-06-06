package db.postgres

import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.libs.json._
import scalikejdbc._
import async._
import scalikejdbc.streams.DatabasePublisher

import scala.concurrent.{ExecutionContext, Future}
import scalikejdbc.streams._

trait PostgresDataStore[T] extends SQLSyntaxSupport[T] {

  implicit def executionContext: ExecutionContext

  implicit def format: OFormat[T]

  def fromResultSet(rs: WrappedResultSet): T =
    Json
      .parse(rs.get[String]("payload"))
      .validate[T]
      .get

  override val columns = Seq("tenant", "payload")

  def init(tenant: String): Future[Unit] = {
    // delete everything
    for {
      _ <- deleteByTenant(tenant)
    } yield ()
  }

  def findOneByQuery(tenant: String, jsObject: JsObject): Future[Option[T]] = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${jsObject.toString()}"
        .map(rs => fromResultSet(rs))
        .first()
        .future()
    }
  }

  def findMany(tenant: String): Future[Seq[T]] = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${Json.obj().toString()}"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def insertOne(tenant: String, objToInsert: T): Future[Boolean] =
    AsyncDB withPool { implicit session =>
      sql"insert into ${table} (${column.tenant}, ${column.payload}) values (${tenant},  ${Json.toJson(objToInsert).toString()})"
        .update()
        .future()
        .map(_ > 0)
    }

  def ensureIndices(tenant: String): Future[Unit] =
    // not useful
    for {
      _ <- Future.successful("")
    } yield {
      ()
    }

  def updateOne(tenant: String, id: String, objToUpdate: T): Future[Boolean] = {
    AsyncDB withPool { implicit session =>
      sql"update ${table} set payload = ${Json.toJson(objToUpdate).toString()} where tenant = ${tenant} and payload @> ${Json.obj("_id" -> id).toString()}"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def updateOneByQuery(tenant: String,
                       query: JsObject,
                       objToUpdate: T): Future[Boolean] = {

    updateByQuery(tenant, query, Json.toJson(objToUpdate).as[JsObject])
  }

  def updateByQuery(tenant: String,
                    query: JsObject,
                    update: JsObject): Future[Boolean] = {
    AsyncDB withPool { implicit session =>
      sql"update ${table} set payload = ${update
        .toString()} where tenant = ${tenant} and  payload @> ${query.toString()}"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def findOneById(tenant: String, id: String): Future[Option[T]] = {
    findOneByQuery(tenant, Json.obj("_id" -> id))
  }

  def streamAsyncAsJsValue(tenant: String)(implicit mat: Materializer) = {

    val publisher: DatabasePublisher[JsValue] = DB readOnlyStream {
      sql"select * from ${table} where tenant=${tenant} "
        .map(rs => Json.toJson(fromResultSet(rs)))
        .iterator
    }
    FastFuture.successful(
      Source.fromFutureSource(
        FastFuture.successful(
          Source.fromPublisher(publisher)
        )
      )
    )
  }

  def streamAsJsValue(tenant: String)(implicit mat: Materializer) = {

    val publisher: DatabasePublisher[JsValue] = DB readOnlyStream {
      sql"select * from ${table} where tenant=${tenant} "
        .map(rs => Json.toJson(fromResultSet(rs)))
        .iterator
    }
    Source.fromPublisher(publisher)
  }

  def streamAsJsValue(tenant: String, limit: Int, offset: Int)(
      implicit mat: Materializer) = {

    val publisher: DatabasePublisher[JsValue] = DB readOnlyStream {
      sql"select * from ${table} where tenant=${tenant} limit ${limit} offset ${offset}"
        .map(rs => Json.toJson(fromResultSet(rs)))
        .iterator
    }
    Source.fromPublisher(publisher)
  }

  def streamAsyncByQuery(tenant: String, query: JsObject)(
      implicit mat: Materializer) = {

    val publisher: DatabasePublisher[T] = DB readOnlyStream {
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}"
        .map(rs => fromResultSet(rs))
        .iterator
    }
    // sorry about this
    FastFuture.successful(
      Source.fromFutureSource(
        FastFuture.successful(Source.fromPublisher(publisher))))
  }

  def streamAsyncByQueryAsJsValue(tenant: String, query: JsObject)(
      implicit mat: Materializer) = {

    val publisher: DatabasePublisher[JsValue] = DB readOnlyStream {
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}"
        .map(rs => Json.toJson(fromResultSet(rs)))
        .iterator
    }
    FastFuture.successful(
      Source.fromFutureSource(
        FastFuture.successful(
          Source.fromPublisher(publisher)
        )
      )
    )
  }

  def findManyByQuery(tenant: String, query: JsObject): Future[Seq[T]] = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def count(tenant: String, query: JsObject): Future[Int] = {
    AsyncDB withPool { implicit session =>
      sql"select count(*) as c from ${table} where tenant=${tenant} and payload @> ${query.toString()}"
        .map(rs => rs.get[Int]("c"))
        .single()
        .future()
        .map(_.get)
    }
  }

  def findManyByQueryPaginateCount(tenant: String,
                                   query: JsObject,
                                   sort: JsObject = Json.obj("_id" -> 1),
                                   page: Int,
                                   pageSize: Int): Future[(Seq[T], Int)] = {
    for {
      count <- count(tenant, query)
      queryRes <- findManyByQueryPaginate(tenant, query, sort, page, pageSize)
    } yield {
      (queryRes, count)
    }
  }

  def findManyByQueryPaginate(tenant: String,
                              query: JsObject,
                              sort: JsObject = Json.obj("_id" -> -1),
                              page: Int,
                              pageSize: Int): Future[Seq[T]] = {

    AsyncDB withPool { implicit session =>
      val sqlQuery = (sort \ "_id").asOpt[Int].getOrElse(1) match {
        case 1 =>
          sql"select * from ${table} where tenant=${tenant} and payload @> ${query
            .toString()} order by payload->>'_id' asc limit ${pageSize} offset ${page * pageSize} "
        case _ =>
          sql"select * from ${table} where tenant=${tenant} and payload @> ${query
            .toString()} order by payload->>'_id' desc limit ${pageSize} offset ${page * pageSize} "
      }
      sqlQuery
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def deleteOneById(tenant: String, id: String): Future[Boolean] = {
    deleteByQuery(tenant, Json.obj("_id" -> id))
  }

  def deleteByQuery(tenant: String, query: JsObject): Future[Boolean] = {
    AsyncDB withPool { implicit session =>
      sql"delete from ${table} where tenant = ${tenant} and  payload @> ${query.toString()}"
        .update()
        .future()
        .map(_ > 0)
    }
  }

  def deleteByTenant(tenant: String): Future[Boolean] = {
    AsyncDB withPool { implicit session =>
      sql"delete from ${table} where tenant = ${tenant}"
        .update()
        .future()
        .map(_ > 0)
    }
  }
}
