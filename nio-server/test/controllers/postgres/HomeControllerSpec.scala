package controllers.postgres

import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.{TestUtilsMongo, TestUtilsPostgres}

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class HomeControllerSpec extends TestUtilsPostgres {

  "HomeController GET" should {

    "render the index page from the application" in {

      val response: WSResponse =
        callJson(s"/$tenant/bo", GET, api = false)

      response.status must be(OK)
      response.contentType must be("text/html; charset=UTF-8")
    }

  }
}
