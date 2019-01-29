package service

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import configuration.Env
import db.{
  CatchupLockMongoDatastore,
  ConsentFactMongoDataStore,
  LastConsentFactMongoDataStore,
  TenantMongoDataStore
}
import messaging.KafkaMessageBroker
import models.{ConsentFact, ConsentFactCreated, ConsentFactUpdated}
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.akkastream.State

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

case class Catchup(consentFact: ConsentFact, lastConsentFact: ConsentFact) {
  val isCreation: Boolean = consentFact._id == lastConsentFact._id
}

class CatchupNioEventService(
    tenantMongoDataStore: TenantMongoDataStore,
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    consentFactMongoDataStore: ConsentFactMongoDataStore,
    catchupLockMongoDatastore: CatchupLockMongoDatastore,
    kafkaMessageBroker: KafkaMessageBroker,
    env: Env)(implicit val executionContext: ExecutionContext,
              system: ActorSystem) {

  implicit val materializer = ActorMaterializer()(system)

  def getUnsendConsentFactAsSource(
      tenant: String): Source[Catchup, Future[State]] =
    Source
      .fromFutureSource(
        consentFactMongoDataStore
          .streamByQuery(tenant, Json.obj("sendToKafka" -> false))
      )
      .mapMaterializedValue(_.flatten)
      .mapAsync(1) { consentFact =>
        val eventualMaybeFact: Future[Option[ConsentFact]] =
          lastConsentFactMongoDataStore
            .findOneByQuery(tenant, Json.obj("userId" -> consentFact.userId))

        eventualMaybeFact.map(_.map(r => Catchup(consentFact, r)))
      }
      .mapConcat(_.toList)

  def getUnrelevantConsentFactsAsSource(
      source: Source[Catchup, Future[State]]): Source[Catchup, Future[State]] =
    source
      .filterNot { catchup =>
        catchup.lastConsentFact.lastUpdateSystem
          .isEqual(catchup.consentFact.lastUpdateSystem)
      }

  def getRelevantConsentFactsAsSource(
      tenant: String,
      source: Source[Catchup, Future[State]]): Source[Catchup, Future[State]] =
    source
      .filter { catchup =>
        env.config.kafka.catchUpEventsStrategy == "All" ||
        catchup.lastConsentFact.lastUpdateSystem
          .isEqual(catchup.consentFact.lastUpdateSystem)
      }

  def resendNioEvents(tenant: String,
                      source: Source[Catchup, Future[State]]): Future[Done] = {
    val resendKafkaMessageSink: Sink[Catchup, Future[Done]] =
      Sink.foreach(catchup => {
        val event = if (catchup.isCreation) {
          ConsentFactCreated(tenant = tenant,
                             payload = catchup.lastConsentFact,
                             author = "EVENT_CATCHUP",
                             metadata = None)
        } else {
          ConsentFactUpdated(tenant = tenant,
                             oldValue = catchup.consentFact,
                             payload = catchup.lastConsentFact,
                             author = "EVENT_CATCHUP",
                             metadata = None)
        }

        for {
          _ <- kafkaMessageBroker.publish(event)
          _ <- consentFactMongoDataStore.updateOne(
            tenant,
            catchup.consentFact._id,
            catchup.consentFact.copy(sendToKafka = Some(true)))
        } yield ()
      })

    source
      .watchTermination() { (nu, d) =>
        d.onComplete {
          case Failure(exception) =>
            Logger.error("catchup - relevant - error", exception)
          case _ => ()
        }
        nu
      }
      .runWith(resendKafkaMessageSink)
  }

  def unflagConsentFacts(
      tenant: String,
      source: Source[Catchup, Future[State]]): Future[Done] = {
    val unflagConsentFactSink: Sink[Catchup, Future[Done]] =
      Sink.foreach(
        catchup =>
          consentFactMongoDataStore.updateOne(
            tenant,
            catchup.consentFact._id,
            catchup.consentFact.copy(sendToKafka = Some(true))))

    source
      .watchTermination() { (nu, d) =>
        d.onComplete {
          case Failure(exception) =>
            Logger.error("catchup - unrelevant - error", exception)
          case _ => ()
        }
        nu
      }
      .runWith(unflagConsentFactSink)

  }

  def catchupNioEventScheduler(): Future[Seq[Cancellable]] =
    tenantMongoDataStore.findAll().map { tenants =>
      tenants.map(tenant => {
        Logger.debug(s"start catchup scheduler for tenant ${tenant.key}")
        system.scheduler.schedule(
          1.hour,
          1.hour,
          new Runnable() {
            def run = {
              catchupLockMongoDatastore
                .findLock(tenant.key)
                .map {
                  case Some(_) =>
                    Logger.debug(s"tenant ${tenant.key} already locked")
                  case None =>
                    catchupLockMongoDatastore
                      .createLock(tenant.key)
                      .map {
                        case true =>
                          val source: Source[Catchup, Future[State]] =
                            getUnsendConsentFactAsSource(tenant.key)

                          if (env.config.kafka.catchUpEventsStrategy == "Last") {
                            val unRelevantSource
                              : Source[Catchup, Future[State]] =
                              getUnrelevantConsentFactsAsSource(source)
                            unflagConsentFacts(tenant.key, unRelevantSource)
                          }

                          val relevantSource: Source[Catchup, Future[State]] =
                            getRelevantConsentFactsAsSource(tenant.key, source)

                          resendNioEvents(tenant.key, relevantSource)
                        case false => ()
                      }
                }
            }
          }
        )
      })
    }
}
