package configuration

import play.api.Configuration
import pureconfig._

import scala.concurrent.duration.FiniteDuration

object NioConfiguration {

  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): NioConfiguration = {
    loadConfigOrThrow[NioConfiguration](config.underlying, "nio")
  }
}

case class NioConfiguration(websocketHost: String,
                            filter: Otoroshi,
                            kafka: KafkaConfig)

case class OtoroshiFilterConfig(sharedKey: String,
                                issuer: String,
                                headerClaim: String,
                                headerRequestId: String,
                                headerGatewayState: String,
                                headerGatewayStateResp: String,
                                headerGatewayHeaderClientId: String,
                                headerGatewayHeaderClientSecret: String)

case class Otoroshi(otoroshi: OtoroshiFilterConfig)

case class KafkaConfig(servers: String,
                       keyPass: Option[String],
                       groupId: Option[String],
                       keystore: Location,
                       truststore: Location,
                       topic: String,
                       eventIdSeed: Long,
                       eventsGroupIn: Int,
                       eventsGroupDuration: FiniteDuration)

case class Location(location: Option[String])
