package loader

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.japi.Option.Some
import akka.stream.ActorMaterializer
import configuration.Env
import db._
import models.{NioAccount, Tenant}
import play.api.{Configuration, Logger}
import s3.S3
import utils.{DefaultLoader, SecureEvent, Sha}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

class Starter(config: Configuration,
              env: Env,
              system: ActorSystem,
              tenantDataStore: TenantDataStore,
              organisationDataStore: OrganisationDataStore,
              userDataStore: UserDataStore,
              consentFactDataStore: ConsentFactDataStore,
              lastConsentFactDataStore: LastConsentFactDataStore,
              accountDataStore: AccountDataStore,
              deletionTaskDataStore: DeletionTaskDataStore,
              extractionTaskDataStore: ExtractionTaskDataStore,
              userExtractTaskDataStore: UserExtractTaskDataStore,
              nioAccountDataStore: NioAccountDataStore,
              catchupLockDataStore: CatchupLockDataStore,
              defaultLoader: DefaultLoader,
              s3: S3,
              secureEvent: SecureEvent)(implicit ec: ExecutionContext) {

  def initialize() = {

    implicit val mat: ActorMaterializer = ActorMaterializer()(system)

    // clean up db
    val dbFlush: Boolean = config.get[Boolean]("store.flush")
    if (dbFlush) {
      val tenants = config.get[Seq[String]]("store.tenants")

      Await.result(
        for {
          _ <- tenantDataStore.init()
          _ <- Future.sequence(tenants.map { tenant =>
            tenantDataStore.insert(
              Tenant(tenant, "Default tenant from config file"))
          })
          _ <- Future.sequence(tenants.map {
            tenant =>
              for {
                _ <- organisationDataStore.init(tenant)
                _ <- userDataStore.init(tenant)
                _ <- consentFactDataStore.init(tenant)
                _ <- lastConsentFactDataStore.init(tenant)
                _ <- accountDataStore.init(tenant)
                _ <- deletionTaskDataStore.init(tenant)
                _ <- extractionTaskDataStore.init(tenant)
                _ <- userExtractTaskDataStore.init(tenant)
                _ <- defaultLoader.load(tenant)
              } yield {
                ()
              }
          })
          _ = s3.startExpiredFilesCleaner
        } yield (),
        Duration(60, TimeUnit.SECONDS)
      )
    }

    // Ensure index on different collections
    Await.result(
      for {
        _ <- catchupLockDataStore.init()
        _ <- tenantDataStore.findAll().flatMap {
          tenants =>
            Future.sequence(
              tenants.map {
                t =>
                  Future.sequence(
                    Seq(
                      {
                        Logger.info(s"Ensuring indices for users on ${t.key}")
                        userDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for consents on ${t.key}")
                        consentFactDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for last consents on ${t.key}")
                        lastConsentFactDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for organisations on ${t.key}")
                        organisationDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for accounts on ${t.key}")
                        accountDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for destroy task on ${t.key}")
                        deletionTaskDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for extraction task on ${t.key}")
                        extractionTaskDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for user extract task on ${t.key}")
                        userExtractTaskDataStore.ensureIndices(t.key)
                      }, {
                        Logger.info(
                          s"Ensuring indices for user account ${t.key}")
                        nioAccountDataStore.ensureIndices(t.key)
                      }
                    )
                  )
              }
            )
        }
      } yield (),
      Duration(5, TimeUnit.MINUTES)
    )

    if (env.config.filter.securityMode == "default")
      nioAccountDataStore
        .findMany()
        .flatMap {
          case Nil =>
            val config = env.config.filter.default.defaultUser
            val email: String = config.username
            val password: String = config.password
            Logger.info(
              s"create an admin user with email/password = ( $email : $password )")
            nioAccountDataStore
              .insertOne(
                new NioAccount(
                  email = email,
                  password = Sha.hexSha512(password),
                  isAdmin = true,
                  offerRestrictionPatterns = Some(Seq("*"))
                )
              )
              .map(bool => {
                Logger.info(s"has been created = ( $bool )")
                bool
              })
          case _ =>
            FastFuture.successful(true)
        }
  }

  // Run secure event action to store and secure events
  secureEvent.initialize()

}
