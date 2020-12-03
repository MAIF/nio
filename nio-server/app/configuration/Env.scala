package configuration

import play.api.{Configuration, Environment, Mode}
import utils.NioLogger

class Env(
    configuration: Configuration,
    val environment: Environment
) {

  val config = NioConfiguration(configuration)
  NioLogger.info(s"Nio Configuration $config")

  val tenantConfig = TenantConfiguration(configuration)
  NioLogger.info(s"Tenant Configuration $tenantConfig")

  val healthCheckConfig = HealthCheckConfiguration(configuration)
  NioLogger.info(s"Healt hcheck Configuration $healthCheckConfig")

  val env: String = environment.mode match {
    case Mode.Dev  => "dev"
    case Mode.Prod => "prod"
    case Mode.Test => "test"
  }

  def isDev = environment.mode == Mode.Dev
}
