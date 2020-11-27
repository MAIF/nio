package models

import org.scalatest.WordSpecLike
import org.scalatestplus.play.PlaySpec

class TenantSpec extends PlaySpec with WordSpecLike {

  "Tenant" should {

    "serialize/deserialize from XML" in {
      val tenant = Tenant("testTenant", "test tenant")

      val xml = tenant.asXml()

      val fromXml = Tenant.fromXml(xml)

      fromXml.isRight mustBe true
      fromXml.map(_.key) mustBe Right(tenant.key)
    }

  }

}
