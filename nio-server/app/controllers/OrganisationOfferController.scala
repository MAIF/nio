package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import auth.AuthAction
import db.OrganisationMongoDataStore
import messaging.KafkaMessageBroker
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class OrganisationOfferController(
    val authAction: AuthAction,
    val cc: ControllerComponents,
    val organisationMongoDataStore: OrganisationMongoDataStore,
    val kafkaMessageBroker: KafkaMessageBroker)(
    implicit val ec: ExecutionContext,
    actorSystem: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val materializer = ActorMaterializer()(actorSystem)

}
