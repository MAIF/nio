package messaging

import java.io.Closeable
import java.security.MessageDigest
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ConsumerMessage.CommittableOffsetBatch
import org.apache.pekko.kafka.{CommitterSettings, ConsumerMessage, ProducerSettings, Subscriptions}
import org.apache.pekko.kafka.scaladsl.{Committer, Consumer}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Keep.both
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import org.apache.pekko.{Done, NotUsed}
import configuration.{Env, KafkaConfig}
import models.{Digest, NioEvent, SecuredEvent}
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, Producer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import utils.NioLogger
import play.api.libs.json.Json
import utils.{FSManager, S3ExecutionContext}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.collection.{Seq, immutable}

class KafkaMessageBroker(actorSystem: ActorSystem)(implicit context: ExecutionContext, env: Env, s3Manager: FSManager)
    extends Closeable {

  implicit val s3ExecutionContext: S3ExecutionContext = S3ExecutionContext(
    actorSystem.dispatchers.lookup("S3-dispatcher")
  )

  private lazy val kafka: KafkaConfig = env.config.kafka

  lazy val producerSettings: ProducerSettings[String, String] =
    KafkaSettings.producerSettings(actorSystem, kafka)

  private lazy val producer: Producer[String, String] =
    producerSettings.createKafkaProducer()

  private lazy val consumerSettings =
    KafkaSettings.consumerSettings(actorSystem, kafka)

  private lazy val partitions: Seq[TopicPartition] = consumerSettings
    .createKafkaConsumer()
    .partitionsFor(kafka.topic)
    .asScala
    .map { t =>
      NioLogger.info(s"------> Found topic: ${kafka.topic} partition: ${t.partition()}")
      new TopicPartition(kafka.topic, t.partition())
    }
    .toSeq

  private def publishEvent(event: NioEvent): Future[RecordMetadata] = {
    val promise = Promise[RecordMetadata]()
    val json    = event.asJson().toString()
    try {
      NioLogger.info(s"Publishing event $json")
      val record =
        new ProducerRecord[String, String](kafka.topic, event.shardId, json)
      producer.send(record, callback(promise))
    } catch {
      case NonFatal(e) => promise.failure(e)
    }

    promise.future
  }

  def publish(event: NioEvent): Future[Unit] =
    if (env.config.recordManagementEnabled) {
      publishEvent(event).map(_ => ())
//      fut.onComplete {
//        case Failure(e) =>
//          NioLogger.error(s"Error sending message ${event.asJson().toString()}", e)
//        case _ =>
//      }
    } else {
      Future.successful(())
    }

  def events(tenant: String, lastEventId: Option[Long] = None): Source[NioEvent, NotUsed] = {
    val lastDate: Long = System.currentTimeMillis() - (1000 * 60 * 60 * 24)

    val assignements: Map[TopicPartition, Long] = partitions.map(p => p -> lastDate).toMap
    val topicsAndDate                           =
      Subscriptions.assignmentOffsetsForTimes(assignements)

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
        case None    =>
          NioLogger.error(s"Error deserializing event of type ${json \ "type"}")
          List.empty[NioEvent]
        case Some(e) =>
          List(e)
      }
    )

  private def digest(message: String): String = {
    val md: MessageDigest  = MessageDigest.getInstance("SHA-512")
    val bytes: Array[Byte] = md.digest(message.getBytes)
    bytes.map(_.toChar).mkString
  }

  def readAllEvents(groupIn: Int, groupDuration: FiniteDuration)(implicit
      m: Materializer
  ): Source[Done, (Consumer.Control, Future[Done])] = {

    val committerFlow = Committer.batchFlow(CommitterSettings(actorSystem))
    val source: Source[Done, (Consumer.Control, Future[Done])] = Consumer
      .committablePartitionedSource(consumerSettings, Subscriptions.topics(env.config.kafka.topic))
      .flatMapMerge(partitions.size, { case (_, messagesStream) =>
        messagesStream
          .groupedWithin(groupIn, groupDuration)
          .filter(_.nonEmpty)
          .mapAsync(1) { messages =>
            val message: String = messages
              .map(_.record)
              .map(_.value())
              .mkString("\n")
            NioLogger.info(s"message readed $message")
            val digestValue: String = digest(message)
            s3Manager
              .addFile(digestValue, message + "\n" + digestValue)
              .flatMap { _ =>
                publishEvent(SecuredEvent(payload = Digest(digestValue))) // TODO : choose partition
              }
              .map(_ => messages.map(_.committableOffset))
          }
          .mapConcat(_.toList)
          .via(committerFlow)
          .map(_ => Done)
      })
      .watchTermination()(both)

    source
  }

  override def close() =
    producer.close()

  private def dropUntilLastId(lastId: Option[Long]): Flow[NioEvent, NioEvent, NotUsed] =
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
