package models

import org.scalatest.WordSpecLike
import org.scalatestplus.play.PlaySpec
import utils.UploadTracker

class ExtractionTaskSpec extends PlaySpec with WordSpecLike {

  val orgKey = "zeorg"

  "FilesMetadata" should {
    "serialize/deserialize from XML" in {
      val input = FilesMetadata(
        Seq(FileMetadata("file1.json", "json", 150),
            FileMetadata("file1.json", "json", 250)))
      val xml = input.asXml
      val fromXml = FilesMetadata.fromXml(xml)
      fromXml.isRight mustBe true
      fromXml.right.get mustBe input
    }
  }

  "ExtractionTaskInput" should {
    "serialize/deserialize from XML" in {
      val input = AppIds(Seq("app1", "app2"))
      val xml = input.asXml
      val fromXml = AppIds.fromXml(xml)
      fromXml.isRight mustBe true
      fromXml.right.get mustBe input
    }
  }

  "ExtractionTask" should {
    "serialize/deserialize from XML" in {
      val extractionTask =
        ExtractionTask.newFrom(orgKey, "user1", Set("app1", "app2"))
      val xml = extractionTask.asXml
      (xml \ "status").head.text mustBe "Running"
    }

    "serialize/deserialize from JSON" in {
      val extractionTask =
        ExtractionTask.newFrom(orgKey, "user1", Set("app1", "app2"))
      val xml = extractionTask.asJson
      (xml \ "status").as[String] mustBe "Running"
    }

    "correctly handle progress" in {
      val app1Id = "app1"
      val app2Id = "app2"
      var task = ExtractionTask.newFrom(orgKey, "user1", Set(app1Id, app2Id))

      task = task.copyWithUpdatedAppState(
        app1Id,
        FilesMetadata(Seq(FileMetadata("file1.json", "json", 20))))
      task = task.copyWithUpdatedAppState(
        app2Id,
        FilesMetadata(Seq(FileMetadata("file2.json", "json", 20))))

      val maybeAppState = task.states.find(_.appId == app1Id)
      maybeAppState.isDefined mustBe true

      UploadTracker.incrementUploadedBytes(task._id, app1Id, 20)

      val appState = maybeAppState.get
      task = task.copyWithFileUploadHandled(app1Id, appState)

      task.done mustBe 1
      task.states.exists(appState =>
        appState.appId == app1Id && appState.status == ExtractionTaskStatus.Done) mustBe true
      task.progress mustBe 50

      UploadTracker.incrementUploadedBytes(task._id, app2Id, 20)

      task =
        task.copyWithFileUploadHandled(app2Id,
                                       task.states.find(_.appId == app2Id).get)
      task.done mustBe 2
      task.states.exists(appState =>
        appState.appId == app2Id && appState.status == ExtractionTaskStatus.Done) mustBe true
      task.progress mustBe 100

    }
  }
}
