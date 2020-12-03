package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class TenantSpec extends AnyWordSpecLike with Matchers {

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
