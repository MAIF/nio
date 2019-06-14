package controllers.mongo

import models.{Account, OrganisationUser}
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtilsMongo

class AccountControllerSpec extends TestUtilsMongo {

  val account1: String = "account1"
  val account2: String = "account2"

  val accountToCreate1: Account = Account(account1,
                                          organisationsUsers = Seq(
                                            OrganisationUser("user1", "org1"),
                                            OrganisationUser("user1", "org2")
                                          ))

  val accountToUpdate1: Account = Account(account1,
                                          organisationsUsers = Seq(
                                            OrganisationUser("user1", "org1"),
                                            OrganisationUser("user1", "org2"),
                                            OrganisationUser("user1", "org3")
                                          ))

  val accountToUpdateError1: Account = Account(
    account2,
    organisationsUsers = Seq(
      OrganisationUser("user1", "org1"),
      OrganisationUser("user1", "org2"),
      OrganisationUser("user1", "org3")
    ))

  "AccountControllerSpec" should {

    "create an account" in {
      val resp: WSResponse =
        postJson(s"/$tenant/accounts", accountToCreate1.asJson())

      resp.status must be(CREATED)
      val accountCreated = resp.json

      (accountCreated \ "accountId").as[String] must be(account1)

      val organisationsUsers: JsArray =
        (accountCreated \ "organisationsUsers").as[JsArray]
      organisationsUsers.value.size must be(2)

      (organisationsUsers \ 0 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 0 \ "orgKey").as[String] must be("org1")

      (organisationsUsers \ 1 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 1 \ "orgKey").as[String] must be("org2")
    }

    "create with an already exist accountId" in {
      val resp: WSResponse =
        postJson(s"/$tenant/accounts", accountToCreate1.asJson())

      resp.status must be(CONFLICT)
    }

    "update an account" in {
      val resp: WSResponse =
        putJson(s"/$tenant/accounts/$account1", accountToUpdate1.asJson())

      resp.status must be(OK)
      val accountCreated = resp.json

      (accountCreated \ "accountId").as[String] must be(account1)

      val organisationsUsers: JsArray =
        (accountCreated \ "organisationsUsers").as[JsArray]
      organisationsUsers.value.size must be(3)

      (organisationsUsers \ 0 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 0 \ "orgKey").as[String] must be("org1")

      (organisationsUsers \ 1 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 1 \ "orgKey").as[String] must be("org2")

      (organisationsUsers \ 2 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 2 \ "orgKey").as[String] must be("org3")
    }

    "update with a wrong accountId" in {
      val resp: WSResponse =
        putJson(s"/$tenant/accounts/$account1", accountToUpdateError1.asJson())

      resp.status must be(BAD_REQUEST)
    }

    "find an account" in {
      val resp: WSResponse = getJson(s"/$tenant/accounts/$account1")

      resp.status must be(OK)
      val account = resp.json

      (account \ "accountId").as[String] must be(account1)

      val organisationsUsers: JsArray =
        (account \ "organisationsUsers").as[JsArray]
      organisationsUsers.value.size must be(3)

      (organisationsUsers \ 0 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 0 \ "orgKey").as[String] must be("org1")

      (organisationsUsers \ 1 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 1 \ "orgKey").as[String] must be("org2")

      (organisationsUsers \ 2 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 2 \ "orgKey").as[String] must be("org3")
    }

    "find an unknow account" in {
      val resp: WSResponse = getJson(s"/$tenant/accounts/$account2")

      resp.status must be(NOT_FOUND)
    }

    "find all accounts" in {
      val resp: WSResponse = getJson(s"/$tenant/accounts")

      resp.status must be(OK)
      val accounts = resp.json.as[JsArray]

      accounts.value.size must be(1)

      val account = (accounts \ 0).as[JsValue]

      (account \ "accountId").as[String] must be(account1)

      val organisationsUsers: JsArray =
        (account \ "organisationsUsers").as[JsArray]
      organisationsUsers.value.size must be(3)

      (organisationsUsers \ 0 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 0 \ "orgKey").as[String] must be("org1")

      (organisationsUsers \ 1 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 1 \ "orgKey").as[String] must be("org2")

      (organisationsUsers \ 2 \ "userId").as[String] must be("user1")
      (organisationsUsers \ 2 \ "orgKey").as[String] must be("org3")
    }

    "delete an account" in {
      delete(s"/$tenant/accounts/$account1").status must be(OK)

      getJson(s"/$tenant/accounts/$account1").status must be(NOT_FOUND)
    }
  }

}
