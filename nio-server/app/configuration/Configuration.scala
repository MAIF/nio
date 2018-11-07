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

case class NioConfiguration(baseUrl: String,
                            logoutUrl: String,
                            downloadFileHost: String,
                            mailSendingEnable: Boolean,
                            filter: SecurityFilter,
                            recordManagementEnabled: Boolean,
                            s3ManagementEnabled: Boolean,
                            kafka: KafkaConfig,
                            s3Config: S3Config,
                            mailGunConfig: MailGunConfig)

case class SecurityFilter(securityMode: String,
                          otoroshi: OtoroshiFilterConfig,
                          auth0: Auth0Config,
                          default: DefaultFilterConfig)

case class Auth0Config(allowedPaths: Seq[String],
                       clientId: String,
                       clientSecret: String,
                       domain: String,
                       callbackUrl: String,
                       audience: String,
                       signInSecret: String)

case class ApiKeyHeaders(headerClientId: String, headerClientSecret: String)

case class OtoroshiFilterConfig(sharedKey: String,
                                issuer: String,
                                headerClaim: String,
                                headerRequestId: String,
                                headerGatewayState: String,
                                headerGatewayStateResp: String,
                                headerGatewayHeaderClientId: String,
                                headerGatewayHeaderClientSecret: String)

case class DefaultFilterConfig(allowedPaths: Seq[String],
                               sharedKey: String,
                               cookieClaim: String,
                               issuer: String,
                               apiKeys: ApiKeysConfig,
                               defaultUser: DefaultUserConfig)

case class DefaultUserConfig(username: String,
                             password: String,
                             clientId: String,
                             clientSecret: String)

case class ApiKeysConfig(headerClientId: String, headerClientSecret: String)

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

object HealthCheckConfiguration {
  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def apply(config: Configuration): HealthCheckConfiguration = {
    loadConfigOrThrow[HealthCheckConfiguration](config.underlying,
                                                "healthcheck")
  }
}

case class HealthCheckConfiguration(secret: String, header: String)

case class AdminConfig(secret: String, header: String)

case class S3Config(bucketName: String,
                    uploadBucketName: String,
                    endpoint: String,
                    region: String,
                    chunkSizeInMb: Int,
                    accessKey: String,
                    secretKey: String,
                    expireAtInDay: Int)

case class MailGunConfig(apiKey: String, endpoint: String, from: String)
