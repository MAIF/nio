package controllers

import play.api.test.Helpers._
import play.api.libs.ws.WSResponse
import utils.TestUtils

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class HomeControllerSpec extends TestUtils {

  "HomeController GET" should {

    "render the index page from the application" in {

      val response: WSResponse =
        callJson("/prod1/bo", GET, api = false)

      response.status must be(OK)
      response.contentType must be("text/html; charset=UTF-8")
    }

  }
}
