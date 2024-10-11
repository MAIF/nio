package configuration

import play.api.Configuration
import pureconfig.generic.ProductHint
import pureconfig._
import pureconfig.generic.derivation.default._


import scala.concurrent.duration.FiniteDuration


case class NioConfiguration(
    baseUrl: String,
    logoutUrl: String,
    downloadFileHost: String,
    mailSendingEnable: Boolean,
    filter: SecurityFilter,
    recordManagementEnabled: Boolean,
    s3ManagementEnabled: Boolean,
    kafka: KafkaConfig,
    s3Config: S3Config,
    mailGunConfig: MailGunConfig,
    db: Db
) derives ConfigReader

case class SecurityFilter(
    securityMode: String,
    otoroshi: OtoroshiFilterConfig,
    auth0: Auth0Config,
    default: DefaultFilterConfig
)

case class Auth0Config(
    allowedPaths: Seq[String],
    clientId: String,
    clientSecret: String,
    domain: String,
    callbackUrl: String,
    audience: String,
    apiKeys: ApiKeysConfig
)

case class ApiKeyHeaders(headerClientId: String, headerClientSecret: String)

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

case class DefaultFilterConfig(
    allowedPaths: Seq[String],
    sharedKey: String,
    cookieClaim: String,
    issuer: String,
    apiKeys: ApiKeysConfig,
    defaultUser: DefaultUserConfig
)

case class DefaultUserConfig(username: String, password: String)

case class ApiKeysConfig(headerClientId: String, headerClientSecret: String)

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
    eventsGroupDuration: FiniteDuration,
    catchUpEvents: CatchUpEventsConfig
)

case class Location(location: Option[String])


case class TenantConfiguration(admin: AdminConfig) derives ConfigReader


case class HealthCheckConfiguration(secret: String, header: String) derives ConfigReader

case class AdminConfig(secret: String, header: String)

case class S3Config(
    bucketName: String,
    uploadBucketName: String,
    endpoint: String,
    region: String,
    chunkSizeInMb: Int,
    accessKey: String,
    secretKey: String,
    expireAtInDay: Int
)

case class MailGunConfig(apiKey: String, endpoint: String, from: String)

case class CatchUpEventsConfig(strategy: String, delay: FiniteDuration, interval: FiniteDuration)

case class Db(batchSize: Int)

object NioConfiguration {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): NioConfiguration =
    ConfigSource.fromConfig(config.underlying).at("nio").loadOrThrow[NioConfiguration]
}


object TenantConfiguration {
  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): TenantConfiguration =
    ConfigSource.fromConfig(config.underlying).at("tenant").loadOrThrow[TenantConfiguration]
}


object HealthCheckConfiguration {
  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): HealthCheckConfiguration =
    ConfigSource.fromConfig(config.underlying).at("healthcheck").loadOrThrow[HealthCheckConfiguration]
}