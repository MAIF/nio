package configuration

import play.api.Configuration
import pureconfig.*
import pureconfig.generic.derivation.default.*
import pureconfig.generic.semiauto.*
import pureconfig.generic.ProductHint

import scala.concurrent.duration.FiniteDuration

object NioConfiguration {
  import pureconfig._
  import pureconfig.generic.derivation.default._

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): NioConfiguration = {
    given ConfigReader[NioConfiguration] = deriveReader
    given ConfigWriter[NioConfiguration] = deriveWriter
    ConfigSource.fromConfig(config.underlying).at("nio").loadOrThrow[NioConfiguration]
  }
}

case class NioConfiguration(websocketHost: String, filter: Otoroshi, kafka: KafkaConfig, nio: NioConfig) 

case class NioConfig(url: String, headerValueClientId: String, headerValueClientSecret: String)

case class OtoroshiFilterConfig(
    sharedKey: String,
    issuer: String,
    headerClaim: String,
    headerRequestId: String,
    headerGatewayState: String,
    headerGatewayStateResp: String,
    headerGatewayHeaderClientId: String,
    headerGatewayHeaderClientSecret: String
)

case class Otoroshi(otoroshi: OtoroshiFilterConfig)

case class KafkaConfig(
    servers: String,
    keyPass: Option[String],
    groupId: Option[String],
    keystore: Location,
    truststore: Location,
    topic: String,
    eventIdSeed: Long,
    eventsGroupIn: Int,
    eventsGroupDuration: FiniteDuration
)

case class Location(location: Option[String])
