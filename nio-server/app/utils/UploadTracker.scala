package utils

import java.util.concurrent.atomic.LongAdder

import scala.collection.concurrent.TrieMap

object UploadTracker {

  val appsUploads = new TrieMap[String, LongAdder]()

  def addApp(appId: String) = appsUploads.put(appId, new LongAdder())

  def incrementUploadedBytes(appId: String, receivedBytes: Long) =
    appsUploads.getOrElseUpdate(appId, new LongAdder()).add(receivedBytes)

  def getUploadedBytes(appId: String) = {
    println("------> " + appsUploads)
    appsUploads.getOrElse(appId, new LongAdder).longValue()
  }

  // Once all upload are finished remove the entry
  def removeApp(appId: String) = appsUploads.remove(appId)

}
