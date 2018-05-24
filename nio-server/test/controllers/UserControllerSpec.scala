package controllers

import models._
import play.api.libs.json.JsArray
import play.api.test.Helpers._
import utils.TestUtils

class UserControllerSpec extends TestUtils {

  val tenant: String = "sandbox"
  val org1key: String = "maif1"
  val org2key: String = "maif2"
  val userId1: String = "userId1"
  val userId2: String = "userId2"
  val userId3: String = "userId3"

  val org1 = Organisation(
    key = org1key,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "blalba",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )
  val org1AsJson = org1.asJson

  val org2 = Organisation(
    key = org2key,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "blalba",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )
  val org2AsJson = org2.asJson

  val u1 = ConsentFact(
    userId = userId1,
    doneBy = DoneBy(userId = userId1, role = "USER"),
    version = 1,
    groups = Seq(
      ConsentGroup(
        key = "group1",
        label = "blalba",
        consents = Seq(
          Consent(key = "sms", label = "Please accept sms", checked = true))))
  )
  val user1Org1AsJson = u1.asJson

  val u2 = ConsentFact(
    userId = userId2,
    doneBy = DoneBy(userId = userId2, role = "USER"),
    version = 1,
    groups = Seq(
      ConsentGroup(
        key = "group1",
        label = "blalba",
        consents = Seq(
          Consent(key = "sms", label = "Please accept sms", checked = true))))
  )
  val user2Org2AsJson = u2.asJson

  val u3 = ConsentFact(
    userId = userId3,
    doneBy = DoneBy(userId = userId3, role = "USER"),
    version = 1,
    groups = Seq(
      ConsentGroup(
        key = "group1",
        label = "blalba",
        consents = Seq(
          Consent(key = "sms", label = "Please accept sms", checked = true))))
  )
  val user3Org2AsJson = u3.asJson

  "UserController" should {

    "list all users" in {
      val createOrg1 = postJson(s"/$tenant/organisations", org1AsJson)
      createOrg1.status mustBe CREATED

      val releaseOrg1 =
        postJson(s"/$tenant/organisations/$org1key/draft/_release", org1AsJson)
      releaseOrg1.status mustBe OK

      val createOrg2 = postJson(s"/$tenant/organisations", org2AsJson)
      createOrg2.status mustBe CREATED

      val releaseOrg2 =
        postJson(s"/$tenant/organisations/$org2key/draft/_release", org2AsJson)
      releaseOrg2.status mustBe OK

      val createUser1 =
        putJson(s"/$tenant/organisations/$org1key/users/$userId1",
                user1Org1AsJson)

      createUser1.status mustBe OK

      val createUser2 =
        putJson(s"/$tenant/organisations/$org2key/users/$userId2",
                user2Org2AsJson)

      createUser2.status mustBe OK

      val response = getJson(s"/$tenant/users")

      response.status mustBe OK

      (response.json \ "count").as[Int] mustBe 2
      (response.json \ "page").as[Int] mustBe 0
      (response.json \ "pageSize").as[Int] mustBe 10

      val value: JsArray = (response.json \ "items").as[JsArray]

      value.value.size mustBe 2
      (value \ 0 \ "userId").as[String] mustBe userId1
      (value \ 0 \ "orgKey").as[String] mustBe org1key

      (value \ 1 \ "userId").as[String] mustBe userId2
      (value \ 1 \ "orgKey").as[String] mustBe org2key
    }

    "get users by organisation" in {
      val response = getJson(s"/$tenant/organisations/$org1key/users")
      response.status mustBe OK

      (response.json \ "count").as[Int] mustBe 1
      (response.json \ "page").as[Int] mustBe 0
      (response.json \ "pageSize").as[Int] mustBe 10

      val value: JsArray = (response.json \ "items").as[JsArray]

      value.value.size mustBe 1
      (value \ 0 \ "userId").as[String] mustBe userId1
      (value \ 0 \ "orgKey").as[String] mustBe org1key
    }

    "list all users with pagination" in {

      val response = getJson(s"/$tenant/users?page=0&pageSize=1")

      response.status mustBe OK

      (response.json \ "count").as[Int] mustBe 2
      (response.json \ "page").as[Int] mustBe 0
      (response.json \ "pageSize").as[Int] mustBe 1

      val value: JsArray = (response.json \ "items").as[JsArray]

      value.value.size mustBe 1
      (value \ 0 \ "userId").as[String] mustBe userId1
      (value \ 0 \ "orgKey").as[String] mustBe org1key

      val responsePage2 = getJson(s"/$tenant/users?page=1&pageSize=1")

      responsePage2.status mustBe OK

      (responsePage2.json \ "count").as[Int] mustBe 2
      (responsePage2.json \ "page").as[Int] mustBe 1
      (responsePage2.json \ "pageSize").as[Int] mustBe 1

      val valuePage2: JsArray = (responsePage2.json \ "items").as[JsArray]

      valuePage2.value.size mustBe 1
      (valuePage2 \ 0 \ "userId").as[String] mustBe userId2
      (valuePage2 \ 0 \ "orgKey").as[String] mustBe org2key
    }

    "get users by organisation with pagination" in {

      val createUser3 =
        putJson(s"/$tenant/organisations/$org2key/users/$userId3",
                user3Org2AsJson)

      createUser3.status mustBe OK

      val response =
        getJson(s"/$tenant/organisations/$org2key/users?page=0&pageSize=1")
      response.status mustBe OK

      (response.json \ "count").as[Int] mustBe 2
      (response.json \ "page").as[Int] mustBe 0
      (response.json \ "pageSize").as[Int] mustBe 1

      val value: JsArray = (response.json \ "items").as[JsArray]

      value.value.size mustBe 1
      (value \ 0 \ "userId").as[String] mustBe userId2
      (value \ 0 \ "orgKey").as[String] mustBe org2key

      val responsePage2 =
        getJson(s"/$tenant/organisations/$org2key/users?page=1&pageSize=1")
      responsePage2.status mustBe OK

      (responsePage2.json \ "count").as[Int] mustBe 2
      (responsePage2.json \ "page").as[Int] mustBe 1
      (responsePage2.json \ "pageSize").as[Int] mustBe 1

      val valuePage2: JsArray = (responsePage2.json \ "items").as[JsArray]

      valuePage2.value.size mustBe 1
      (valuePage2 \ 0 \ "userId").as[String] mustBe userId3
      (valuePage2 \ 0 \ "orgKey").as[String] mustBe org2key
    }

    "list all users as XML" in {
      val resp =
        callXml(s"/$tenant/users", GET, headers = Seq(ACCEPT -> XML))
      resp.status mustBe OK

      val items = resp.xml \ "items"

      (items \\ "userId")(0).text mustBe userId1
      (items \\ "orgKey")(0).text mustBe org1key

      (items \\ "userId")(1).text mustBe userId2
      (items \\ "orgKey")(1).text mustBe org2key
    }

  }

}
