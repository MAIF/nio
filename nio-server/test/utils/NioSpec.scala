package utils

import auth.AuthInfo
import com.softwaremill.macwire.wire
import filters.{AuthInfoMock, OtoroshiFilter}
import loader.{NioComponents, NioComponentsMongo, NioComponentsPostgres}
import play.api.ApplicationLoader._
import play.api.{Application, ApplicationLoader, Logger, LoggerConfigurator}
import play.api.mvc.{EssentialFilter, Filter}

class AuthInfoTest extends AuthInfoMock {
  override def getAuthInfo: AuthInfo =
    AuthInfo("nio-test@test.com",
             isAdmin = true,
             Some(Seq(("foo", "bar"), ("foo2", "bar2"))),
             Some(Seq("offer1", "offer2")))
}

class NioTestLoaderMongo(maybeAuthInfo: Option[AuthInfoMock] = None)
    extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new NioSpecMongo(context, maybeAuthInfo).application
  }
}

class NioTestLoaderPostgres(maybeAuthInfo: Option[AuthInfoMock] = None)
    extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new NioSpecPostgres(context, maybeAuthInfo).application
  }
}

class NioSpecMongo(context: Context, maybeAuthInfo: Option[AuthInfoMock])
    extends NioComponentsMongo(context) {
  override implicit lazy val authInfo: AuthInfoMock = maybeAuthInfo match {
    case Some(value) => value
    case None        => new AuthInfoTest
  }
  override implicit lazy val securityFilter: Filter = wire[OtoroshiFilter]
}

class NioSpecPostgres(context: Context, maybeAuthInfo: Option[AuthInfoMock])
    extends NioComponentsPostgres(context) {
  override implicit lazy val authInfo: AuthInfoMock = maybeAuthInfo match {
    case Some(value) => value
    case None        => new AuthInfoTest
  }
  override implicit lazy val securityFilter: Filter = wire[OtoroshiFilter]
}
