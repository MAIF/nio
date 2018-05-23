package utils

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}

trait WithKafka {

  val kafkaPort = Tools.nextFreePort
  implicit val conf = EmbeddedKafkaConfig(kafkaPort = kafkaPort)

  val kafkaTopic: String = "nio-consent-events"

  def startKafka() {
    EmbeddedKafka.start()
    EmbeddedKafka.createCustomTopic(kafkaTopic, Map(), 1, 1)
  }

  def stopKafka() {
    EmbeddedKafka.stop()
  }

  def getKafkaPort(): String = {
    String.valueOf(kafkaPort)
  }

  def getKafkaTopic(): String = {
    kafkaTopic
  }
}
