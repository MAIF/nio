package messaging

import java.io.File

import akka.actor.ActorSystem
import configuration.KafkaConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

object KafkaSettings {

  import akka.kafka.{ConsumerSettings, ProducerSettings}
  import org.apache.kafka.clients.consumer.ConsumerConfig
  import org.apache.kafka.common.config.SslConfigs
  import org.apache.kafka.common.serialization.ByteArrayDeserializer

  def getFile(path: String): File = new File(path)

  def consumerSettings(
      system: ActorSystem,
      config: KafkaConfig): ConsumerSettings[Array[Byte], String] = {
    val settings =
      ConsumerSettings
        .create(system, new ByteArrayDeserializer, new StringDeserializer())
        .withGroupId(config.groupId.get)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
        .withBootstrapServers(config.servers)

    val s = for {
      ks <- config.keystore.location
      ts <- config.truststore.location
      kp <- config.keyPass
    } yield {
      settings
        .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
        .withProperty(BrokerSecurityConfigs.SSL_CLIENT_AUTH_CONFIG, "required")
        .withProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kp)
        //.withProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getFile(ks).getAbsolutePath)
        .withProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ks)
        .withProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kp)
//        .withProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getFile(ts).getAbsolutePath)
        .withProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ts)
        .withProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kp)
    }

    s.getOrElse(settings)
  }
}
