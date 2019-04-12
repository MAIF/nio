package messaging

import java.io.File

import akka.actor.ActorSystem
import configuration.KafkaConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.Logger

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
        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        .withBootstrapServers(config.servers)

    val s = config.securityProtocol match {
      case Some("SASL_SSL") => for {
        sp <- config.securityProtocol
        sm <- config.saslMechanism
      } yield {
        Logger.debug(s"sasl config ==> ${config.saslJaasConfig}")
        settings
            .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sp)
            .withProperty(SaslConfigs.SASL_MECHANISM, sm)
            .withProperty(SaslConfigs.SASL_JAAS_CONFIG, config.saslJaasConfig)
      }
      case _ => for {
        ks <- config.keystore.location
        ts <- config.truststore.location
        kp <- config.keyPass
      } yield {
        settings
            .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
            .withProperty(BrokerSecurityConfigs.SSL_CLIENT_AUTH_CONFIG, "required")
            .withProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kp)
            .withProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ks)
            .withProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kp)
            .withProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ts)
            .withProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kp)
      }
    }

    s.getOrElse(settings)
  }

  def producerSettings(
      system: ActorSystem,
      config: KafkaConfig): ProducerSettings[String, String] = {
    val settings = ProducerSettings
      .create(system, new StringSerializer(), new StringSerializer())
      .withBootstrapServers(config.servers)

    val s = config.securityProtocol match {
      case Some("SASL_SSL") => for {
        sp <- config.securityProtocol
        sm <- config.saslMechanism
      } yield {
        settings
            .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sp)
            .withProperty(SaslConfigs.SASL_MECHANISM, sm)
            .withProperty(SaslConfigs.SASL_JAAS_CONFIG, config.saslJaasConfig)
      }
      case _ => for {
        ks <- config.keystore.location
        ts <- config.truststore.location
        kp <- config.keyPass
      } yield {
        settings
            .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
            .withProperty(BrokerSecurityConfigs.SSL_CLIENT_AUTH_CONFIG, "required")
            .withProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kp)
            .withProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ks)
            .withProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kp)
            .withProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ts)
            .withProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kp)
      }
    }

    s.getOrElse(settings)
  }
}
