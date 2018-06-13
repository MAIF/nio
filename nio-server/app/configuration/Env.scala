package configuration

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Environment, Mode}

@Singleton
class Env @Inject()(
    configuration: Configuration,
    val environment: Environment
) {

  val config = NioConfiguration(configuration)

  val tenantConfig = TenantConfiguration(configuration)

  val healthCheckConfig = HealthCheckConfiguration(configuration)

  val env: String = environment.mode match {
    case Mode.Dev  => "dev"
    case Mode.Prod => "prod"
    case Mode.Test => "test"
  }

  def isDev = environment.mode == Mode.Dev
}
