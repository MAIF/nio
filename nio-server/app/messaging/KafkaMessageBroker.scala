package messaging

import java.io.Closeable
import java.security.MessageDigest

import akka.{Done, NotUsed}
import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.Subscriptions
import akka.stream.Materializer
import akka.stream.scaladsl.Keep.both
import akka.stream.scaladsl.{Flow, Source}
import configuration.{Env, KafkaConfig}
import models.{Digest, NioEvent, SecuredEvent}
import org.apache.kafka.clients.producer.{
  Callback,
  KafkaProducer,
  ProducerRecord,
  RecordMetadata
}
import org.apache.kafka.common.TopicPartition
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import utils.{S3ExecutionContext, S3Manager}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure
import scala.util.control.NonFatal
import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

@Singleton
class KafkaMessageBroker @Inject()(actorSystem: ActorSystem)(
    implicit context: ExecutionContext,
    env: Env,
    s3Manager: S3Manager)
    extends Closeable {

  implicit val s3ExecutionContext: S3ExecutionContext = S3ExecutionContext(
    actorSystem.dispatchers.lookup("S3-dispatcher"))

  private lazy val kafka: KafkaConfig = env.config.kafka

  private lazy val producerSettings =
    KafkaSettings.producerSettings(actorSystem, kafka)

  private lazy val producer: KafkaProducer[String, String] =
    producerSettings.createKafkaProducer

  private lazy val consumerSettings =
    KafkaSettings.consumerSettings(actorSystem, kafka)

  private lazy val partitions: Seq[TopicPartition] = consumerSettings
    .createKafkaConsumer()
    .partitionsFor(kafka.topic)
    .asScala
    .map { t =>
      Logger.info(
        s"------> Found topic: ${kafka.topic} partition: ${t.partition()}")
      new TopicPartition(kafka.topic, t.partition())
    }

  private def publishEvent(event: NioEvent): Future[RecordMetadata] = {
    val promise = Promise[RecordMetadata]
    val json = event.asJson.toString()
    try {
      Logger.debug(s"Publishing event $json")
      val record =
        new ProducerRecord[String, String](kafka.topic, event.shardId, json)
      producer.send(record, callback(promise))
    } catch {
      case NonFatal(e) => promise.failure(e)
    }

    promise.future
  }

  def publish(event: NioEvent): Unit = {
    if (env.config.recordManagementEnabled) {
      val fut = publishEvent(event)
      fut.onComplete {
        case Failure(e) =>
          Logger.error(s"Error sending message ${event.asJson.toString()}", e)
        case _ =>
      }
    }
  }

  def events(tenant: String,
             lastEventId: Option[Long] = None): Source[NioEvent, NotUsed] = {
    val lastDate: Long = System.currentTimeMillis() - (1000 * 60 * 60 * 24)

    val topicsAndDate =
      Subscriptions.assignmentOffsetsForTimes(partitions.map(_ -> lastDate): _*)

    Logger.info("----> topicsAndDate " + topicsAndDate)

    Consumer
      .plainSource[Array[Byte], String](consumerSettings, topicsAndDate)
      .map(_.value())
      .via(toNioEvent)
      .filter(event => event.tenant == tenant)
      .via(dropUntilLastId(lastEventId))
      .mapMaterializedValue(_ => NotUsed)
  }

  private val toNioEvent: Flow[String, NioEvent, NotUsed] = Flow[String]
    .map(Json.parse)
    .mapConcat(json =>
      NioEvent.fromJson(json) match {
        case None =>
          Logger.error(s"Error deserializing event of type ${json \ "type"}")
          List.empty[NioEvent]
        case Some(e) =>
          List(e)
    })

  private def digest(message: String): String = {
    val md: MessageDigest = MessageDigest.getInstance("SHA-512")
    val bytes: Array[Byte] = md.digest(message.getBytes)
    bytes.map(_.toChar).mkString
  }

  def readAllEvents(groupIn: Int, groupDuration: FiniteDuration)(
      implicit m: Materializer)
    : Source[Done, (Consumer.Control, Future[Done])] = {

    val source: Source[Done, (Consumer.Control, Future[Done])] = Consumer
      .committableSource(consumerSettings,
                         Subscriptions.topics(env.config.kafka.topic))
      .groupBy(partitions.size, _.record.partition())
      .groupedWithin(groupIn, groupDuration)
      .filter(_.nonEmpty)
      .mapAsync(1) { messages =>
        val message: String = messages
          .map(_.record)
          .map(_.value())
          .mkString("\n")
        Logger.info(s"message readed $message")
        val digestValue: String = digest(message)
        s3Manager
          .addFile(digestValue, message + "\n" + digestValue)
          .flatMap { _ =>
            publishEvent(SecuredEvent(payload = Digest(digestValue))) // TODO : choose partition
          }
          .flatMap { _ =>
            val batchFold =
              messages
                .map(_.committableOffset)
                .foldLeft(CommittableOffsetBatch.empty) { (batch, elem) =>
                  batch.updated(elem)
                }

            batchFold.commitScaladsl()

          }
      }
      .mergeSubstreams
      .watchTermination()(both)

    source
  }

  override def close() =
    producer.close()

  private def dropUntilLastId(
      lastId: Option[Long]): Flow[NioEvent, NioEvent, NotUsed] =
    lastId.map { id =>
      Flow[NioEvent].filter(_.id > id)
    } getOrElse {
      Flow[NioEvent]
    }

  private def callback(promise: Promise[RecordMetadata]) = new Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception) =
      if (exception != null) {
        promise.failure(exception)
      } else {
        promise.success(metadata)
      }
  }

}
