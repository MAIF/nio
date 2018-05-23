package utils

import java.net.ServerSocket

object Tools {

  def nextFreePort = {
    val sock = new ServerSocket(0)
    val port = sock.getLocalPort
    sock.close()
    port
  }

}
