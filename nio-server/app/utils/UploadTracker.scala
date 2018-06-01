package utils

import java.util.concurrent.atomic.LongAdder

import scala.collection.concurrent.TrieMap

object UploadTracker {

  val appsUploads = new TrieMap[String, LongAdder]()

  def addApp(taskId: String, appId: String) =
    appsUploads.put(s"$taskId$appId", new LongAdder())

  def incrementUploadedBytes(taskId: String,
                             appId: String,
                             receivedBytes: Long) =
    appsUploads
      .getOrElseUpdate(s"$taskId$appId", new LongAdder())
      .add(receivedBytes)

  def getUploadedBytes(taskId: String, appId: String) =
    appsUploads.getOrElse(s"$taskId$appId", new LongAdder).longValue()

  // Once all upload are finished remove the entry
  def removeApp(taskId: String, appId: String) =
    appsUploads.remove(s"$taskId$appId")

}
