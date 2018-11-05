package filters

import auth.AuthInfo
import play.api.libs.typedmap.TypedKey

object FilterAttributes {
  val Email: TypedKey[String] = TypedKey("email")
  val AuthInfo: TypedKey[Option[AuthInfo]] = TypedKey("authInfo")
}

trait AuthInfoMock {

  def getAuthInfo: AuthInfo
}

class AuthInfoDev extends AuthInfoMock {
  override def getAuthInfo: AuthInfo =
    AuthInfo("test@test.com",
             isAdmin = true,
             Some(Seq(("foo", "bar"), ("foo2", "bar2"))),
             Some(Seq("offer1", "offer2")))
}
