package controllers

import models.{Organisation, Permission, PermissionGroup, UserExtract}
import play.api.test.Helpers._
import utils.TestUtils

class UserExtractControllerSpec extends TestUtils {

  val orgKey: String = s"org1"
  val userId: String = "user1"
  val userIdXml: String = "user1Xml"

  val org1 = Organisation(
    key = orgKey,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "blalba",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val userExtract = UserExtract("user@nio.fr")

  "UserExtractController JSON" should {

    "ask an extraction" in {
      val path: String = s"/$tenant/organisations"
      val createResponse = postJson(path, org1.asJson)
      createResponse.status mustBe CREATED

      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract",
                 userExtract.asJson())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract",
                 userExtract.asJson())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postJson(s"/$tenant/organisations/org2/users/$userId/_extract",
                 userExtract.asJson())
      userExtractStatus.status mustBe NOT_FOUND
    }

  }

  "UserExtractController XML" should {

    "ask an extraction" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract",
                userExtract.asXml())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userIdXml
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract",
                userExtract.asXml())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/org2/users/$userIdXml/_extract",
                userExtract.asXml())
      userExtractStatus.status mustBe NOT_FOUND
    }
  }

}
