package controllers

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtils

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class EventControllerSpec extends TestUtils {
  val tenant: String = "newTenant1"

  val secret = "tenant-admin-secret" -> "secret"

  val tenant1AsJson = Json.obj(
    "key" -> tenant,
    "description" -> "a new tenant"
  )

  val jsonHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON, secret)

  "EventController" should {

    "listen events, create a new tenant then check that the received event is valid" in {
      val system = app.injector.instanceOf[ActorSystem]

      implicit val materializer = ActorMaterializer()(system)

      val response: WSResponse =
        callJson("/tenants", POST, tenant1AsJson, headers = jsonHeaders)

      response.status mustBe CREATED

      response.contentType.contains("json") mustBe true

      Thread.sleep(2000)

      val isOk = new AtomicBoolean(false)

      val fut =
        ws.url(s"$apiPath/$tenant/events")
          .withHttpHeaders(jsonHeaders: _*)
          .withMethod("GET")
          .withRequestTimeout(Duration.Inf)
          .stream()
          .flatMap { response =>
            response.bodyAsSource.runForeach { t =>
              val line = t.utf8String
              val json = line.split(": ")(1)

              (Json.parse(json) \ "tenant").asOpt[String] match {
                case Some(x) if x == tenant =>
                  isOk.set(true)
                case _ => isOk.set(false)
              }
            }
          }

      Thread.sleep(2000)

      isOk.get() mustBe true
    }

  }

}
