package db

import java.util.concurrent.TimeUnit

import models.{Organisation, Permission, PermissionGroup, VersionInfo}
import java.time.{LocalDateTime, Clock}
import utils.{DateUtils, TestUtils}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class OrganisationMongoDataStoreSpec extends TestUtils {

  "store" should {
    "insert" in {
      val ds: OrganisationMongoDataStore = nioComponents.organisationDataStore

      val org1Key = "org1Draft"

      val org1 = Organisation(
        key = org1Key,
        label = "lbl",
        version = VersionInfo("DRAFT", 1, false, Some(true)),
        groups = Seq(
          PermissionGroup("group1",
                          "blabla",
                          Seq(Permission("sms", "Please send sms"))))
      )

      val org2Key = "org2Draft"

      val beforeInsert =
        LocalDateTime.now(Clock.systemUTC).format(DateUtils.utcDateFormatter)

      Await.result(
        for {
          _ <- ds.insert(tenant, org1)
          draft1 <- ds.findDraftByKey(tenant, org1Key)

          afterInsert = LocalDateTime
            .now(Clock.systemUTC())
            .format(DateUtils.utcDateFormatter)

          _ = Thread.sleep(1000)

          org2 = Organisation(
            key = org2Key,
            label = "lbl",
            version = VersionInfo("DRAFT", 1, false, Some(true)),
            groups = Seq(
              PermissionGroup("group1",
                              "blabla",
                              Seq(Permission("sms", "Please send sms"))))
          )

          _ <- ds.insert(tenant, org2)
          draft2 <- ds.findDraftByKey(tenant, org2Key)

          allBetweenInterval <- ds.findAllLatestReleasesOrDraftsByDate(
            tenant,
            beforeInsert,
            afterInsert)

          all <- ds.findAll(tenant)
        } yield {
          draft1.isDefined mustBe true
          draft1.get.key mustBe org1.key

          allBetweenInterval.exists(_.key == org2.key) mustBe false

          all.exists(_.key == org1.key) mustBe true
          all.exists(_.key == org2.key) mustBe true
        },
        Duration(10, TimeUnit.SECONDS)
      )
    }
  }
}
