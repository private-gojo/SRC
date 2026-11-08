package app.one.secondvpnlite.injector

import app.one.secondvpnlite.logs.AppLogManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.Locale

class HTTPThread(
    private val incoming: Socket,
    private val outgoing: Socket,
    private val clientToServer: Boolean
) : Thread() {

    private val TAG = "ProxyThread"

    init {
        isDaemon = true
    }

    override fun run() {
        try {
            val buffer = if (clientToServer) ByteArray(16384) else ByteArray(32768)
            val fromClient: InputStream = incoming.getInputStream()
            val toClient: OutputStream = outgoing.getOutputStream()

            while (true) {
                val numberRead = fromClient.read(buffer)
                if (numberRead == -1) break

                var result = String(buffer, 0, numberRead)
                if (clientToServer) {
                    toClient.write(buffer, 0, numberRead)
                    toClient.flush()
                } else {
                    val split = result.split("\r\n".toRegex()).toTypedArray()
                    val fullResult = result

                    if (split[0].lowercase(Locale.getDefault()).startsWith("http")) {
                        result = split[0].substring(9, 12)
                        AppLogManager.addLog("<b>" + split[0] + "</b>")
                        if (result.indexOf("200") >= 0) {
                            if (fullResult.contains("html")) {
                                toClient.write(
                                    (split[0].split(" ")[0] + " 200 OK\r\n\r\n").toByteArray()
                                )
                            } else {
                                toClient.write(buffer, 0, numberRead)
                            }
                            toClient.flush()
                        } else if (true) {
                            if (split[0].split(" ")[0] == "HTTP/1.1") {
                                toClient.write(
                                    (split[0].split(" ")[0] + " 200 OK\r\n\r\n").toByteArray()
                                )
                            } else {
                                try {
                                    toClient.write(
                                        (split[0].split(" ")[0] +
                                            " 200 Connection established\r\n\r\n").toByteArray()
                                    )
                                } catch (_: Exception) {
                                    try {
                                        incoming.close()
                                        outgoing.close()
                                        return
                                    } catch (_: IOException) {
                                        return
                                    }
                                } catch (_: Throwable) {
                                    try {
                                        incoming.close()
                                        outgoing.close()
                                    } catch (_: IOException) {
                                    }
                                }
                            }
                            toClient.flush()
                        } else {
                            if (fullResult.contains("html")) {
                                toClient.write(
                                    (split[0].split(" ")[0] + " 200 OK\r\n\r\n").toByteArray()
                                )
                            } else {
                                toClient.write(buffer, 0, numberRead)
                            }
                            toClient.flush()
                        }
                    } else {
                        if (fullResult.contains("html")) {
                            toClient.write(
                                (split[0].split(" ")[0] + " 200 OK\r\n\r\n").toByteArray()
                            )
                        } else {
                            toClient.write(buffer, 0, numberRead)
                        }
                        toClient.flush()
                    }
                }
            }

            fromClient.close()
            toClient.close()
        } catch (_: Exception) {
            try {
                incoming.close()
                outgoing.close()
            } catch (_: IOException) {
            }
        }
    }
}
