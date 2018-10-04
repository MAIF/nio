package configuration

import play.api.{Configuration, Environment, Mode}

class Env(
    configuration: Configuration,
    val environment: Environment
) {

  val config = NioConfiguration(configuration)

  val env: String = environment.mode match {
    case Mode.Dev  => "dev"
    case Mode.Prod => "prod"
    case Mode.Test => "test"
  }

  def isDev = environment.mode == Mode.Dev
}
