package utils

import db.OrganisationMongoDataStore
import models._
import play.api.Configuration

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DefaultLoader(conf: Configuration, organisationStore: OrganisationMongoDataStore)(implicit ec: ExecutionContext) {

  def load(tenant: String) = {
    val organisations = conf.get[Seq[Configuration]]("organisations").map { orgConfig =>
      Organisation(
        _id = orgConfig.get[String]("_id"),
        key = orgConfig.get[String]("key"),
        label = orgConfig.get[String]("label"),
        version = VersionInfo(
          status = orgConfig.get[String]("version.status"),
          num = orgConfig.get[Int]("version.num"),
          latest = orgConfig.get[Boolean]("version.latest"),
          neverReleased = orgConfig.getOptional[Boolean]("version.neverReleased")
        ),
        groups = orgConfig.get[Seq[Configuration]]("groups").map { groupsConf =>
          PermissionGroup(
            key = groupsConf.get[String]("key"),
            label = groupsConf.get[String]("label"),
            permissions = groupsConf.get[Seq[Configuration]]("permissions").map { permissionsConf =>
              Permission(
                key = permissionsConf.get[String]("key"),
                label = permissionsConf.get[String]("label"),
                `type` = permissionsConf.getOptional[String]("type").flatMap(s => PermissionType.parse(s).toOption).getOrElse(OptIn),
                validityPeriod = permissionsConf.getOptional[FiniteDuration]("validityPeriod")
              )
            }
          )
        }
      )
    }

    NioLogger.info("Loading default data set ...")
    Future.sequence(
      organisations.map { o =>
        organisationStore.insert(tenant, o)
      }
    )
  }
}
