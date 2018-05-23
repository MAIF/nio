package utils

import java.util.concurrent.TimeUnit

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.Elem

trait TestUtils
    extends PlaySpec
    with GuiceOneServerPerSuite
    with WithKafka
    with WithMongo
    with WordSpecLike
    with MustMatchers
    with OptionValues
    with BeforeAndAfterAll {
  protected val serverHost: String = s"http://localhost:${this.port}"
  protected val apiPath: String = s"$serverHost/api"

  private val jsonHeaders: Seq[(String, String)] = Seq(
    ACCEPT -> JSON,
    CONTENT_TYPE -> JSON
  )

  private val xmlHeaders: Seq[(String, String)] = Seq(
    ACCEPT -> XML,
    CONTENT_TYPE -> XML
  )

  protected def ws: WSClient = app.injector.instanceOf[WSClient]

  override def fakeApplication() = {
    val application: Application = new GuiceApplicationBuilder()
      .configure(customConf)
      .build()

    application
  }

  override protected def beforeAll(): Unit = {
    startMongo()

    startKafka()
  }

  override protected def afterAll(): Unit = {
    stopMongo()

    stopKafka()
  }

  private def customConf: Map[String, String] = {
    val mongoUrl = "mongodb://localhost:" + getMongoPort() + "/nio"
    Map(
      "nio.mongo.url" -> mongoUrl,
      "mongodb.uri" -> mongoUrl,
      "tenant.admin.secret" -> "secret",
      "db.flush" -> "true",
      "nio.kafka.port" -> getKafkaPort(),
      "nio.kafka.servers" -> s"127.0.0.1:${getKafkaPort()}",
      "nio.kafka.topic" -> getKafkaTopic()
    )
  }

  private def callByType[T: BodyWritable](path: String,
                                          httpVerb: String,
                                          body: T = null,
                                          api: Boolean = true,
                                          headers: Seq[(String, String)] = Seq(
                                            ACCEPT -> JSON,
                                            CONTENT_TYPE -> JSON
                                          )): WSResponse = {

    val suffix = if (api) apiPath else serverHost
    val futureResponse = httpVerb match {
      case GET =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers: _*)
          .get()
      case DELETE =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers: _*)
          .delete()
      case POST =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers: _*)
          .post(body)
      case PUT =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers: _*)
          .put(body)
      case _ =>
        Future.failed(
          new IllegalArgumentException(s"Unknown http verb: $httpVerb"))
    }
    Await.result[WSResponse](futureResponse, Duration(10, TimeUnit.SECONDS))
  }

  def postJson(path: String,
               body: JsValue,
               headers: Seq[(String, String)] = jsonHeaders) = {
    callByType[JsValue](path = path,
                        httpVerb = POST,
                        body = body,
                        headers = headers)
  }

  def getJson(path: String, headers: Seq[(String, String)] = jsonHeaders) = {
    callByType[JsValue](path = path, httpVerb = GET, headers = headers)
  }

  def putJson(path: String,
              body: JsValue,
              headers: Seq[(String, String)] = jsonHeaders) = {
    callByType[JsValue](path = path,
                        httpVerb = PUT,
                        body = body,
                        headers = headers)
  }

  def delete(path: String, headers: Seq[(String, String)] = jsonHeaders) = {
    callByType[JsValue](path = path, httpVerb = DELETE, headers = headers)
  }

  def postXml(path: String,
              body: Elem,
              headers: Seq[(String, String)] = xmlHeaders) = {
    callByType[Elem](path = path,
                     httpVerb = POST,
                     body = body,
                     headers = headers)
  }

  def getXml(path: String, headers: Seq[(String, String)] = xmlHeaders) = {
    callByType[Elem](path = path, httpVerb = GET, headers = headers)
  }

  def putXml(path: String,
             body: Elem,
             headers: Seq[(String, String)] = xmlHeaders) = {
    callByType[Elem](path = path,
                     httpVerb = PUT,
                     body = body,
                     headers = headers)
  }

  protected def callJson(path: String,
                         httpVerb: String,
                         body: JsValue = null,
                         api: Boolean = true,
                         headers: Seq[(String, String)] = Seq(
                           ACCEPT -> JSON,
                           CONTENT_TYPE -> JSON
                         )): WSResponse = {
    callByType[JsValue](path, httpVerb, body, api, headers)
  }

  protected def callXml(path: String,
                        httpVerb: String,
                        body: Elem = null,
                        api: Boolean = true,
                        headers: Seq[(String, String)] = Seq(
                          ACCEPT -> XML,
                          CONTENT_TYPE -> XML
                        )): WSResponse = {
    callByType[Elem](path, httpVerb, body, api, headers)
  }
}
