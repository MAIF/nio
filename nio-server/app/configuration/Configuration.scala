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

case class NioConfiguration(logoutUrl: String,
                            filter: Otoroshi,
                            recordManagementEnabled: Boolean,
                            s3ManagementEnabled: Boolean,
                            kafka: KafkaConfig,
                            s3Config: S3Config)

case class ApiKeyHeaders(headerClientId: String, headerClientSecret: String)

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

object TenantConfiguration {
  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): TenantConfiguration = {
    loadConfigOrThrow[TenantConfiguration](config.underlying, "tenant")
  }
}

case class TenantConfiguration(admin: AdminConfig)

case class AdminConfig(secret: String, header: String)

case class S3Config(bucketName: String,
                    endpoint: String,
                    accessKey: String,
                    secretKey: String)
