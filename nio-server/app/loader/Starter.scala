package loader

import java.util.concurrent.TimeUnit

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.util.FastFuture
import org.apache.pekko.japi.Option.Some
import org.apache.pekko.stream.Materializer
import configuration.Env
import db.{UserExtractTaskDataStore, _}
import models.{NioAccount, Tenant}
import play.api.{Configuration}
import s3.S3
import utils.{DefaultLoader, SecureEvent, Sha, NioLogger}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

class Starter(
    config: Configuration,
    env: Env,
    system: ActorSystem,
    tenantDataStore: TenantMongoDataStore,
    organisationDataStore: OrganisationMongoDataStore,
    userDataStore: UserMongoDataStore,
    consentFactDataStore: ConsentFactMongoDataStore,
    lastConsentFactDataStore: LastConsentFactMongoDataStore,
    accountDataStore: AccountMongoDataStore,
    deletionTaskDataStore: DeletionTaskMongoDataStore,
    extractionTaskDataStore: ExtractionTaskMongoDataStore,
    userExtractTaskDataStore: UserExtractTaskDataStore,
    userAccountMongoDataStore: NioAccountMongoDataStore,
    catchupLockMongoDatastore: CatchupLockMongoDatastore,
    defaultLoader: DefaultLoader,
    s3: S3,
    secureEvent: SecureEvent)(implicit val executionContext: ExecutionContext) {

  def initialize() = {

    Materializer(system)

    // clean up db
    val dbFlush: Boolean = config.get[Boolean]("db.flush")
    if (dbFlush) {
      val tenants = config.get[Seq[String]]("db.tenants")

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
        _ <- catchupLockMongoDatastore.init()
        _ <- tenantDataStore.findAll().flatMap {
          tenants =>
            Future.sequence(
              tenants.map { t =>
                  Future.sequence(
                    Seq(
                      {
                        NioLogger.info(s"Ensuring indices for users on ${t.key}")
                        userDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for consents on ${t.key}")
                        consentFactDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for last consents on ${t.key}")
                        lastConsentFactDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for organisations on ${t.key}")
                        organisationDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for accounts on ${t.key}")
                        accountDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for destroy task on ${t.key}")
                        deletionTaskDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for extraction task on ${t.key}")
                        extractionTaskDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for user extract task on ${t.key}")
                        userExtractTaskDataStore.ensureIndices(t.key)
                      }, {
                        NioLogger.info(
                          s"Ensuring indices for user account ${t.key}")
                        userAccountMongoDataStore.ensureIndices(t.key)
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
      Await.result(
        userAccountMongoDataStore
          .findMany()
          .flatMap {
            case Nil =>
              val config = env.config.filter.default.defaultUser

              val email: String = config.username
              val password: String = config.password

              NioLogger.info(
                s"create an admin user with email/password = ( $email : $password )")

              userAccountMongoDataStore.insertOne(
                new NioAccount(
                  email = email,
                  password = Sha.hexSha512(password),
                  isAdmin = true,
                  offerRestrictionPatterns = Some(Seq("*"))
                )
              )
            case _ =>
              FastFuture.successful(true)
          },
        Duration(5, TimeUnit.SECONDS)
      )
  }

  // Run secure event action to store and secure events
  secureEvent.initialize()

}
