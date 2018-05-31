package utils

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.{lang, util}

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.amazonaws.services.s3.model.PutObjectResult
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  StringDeserializer
}
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.Elem

class MockS3Manager extends FSManager {
  override def addFile(key: String, content: String)(
      implicit s3ExecutionContext: S3ExecutionContext)
    : Future[PutObjectResult] = {
    Future {
      null
    }
  }
}

trait TestUtils
    extends PlaySpec
    with GuiceOneServerPerSuite
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

  val kafkaPort = 9092

  protected def ws: WSClient = app.injector.instanceOf[WSClient]

  override def fakeApplication() = {

    val application: Application = new GuiceApplicationBuilder()
      .overrides(bind[FSManager].to[MockS3Manager])
      .configure(customConf)
      .build()

    application
  }

  override protected def beforeAll(): Unit = {
    startMongo()

    //    startKafka()
  }

  override protected def afterAll(): Unit = {
    stopMongo()

    //    stopKafka()
  }

  val kafkaTopic = "nio-consent-events"

  private def customConf: Map[String, String] = {
    val mongoUrl = "mongodb://localhost:" + getMongoPort() + "/nio"
    Map(
      "nio.mongo.url" -> mongoUrl,
      "mongodb.uri" -> mongoUrl,
      "tenant.admin.secret" -> "secret",
      "db.flush" -> "true",
      "nio.kafka.port" -> "$kafkaPort",
      "nio.kafka.servers" -> s"127.0.0.1:$kafkaPort",
      "nio.kafka.topic" -> "nio-consent-events",
      "nio.s3ManagementEnabled" -> "true"
    )
  }

  private lazy val actorSystem = ActorSystem("test")
  implicit val materializer = ActorMaterializer()(actorSystem)

  private def consumerSettings =
    ConsumerSettings(actorSystem,
                     new ByteArrayDeserializer,
                     new StringDeserializer)
      .withBootstrapServers(s"127.0.0.1:$kafkaPort")
      .withGroupId(UUID.randomUUID().toString)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def readLastKafkaEvent(): JsValue = {
    Thread.sleep(500)

    import scala.collection.JavaConverters._

    val partition = new TopicPartition(kafkaTopic, 0)
    val partitionToLong: util.Map[TopicPartition, lang.Long] = consumerSettings
      .createKafkaConsumer()
      .endOffsets(List(partition).asJavaCollection)

    val lastOffset: Long = partitionToLong.get(partition)

    val lastEvent: Future[JsValue] = Consumer
      .plainSource(consumerSettings, Subscriptions.assignment(partition))
      .filter(r => r.offset() == (lastOffset - 1))
      .map(_.value())
      .map(Json.parse)
      .take(1)
      .alsoTo(Sink.foreach(v => println(s"read events ${Json.stringify(v)}")))
      .runWith(Sink.last)

    Await.result[JsValue](lastEvent, Duration(10, TimeUnit.SECONDS))
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
