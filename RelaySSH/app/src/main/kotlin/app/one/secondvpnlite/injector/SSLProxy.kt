package app.one.secondvpnlite.injector

import org.conscrypt.Conscrypt
import app.one.secondvpnlite.SecondVPN
import app.one.secondvpnlite.logs.AppLogManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.Security
import java.util.Arrays
import java.util.Random
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.net.ssl.HandshakeCompletedEvent
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SSLProxy(private val incoming: Socket) {
    private var h = 0

    fun inject(): Socket? {
        try {
            val reader = BufferedReader(InputStreamReader(incoming.getInputStream()))
            val requestBuilder = StringBuilder()
            while (true) {
                val readLine = reader.readLine()
                if (readLine != null && readLine.isNotEmpty()) {
                    requestBuilder.append(readLine)
                    requestBuilder.append("\r\n")
                }

                if (requestBuilder.toString().isEmpty()) {
                    return null
                }

                val payload = RetornaPayload(requestBuilder.toString())
                if (payload != null) {
                    val ipSplit = SecondVPN.getServidorSSHDomain().split(":")
                    val server = ipSplit[0]
                    val port = ipSplit[1].toInt()
                    val sni = SecondVPN.getSNI()
                    var socket = Socket()
                    socket.connect(InetSocketAddress(server, port), 15000)
                    if (socket.isConnected) {
                        socket = doSSLHandshake(server, sni, port)
                    }
                    InjetaPayload(payload, socket)
                    return socket
                }
                return null
            }
        } catch (_: Exception) {
            return null
        }
    }

    @Throws(IOException::class)
    private fun doSSLHandshake(host: String, sni: String, port: Int): SSLSocket {
        val trustAllCertificates = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> =
                    emptyArray()

                override fun checkClientTrusted(
                    certificates: Array<out java.security.cert.X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    certificates: Array<out java.security.cert.X509Certificate>?,
                    authType: String?
                ) {
                }
            }
        )

        try {
            val trustManager = Conscrypt.getDefaultX509TrustManager()
            val sslContext = SSLContext.getInstance("TLS", "Conscrypt")
            sslContext.init(null, arrayOf(trustManager), null)

            val socket = TLSSocketFactory().createSocket(host, port) as SSLSocket
            try {
                socket.javaClass.getMethod("setHostname", String::class.java)
                    .invoke(socket, sni)
            } catch (_: Throwable) {
            }

            var currentTlsVersion = arrayOf("")
            try {
                currentTlsVersion = if (SecondVPN.getTLSVersion() == "auto") {
                    socket.supportedProtocols
                } else {
                    arrayOf(SecondVPN.getTLSVersion())
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }
            socket.enabledProtocols = currentTlsVersion
            socket.enabledCipherSuites = socket.enabledCipherSuites
            socket.addHandshakeCompletedListener(
                mHandshakeCompletedListener(host, port, socket)
            )
            AppLogManager.addLog("Starting SSL Handshake...")
            socket.startHandshake()
            return socket
        } catch (error: Exception) {
            AppLogManager.addLog("SSL Error: $error")
            throw IOException("Could not complete SSL handshake: $error")
        }
    }

    inner class mHandshakeCompletedListener(
        private val host: String,
        private val port: Int,
        private val sslSocket: SSLSocket
    ) : HandshakeCompletedListener {
        override fun handshakeCompleted(event: HandshakeCompletedEvent) {
            AppLogManager.addLog(
                ("SSL: Enabled protocols: <br>" +
                    Arrays.toString(sslSocket.enabledProtocols))
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "<br>")
            )
            AppLogManager.addLog(
                "<b><font color=#49C53C>SSL: Using cipher " +
                    event.session.cipherSuite + "</font></b>"
            )
            AppLogManager.addLog("SSL: Using protocol " + event.session.protocol)
            AppLogManager.addLog("SSL: Handshake finished")
        }
    }

    private fun d(value: String): String {
        var result = value
        if (result.contains("[cr*")) result = a(result, "[cr*", "\r")
        if (result.contains("[lf*")) result = a(result, "[lf*", "\n")
        if (result.contains("[crlf*")) result = a(result, "[crlf*", "\r\n")
        if (result.contains("[lfcr*")) result = a(result, "[lfcr*", "\n\r")
        return result
    }

    private fun a(value: String, marker: String, replacement: String): String {
        var result = value
        while (result.contains(marker)) {
            val matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(result)
            if (matcher.find()) {
                val count = matcher.group(1)!!.toInt()
                var repeated = ""
                for (index in 0 until count) {
                    repeated = repeated + replacement
                }
                result = result.replace(marker + count + "]", repeated)
            } else {
                break
            }
        }
        return result
    }

    fun ua(): String =
        System.getProperty("http.agent")
            ?: "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36"

    private fun RetornaPayload(value: String?): String? {
        var result: String? = null
        if (value != null) {
            try {
                if (value.isNotEmpty()) {
                    val firstLine = value.split("\r\n".toRegex())[0]
                    val split = firstLine.split(" ")
                    val hostPort = split[1].split(":")
                    val host = hostPort[0]
                    val port = hostPort[1]
                    result = d(
                        SecondVPN.getPayloadKey()
                            .replace("[real_raw]", value)
                            .replace("[raw]", firstLine)
                            .replace("[method]", split[0])
                            .replace("[host_port]", split[1])
                            .replace("[host]", host)
                            .replace("[port]", port)
                            .replace("[protocol]", split[2])
                            .replace("[ua]", ua())
                            .replace("[cr]", "\r")
                            .replace("[lf]", "\n")
                            .replace("[crlf]", "\r\n")
                            .replace("[lfcr]", "\n\r")
                            .replace("\\r", "\r")
                            .replace("\\n", "\n")
                    )
                    return result
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    @Throws(Exception::class)
    private fun InjetaPayload(value: String, socket: Socket) {
        AppLogManager.addLog("<b><font color=#49C53C>Sending payload...</font></b>")
        var index = 0
        var payload = value
        val outputStream: OutputStream = socket.getOutputStream()
        var split: Array<String>

        if (payload.contains("[random]")) {
            split = Pattern.compile(Pattern.quote("[random]")).split(payload)
            payload = split[Random().nextInt(split.size)]
        }

        if (payload.contains("[repeat]")) {
            split = Pattern.compile(Pattern.quote("[repeat]")).split(payload)
            payload = split[h]
            h++
            if (h > split.size - 1) h = 0
        }

        if (payload.contains("[split_delay]")) {
            split = Pattern.compile(Pattern.quote("[split_delay]")).split(payload)
            while (index < split.size) {
                val part = split[index]
                if (a(part, socket, outputStream)) {
                    outputStream.write(part.toByteArray())
                    outputStream.flush()
                }
                index++
            }
        } else if (payload.contains("[splitNoDelay]")) {
            split = Pattern.compile("\\[splitNoDelay\\]").split(payload)
            for (part in split) {
                outputStream.write(part.toByteArray())
                outputStream.flush()
            }
        } else if (payload.contains("[instant_split]")) {
            split = Pattern.compile("\\[instant_split\\]").split(payload)
            for (part in split) {
                outputStream.write(part.toByteArray())
                outputStream.flush()
            }
        } else if (payload.contains("[delay]")) {
            split = Pattern.compile("\\[delay\\]").split(payload)
            for (position in split.indices) {
                outputStream.write(split[position].toByteArray())
                outputStream.flush()
                if (position != split.size - 1) Thread.sleep(1000)
            }
        } else if (payload.contains("[delay_split]")) {
            split = Pattern.compile("\\[delay_split\\]").split(payload)
            for (position in split.indices) {
                outputStream.write(split[position].toByteArray())
                outputStream.flush()
                if (position != split.size - 1) Thread.sleep(1000)
            }
        } else if (a(payload, socket, outputStream)) {
            outputStream.write(payload.toByteArray())
            outputStream.flush()
        }
    }

    @Throws(Exception::class)
    private fun a(value: String, socket: Socket, outputStream: OutputStream): Boolean {
        if (!value.contains("[split]")) return true
        val parts = Pattern.compile(Pattern.quote("[split]")).split(value)
        for (part in parts) {
            outputStream.write(part.toByteArray())
            outputStream.flush()
        }
        return false
    }

    companion object {
        init {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
            } catch (error: NoClassDefFoundError) {
                error.printStackTrace()
            }
        }
    }
}
