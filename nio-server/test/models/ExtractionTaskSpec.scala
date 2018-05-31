package models

import org.scalatest.WordSpecLike
import org.scalatestplus.play.PlaySpec

class ExtractionTaskSpec extends PlaySpec with WordSpecLike {

  val orgKey = "zeorg"

  "FilesMetadata" should {
    "serialize/deserialize from XML" in {
      val input = FilesMetadata(Seq(FileMetadata("file1.json","json",150), FileMetadata("file1.json","json",250)))
      val xml = input.asXml
      val fromXml = FilesMetadata.fromXml(xml)
      fromXml.isRight mustBe true
      fromXml.right.get mustBe input
    }
  }

  "ExtractionTaskInput" should {
    "serialize/deserialize from XML" in {
      val input = AppIds(Seq("app1","app2"))
      val xml = input.asXml
      val fromXml = AppIds.fromXml(xml)
      fromXml.isRight mustBe true
      fromXml.right.get mustBe input
    }
  }

  "ExtractionTask" should {
    "serialize/deserialize from XML" in {
      val extractionTask = ExtractionTask.newFrom(orgKey, "user1", Set("app1","app2"))
      val xml = extractionTask.asXml
      (xml \ "status").head.text mustBe "Running"
    }

    "serialize/deserialize from JSON" in {
      val extractionTask = ExtractionTask.newFrom(orgKey, "user1", Set("app1","app2"))
      val xml = extractionTask.asJson
      (xml \ "status").as[String] mustBe "Running"
    }

    "correctly handle progress" in {
//      val extractionTask = ExtractionTask.newFrom(orgKey, "user1", Set("app1","app2"))
//
//      extractionTask.set

    }
  }
}
