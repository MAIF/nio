package utils

import javax.inject.Inject

import db.OrganisationMongoDataStore
import models._
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class DefaultLoader @Inject()(conf: Configuration,
                              organisationStore: OrganisationMongoDataStore)(
    implicit ec: ExecutionContext) {

  def load(tenant: String) = {
    val organisations = conf.get[Seq[Configuration]]("organisations").map {
      orgConfig =>
        Organisation(
          _id = orgConfig.get[String]("_id"),
          key = orgConfig.get[String]("key"),
          label = orgConfig.get[String]("label"),
          version = VersionInfo(
            status = orgConfig.get[String]("version.status"),
            num = orgConfig.get[Int]("version.num"),
            latest = orgConfig.get[Boolean]("version.latest"),
            neverReleased =
              orgConfig.getOptional[Boolean]("version.neverReleased")
          ),
          groups =
            orgConfig.get[Seq[Configuration]]("groups").map { groupsConf =>
              PermissionGroup(
                key = groupsConf.get[String]("key"),
                label = groupsConf.get[String]("label"),
                permissions =
                  groupsConf.get[Seq[Configuration]]("permissions").map {
                    permissionsConf =>
                      Permission(
                        key = permissionsConf.get[String]("key"),
                        label = permissionsConf.get[String]("label")
                      )
                  }
              )
            }
        )
    }

    println("Loading default data set ...")
    Future.sequence(
      organisations.map { o =>
        organisationStore.insert(tenant, o)
      }
    )
  }
}
