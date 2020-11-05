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
      sql"select * from ${table} where tenant=${tenant} and payload @> ${jsObject.toString()}::jsonb"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
        .map(_.headOption)
    }
  }

  def findMany(tenant: String): Future[Seq[T]] = {
    AsyncDB withPool { implicit session =>
      sql"select * from ${table} where tenant=${tenant}"
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
      sql"update ${table} set payload = ${Json.toJson(objToUpdate).toString()} where tenant = ${tenant} and payload @> ${Json.obj("_id" -> id).toString()}::jsonb"
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
        .toString()} where tenant = ${tenant} and  payload @> ${query.toString()}::jsonb"
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

  def streamAsJsValue(tenant: String, limit: Int, offset: Int) = {

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
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb"
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
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb"
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
      sql"select * from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb"
        .map(rs => fromResultSet(rs))
        .list()
        .future()
    }
  }

  def count(tenant: String, query: JsObject): Future[Int] = {
    AsyncDB withPool { implicit session =>
      sql"select count(*) as _count from ${table} where tenant=${tenant} and payload @> ${query.toString()}::jsonb"
        .map(rs => rs.get[Int]("_count"))
        .single()
        .future()
        .map(_.get)
    }
  }

  def count(tenant: String,
            rootQuery: JsObject,
            query: JsArray): Future[Int] = {
    AsyncDB withPool { implicit session =>
      sql"""with temps as (
            select id, jsonb_array_elements(${query
        .toString()}::jsonb ) <@ any (array [jsonb_array_elements(payload -> 'groups')]) as agg
            from ${table} lf where lf.tenant = ${tenant} and payload @> ${rootQuery
        .toString()}::jsonb), agg_2 as (select id, ARRAY_AGG(agg) from temps group by id),
            ids as (select * from agg_2 where true = all(array_remove(array_agg, null)) )
            select count(*) as _count from  ids"""
        .map(rs => rs.get[Int]("_count"))
        .single()
        .future()
        .map(_.get)
    }
  }

  def findManyByQueriesPaginateCount(tenant: String,
                                     rootQuery: JsObject,
                                     query: JsArray,
                                     sort: JsObject = Json.obj("_id" -> 1),
                                     page: Int,
                                     pageSize: Int): Future[(Seq[T], Int)] = {
    for {
      count <- count(tenant, rootQuery, query)
      queryRes <- findManyByQueriesPaginate(tenant,
                                            rootQuery,
                                            query,
                                            sort,
                                            page,
                                            pageSize)
    } yield {
      (queryRes, count)
    }
  }

  def findManyByQueriesPaginate(tenant: String,
                                rootQuery: JsObject,
                                query: JsArray,
                                sort: JsObject = Json.obj("_id" -> -1),
                                page: Int,
                                pageSize: Int): Future[Seq[T]] = {

    AsyncDB withPool { implicit session =>
      val sqlQuery = (sort \ "_id").asOpt[Int].getOrElse(1) match {
        case 1 =>
          sql"""with temps as (
            select id, jsonb_array_elements(${query
            .toString()}::jsonb ) <@ any (array [jsonb_array_elements(payload -> 'groups')]) as agg
            from ${table} lf where lf.tenant = ${tenant} and payload @> ${rootQuery
            .toString()}::jsonb), agg_2 as (select id, ARRAY_AGG(agg) from temps group by id),
            ids as (select * from agg_2 where true = all(array_remove(array_agg, null)) limit ${pageSize} offset ${page * pageSize} )
            select lf.* from ${table} lf, ids where ids.id = lf.id order by lf.payload->>'_id' asc """
        case _ =>
          sql"""with temps as (
            select id, jsonb_array_elements(${query
            .toString()}::jsonb ) <@ any (array [jsonb_array_elements(payload -> 'groups')]) as agg
            from ${table} lf where lf.tenant = ${tenant} and payload @> ${rootQuery
            .toString()}::jsonb), agg_2 as (select id, ARRAY_AGG(agg) from temps group by id),
            ids as (select * from agg_2 where true = all(array_remove(array_agg, null)) limit ${pageSize} offset ${page * pageSize} )
            select lf.* from ${table} lf, ids where ids.id = lf.id order by lf.payload->>'_id' desc """
      }
      sqlQuery
        .map(rs => fromResultSet(rs))
        .list()
        .future()
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
            .toString()}::jsonb order by payload->>'_id' asc limit ${pageSize} offset ${page * pageSize} "
        case _ =>
          sql"select * from ${table} where tenant=${tenant} and payload @> ${query
            .toString()}::jsonb order by payload->>'_id' desc limit ${pageSize} offset ${page * pageSize} "
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
      sql"delete from ${table} where tenant = ${tenant} and  payload @> ${query.toString()}::jsonb"
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
