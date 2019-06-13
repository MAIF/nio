package db.postgres

import java.util.concurrent.TimeUnit

import db.OrganisationDataStore
import models.{Organisation, Permission, PermissionGroup, VersionInfo}
import org.joda.time.{DateTime, DateTimeZone}
import utils.{DateUtils, TestUtilsMongo, TestUtilsPostgres}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class OrganisationPostgresDataStoreSpec extends TestUtilsPostgres {

  "store" should {
    "insert" in {
      val ds: OrganisationDataStore = nioComponents.organisationDataStore

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
        DateTime.now(DateTimeZone.UTC).toString(DateUtils.utcDateFormatter)

      Await.result(
        for {
          _ <- ds.insert(tenant, org1)
          draft1 <- ds.findDraftByKey(tenant, org1Key)

          afterInsert = DateTime
            .now(DateTimeZone.UTC)
            .toString(DateUtils.utcDateFormatter)

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
