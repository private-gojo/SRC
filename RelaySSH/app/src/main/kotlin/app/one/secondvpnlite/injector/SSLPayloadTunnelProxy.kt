package app.one.secondvpnlite.injector

import android.content.Context
import android.util.Log
import com.trilead.ssh2.Connection
import com.trilead.ssh2.HTTPProxyException
import com.trilead.ssh2.ProxyData
import com.trilead.ssh2.crypto.Base64
import com.trilead.ssh2.sftp.Packet
import com.trilead.ssh2.transport.ClientServerHello
import org.conscrypt.Conscrypt
import app.one.secondvpnlite.R
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.tunnel.TunnelUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.security.Security
import java.util.Arrays
import javax.net.ssl.HandshakeCompletedEvent
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SSLPayloadTunnelProxy(
    private val stunnelServer: String,
    private val stunnelPort: Int,
    private val stunnelHostSNI: String,
    private val requestPayload: String?,
    private val TLSVersion: String,
    private val context: Context
) : ProxyData {

    private val proxyUser: String? = null
    private val proxyPass: String? = null
    private var input: Socket? = null

    inner class HandshakeTunnelCompletedListener(
        private val host: String,
        private val port: Int,
        private val sslSocket: SSLSocket
    ) : HandshakeCompletedListener {
        private var mConnection: Connection? = null

        override fun handshakeCompleted(event: HandshakeCompletedEvent) {
            AppLogManager.addLog(
                ("SSL: Supported protocols: <br>" +
                    Arrays.toString(sslSocket.supportedProtocols))
                    .replace("[", "").replace("]", "").replace(",", "<br>")
            )
            AppLogManager.addLog(
                ("SSL: Enabled protocols: <br>" +
                    Arrays.toString(sslSocket.enabledProtocols))
                    .replace("[", "").replace("]", "").replace(",", "<br>")
            )
            AppLogManager.addLog("SSL: Using cipher " + event.session.cipherSuite)
            AppLogManager.addLog("SSL: Using protocol " + event.session.protocol)
            AppLogManager.addLog("SSL: Handshake finished")
        }
    }

    fun SSLSupport(socket: Socket) {
        input = socket
    }

    @Throws(IOException::class)
    override fun openConnection(
        hostname: String,
        port: Int,
        connectTimeout: Int,
        readTimeout: Int
    ): Socket {
        Log.d(
            "SSL Payload",
            "Start SSL on $stunnelServer:$stunnelPort with SNI $stunnelHostSNI and $requestPayload"
        )

        var socket = SocketChannel.open().socket()
        socket.connect(InetSocketAddress(stunnelServer, stunnelPort), 15000)

        if (socket.isConnected) {
            Log.d("SSL Payload", "Socket is connected")
            socket = doSSLHandshake(hostname, stunnelHostSNI, port)
            val payload = getRequestPayload(hostname, port)
            Log.d("SSL Payload", "Inject payload")
            val output: OutputStream = socket.getOutputStream()

            AppLogManager.addLog(context.getString(R.string.injeting))
            if (!TunnelUtils.injectSplitPayload(payload, output)) {
                try {
                    output.write(payload.toByteArray(Charset.forName("ISO-8859-1")))
                } catch (_: UnsupportedEncodingException) {
                    output.write(payload.toByteArray())
                }
                output.flush()
            }

            val buffer = ByteArray(1024)
            val input: InputStream = socket.getInputStream()
            var length = ClientServerHello.readLineRN(input, buffer)

            val responseFirstLine = try {
                String(buffer, 0, length, Charset.forName("ISO-8859-1"))
            } catch (_: UnsupportedEncodingException) {
                String(buffer, 0, length)
            }

            AppLogManager.addLog("<strong>$responseFirstLine</strong>")
            val responseCode = responseFirstLine.substring(9, 12).toInt()
            if (responseCode == 200) return socket
            if (responseCode != 200) {
                Log.d("SSL Payload", "Replace with 200 OK")
                AppLogManager.addLog("Proxy: Auto Replace Header")
                AppLogManager.addLog("<b>HTTP/1.1 200 Connection established</b>")
                return socket
            }

            var responseAll = responseFirstLine
            while (ClientServerHello.readLineRN(input, buffer).also { length = it } != 0) {
                responseAll += "\n"
                responseAll += try {
                    String(buffer, 0, length, Charset.forName("ISO-8859-1"))
                } catch (_: UnsupportedEncodingException) {
                    String(buffer, 0, length)
                }
            }

            if (responseAll.isNotEmpty()) {
                // Original response-body debug logging remains disabled.
            }

            if (!responseFirstLine.startsWith("HTTP/")) {
                throw IOException("The proxy did not send back a valid HTTP response.")
            } else if (
                responseFirstLine.length >= 14 &&
                responseFirstLine[8] == ' ' &&
                responseFirstLine[12] == ' '
            ) {
                try {
                    val errorCode = responseFirstLine.substring(9, 12).toInt()
                    if (errorCode < 0 || errorCode > 999) {
                        throw IOException("The proxy did not send back a valid HTTP response.")
                    } else if (errorCode != Packet.SSH_FXP_EXTENDED) {
                        throw HTTPProxyException(responseFirstLine.substring(13), errorCode)
                    } else {
                        return socket
                    }
                } catch (_: NumberFormatException) {
                    throw IOException("The proxy did not send back a valid HTTP response.")
                }
            } else if (responseCode < 0 || length > 999) {
                throw IOException("The proxy did not send back a valid HTTP response.")
            } else {
                return socket
            }
        }
        return socket
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
                currentTlsVersion = if (TLSVersion == "auto") {
                    socket.supportedProtocols
                } else {
                    arrayOf(TLSVersion)
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }
            socket.enabledProtocols = currentTlsVersion
            socket.enabledCipherSuites = socket.enabledCipherSuites
            socket.addHandshakeCompletedListener(
                HandshakeTunnelCompletedListener(host, port, socket)
            )
            AppLogManager.addLog("Starting SSL Handshake...")
            socket.startHandshake()
            return socket
        } catch (error: Exception) {
            throw IOException("Could not complete SSL handshake: $error")
        }
    }

    private fun getRequestPayload(hostname: String, port: Int): String {
        var payload = requestPayload
        if (payload != null) {
            payload = TunnelUtils.formatCustomPayload(hostname, port, payload)
        } else {
            val builder = StringBuilder()
            builder.append("CONNECT ")
            builder.append(hostname)
            builder.append(':')
            builder.append(port)
            builder.append(" HTTP/1.0\r\n")
            if (proxyUser != null && proxyPass != null) {
                val credentials = "$proxyUser:$proxyPass"
                val encoded = try {
                    Base64.encode(credentials.toByteArray(Charset.forName("ISO-8859-1")))
                } catch (_: UnsupportedEncodingException) {
                    Base64.encode(credentials.toByteArray())
                }
                builder.append("Proxy-Authorization: Basic ")
                builder.append(encoded)
                builder.append("\r\n")
            }
            builder.append("\r\n")
            payload = builder.toString()
        }
        return payload
    }

    override fun close() {
        try {
            mSocket?.close()
        } catch (_: IOException) {
        }
    }

    companion object {
        @JvmField
        var mSocket: Socket? = null

        init {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
            } catch (error: NoClassDefFoundError) {
                error.printStackTrace()
            }
        }
    }
}
