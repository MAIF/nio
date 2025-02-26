package utils

import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.{lang, util}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import com.amazonaws.services.s3.model.PutObjectResult
import com.typesafe.config.{Config, ConfigFactory}
import filters.AuthInfoMock
import models.Tenant
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.scalatest._
import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.{BaseOneServerPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers._
import play.api.{Application, ApplicationLoader, Configuration, Environment}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import service.ConsentManagerService

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.Elem
import play.api.libs.ws.DefaultBodyWritables._

class MockS3Manager extends FSManager {
  override def addFile(key: String, content: String)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Future[PutObjectResult] =
    Future {
      null
    }
}

trait TestUtils
    extends PlaySpec
    with BaseOneServerPerSuite
    with FakeApplicationFactory
    with AnyWordSpecLike
    with must.Matchers
    with OptionValues
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  protected implicit val actorSystem: ActorSystem = ActorSystem("test")
  protected implicit val materializer: Materializer = Materializer(actorSystem)

  def kafkaPort: Int                    = 9092
  def mongoPort: Int                    = 27018
  def tenant: String                    = "test"
  protected lazy val serverHost: String = s"http://localhost:${this.port}"
  protected lazy val apiPath: String    = s"$serverHost/api"

  private val jsonHeaders: Seq[(String, String)] = Seq(
    ACCEPT       -> JSON,
    CONTENT_TYPE -> JSON
  )

  private val xmlHeaders: Seq[(String, String)] = Seq(
    ACCEPT       -> XML,
    CONTENT_TYPE -> XML
  )

  def extraConfig(): Config = {
    val mongoUrl = s"mongodb://localhost:$mongoPort/nio-test"
    ConfigFactory
      .parseString(s"""
                      |nio.mongo.url="$mongoUrl"
		       |nio.db.batchSize=1
                      |mongodb.uri="$mongoUrl"
                      |tenant.admin.secret="secret"
                      |db.flush=true
                      |nio.s3Config.v4Auth="false"
                      |nio.kafka.port=$kafkaPort
                      |nio.kafka.servers="127.0.0.1:$kafkaPort"
                      |nio.kafka.topic="$kafkaTopic"
                      |nio.kafka.eventsGroupIn=10000
                      |nio.s3ManagementEnabled=false
                      |nio.mailSendingEnable=false
                      |db.tenants=["$tenant"]
                      |nio.filter.securityMode="default"
       """.stripMargin)
      .resolve().withFallback(ConfigFactory.load("dataset.conf"))
  }

  protected lazy val authInfo: AuthInfoMock = new AuthInfoTest

  protected lazy val nioComponents: NioSpec =
    new NioSpec(getContext, Some(authInfo))

  protected def ws: WSClient = nioComponents.wsClient

  protected lazy val consentManagerService: ConsentManagerService =
    nioComponents.consentManagerService

  private def getContext: ApplicationLoader.Context = {
    val env           = Environment.simple()
    val configuration = Configuration.load(env)

    ApplicationLoader.Context(
      environment = env,
      devContext = None,
      initialConfiguration = Configuration(extraConfig().withFallback(configuration.underlying).resolve()),
      lifecycle = new DefaultApplicationLifecycle()
    )
  }

  override def fakeApplication(): Application =
    new NioTestLoader(Some(authInfo)).load(getContext)

  override protected def beforeAll(): Unit = {}
  override protected def afterAll(): Unit     = cleanAll()

  protected def cleanAll(): Unit = {
    implicit val executionContext: ExecutionContext =
      nioComponents.executionContext
    val reactiveMongoApi: ReactiveMongoApi          = nioComponents.reactiveMongoApi

    import play.api.libs.json._
    import reactivemongo.play.json.compat._

    def delete(query: JsObject)(coll: BSONCollection): Future[Boolean] = {
      import json2bson._
      val builder = coll.delete(ordered = false)
      builder
        .element(q = query, limit = None, collation = None)
        .flatMap(d => builder.many(List(d)))
        .map(_.writeErrors.isEmpty)
    }

    // clean mongo data
    val cleanUp = for {
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-userExtractTask").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-accounts").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-consentFacts").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-lastConsentFacts").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-deletionTasks").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-extractionTasks").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-organisations").flatMap(delete(Json.obj()))
      _                 <- getStoredCollection(reactiveMongoApi, s"$tenant-users").flatMap(delete(Json.obj()))
      tenantsCollection <- getStoredCollection(reactiveMongoApi, "tenants")
      _                 <- delete(Json.obj("key" -> tenant))(tenantsCollection)
      _                 <- delete(Json.obj("key" -> "newTenant"))(tenantsCollection)
      _                 <- delete(Json.obj("key" -> "testTenantXml"))(tenantsCollection)
      _                 <- delete(Json.obj("key" -> "testTenantJson"))(tenantsCollection)
      _                 <- delete(Json.obj("key" -> "newTenant1"))(tenantsCollection)
      _                 <- delete(Json.obj("key" -> "newTenantAlreadyExist"))(tenantsCollection)
    } yield ()

    Await.result(cleanUp, 5 seconds span)
  }

  def getStoredCollection(reactiveMongoApi: ReactiveMongoApi, collectionName: String)(implicit
      ec: ExecutionContext
  ): Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection(collectionName))

  val kafkaTopic = "test-nio-consent-events"

  private def consumerSettings: ConsumerSettings[Array[Byte], String] =
    ConsumerSettings(actorSystem, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(s"127.0.0.1:$kafkaPort")
      .withGroupId(UUID.randomUUID().toString)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def readLastKafkaEvent(): JsValue = {
    Thread.sleep(500)

    import scala.jdk.CollectionConverters._

    val partition                                            = new TopicPartition(kafkaTopic, 0)
    val partitionToLong: util.Map[TopicPartition, lang.Long] = consumerSettings
      .createKafkaConsumer()
      .endOffsets(List(partition).asJava)

    val lastOffset: Long = partitionToLong.get(partition)

    val lastEvent: Future[JsValue] = Consumer
      .plainSource(consumerSettings, Subscriptions.assignment(partition))
      .filter(r => r.offset() == (lastOffset - 1))
      .map(_.value())
      .map(Json.parse)
      .take(1)
      .alsoTo(Sink.foreach(v => println(s"read events ${Json.stringify(v)}")))
      .runWith(Sink.last)

    Await.result[JsValue](lastEvent, Duration(30, TimeUnit.SECONDS))
  }

  def readLastNKafkaEvents(n: Int): Seq[JsValue] = {
    Thread.sleep(1000)

    val partition = new TopicPartition(kafkaTopic, 0)

    import scala.jdk.CollectionConverters._

    val partitionToLong: util.Map[TopicPartition, lang.Long] = consumerSettings
      .createKafkaConsumer()
      .endOffsets(List(partition).asJava)

    val lastOffset: Long = partitionToLong.get(partition)

    val topicsAndDate                    =
      Subscriptions.assignmentWithOffset(new TopicPartition(kafkaTopic, 0) -> (lastOffset - n))
    val lastEvents: Future[Seq[JsValue]] = Consumer
      .plainSource[Array[Byte], String](consumerSettings, topicsAndDate)
      .map { r =>
        Json.parse(r.value())
      }
      .take(n)
      .runWith(Sink.seq)

    Await.result[Seq[JsValue]](lastEvents, Duration(10, TimeUnit.SECONDS))
  }

  protected def reinitTenant(): Unit = {
    val currentTenant = getJson(s"/tenants/$tenant")
    if (currentTenant.status == NOT_FOUND) {
      postJson(s"/tenants", Json.toJson(Tenant(tenant, "Test")), Seq("tenant-admin-secret" -> "secret"))
    }
  }

  private def callByType[T: BodyWritable](
      path: String,
      httpVerb: String,
      body: T = null,
      api: Boolean = true,
      headers: Seq[(String, String)] = Seq(
        ACCEPT       -> JSON,
        CONTENT_TYPE -> JSON
      )
  ): WSResponse = {

    val suffix         = if (api) apiPath else serverHost
    val futureResponse = httpVerb match {
      case GET    =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers*)
          .get()
      case DELETE =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers*)
          .delete()
      case POST   =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers*)
          .post(body)
      case PUT    =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers*)
          .put(body)
      case PATCH    =>
        ws.url(s"$suffix$path")
          .withHttpHeaders(headers*)
          .patch(body)
      case _      =>
        Future.failed(new IllegalArgumentException(s"Unknown http verb: $httpVerb"))
    }
    Await.result[WSResponse](futureResponse, Duration(10, TimeUnit.SECONDS))
  }

  def postJson(path: String, body: JsValue, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path = path, httpVerb = POST, body = body, headers = headers)
  }

  def postText(path: String, body: String, headers: Seq[(String, String)] = jsonHeaders): WSResponse =
    callByType[String](path = path, httpVerb = POST, body = body, headers = headers)

  def postBinaryFile(path: String, body: File, api: Boolean = true, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    val suffix         = if (api) apiPath else serverHost
    val futureResponse = ws
      .url(s"$suffix$path")
      .withHttpHeaders(headers*)
      .post(body)

    Await.result[WSResponse](futureResponse, Duration(10, TimeUnit.SECONDS))
  }

  def getJson(path: String, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path = path, httpVerb = GET, headers = headers)
  }

  def putJson(path: String, body: JsValue, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path = path, httpVerb = PUT, body = body, headers = headers)
  }

  def patchJson(path: String, body: JsValue, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path = path, httpVerb = PATCH, body = body, headers = headers)
  }

  def delete(path: String, headers: Seq[(String, String)] = jsonHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path = path, httpVerb = DELETE, headers = headers)
  }

  def postXml(path: String, body: Elem, headers: Seq[(String, String)] = xmlHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_NodeSeq
    callByType[Elem](path = path, httpVerb = POST, body = body, headers = headers)
  }

  def getXml(path: String, headers: Seq[(String, String)] = xmlHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_NodeSeq
    callByType[Elem](path = path, httpVerb = GET, headers = headers)
  }

  def putXml(path: String, body: Elem, headers: Seq[(String, String)] = xmlHeaders): WSResponse = {
    import play.api.libs.ws.writeableOf_NodeSeq
    callByType[Elem](path = path, httpVerb = PUT, body = body, headers = headers)
  }

  protected def callJson(
      path: String,
      httpVerb: String,
      body: JsValue = null,
      api: Boolean = true,
      headers: Seq[(String, String)] = Seq(
        ACCEPT       -> JSON,
        CONTENT_TYPE -> JSON
      )
  ): WSResponse = {
    import play.api.libs.ws.writeableOf_JsValue
    callByType[JsValue](path, httpVerb, body, api, headers)
  }

  protected def callXml(
      path: String,
      httpVerb: String,
      body: Elem = null,
      api: Boolean = true,
      headers: Seq[(String, String)] = Seq(
        ACCEPT       -> XML,
        CONTENT_TYPE -> XML
      )
  ): WSResponse = {
    import play.api.libs.ws.writeableOf_NodeSeq
    callByType[Elem](path, httpVerb, body, api, headers)
  }
}
