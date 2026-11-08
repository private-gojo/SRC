package app.one.secondvpnlite.injector

import android.content.Context
import com.trilead.ssh2.ProxyData
import org.conscrypt.Conscrypt
import app.one.secondvpnlite.logs.AppLogManager
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.SocketChannel
import java.security.Security
import java.util.Arrays
import javax.net.ssl.HandshakeCompletedEvent
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SSLTunnelProxy(
    private val stunnelServer: String,
    private val stunnelPort: Int,
    private val stunnelHostSNI: String,
    private val TLSVersion: String,
    private val context: Context
) : ProxyData {

    private var input: Socket? = null
    private var mSocket: Socket? = null

    inner class HandshakeTunnelCompletedListener(
        private val host: String,
        private val port: Int,
        private val sslSocket: SSLSocket
    ) : HandshakeCompletedListener {
        override fun handshakeCompleted(event: HandshakeCompletedEvent) {
            AppLogManager.addLog(
                ("SSL: Supported protocols: <br>" +
                    Arrays.toString(sslSocket.supportedProtocols))
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "<br>")
            )
            AppLogManager.addLog(
                ("SSL: Enabled protocols: <br>" +
                    Arrays.toString(sslSocket.enabledProtocols))
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "<br>")
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
    private fun sendForwardSuccess(socket: Socket) {
        val response = "HTTP/1.1 200 OK\r\n\r\n"
        socket.getOutputStream().write(response.toByteArray())
        socket.getOutputStream().flush()
    }

    @Throws(IOException::class)
    override fun openConnection(
        hostname: String,
        port: Int,
        connectTimeout: Int,
        readTimeout: Int
    ): Socket {
        mSocket = SocketChannel.open().socket()
        mSocket!!.connect(InetSocketAddress(stunnelServer, stunnelPort), 15000)
        if (mSocket!!.isConnected) {
            mSocket = doSSLHandshake(hostname, stunnelHostSNI, port)
        }
        return mSocket!!
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
                // Hostname override is optional on unsupported socket implementations.
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
            throw IOException("SSL handshake error: $error")
        }
    }

    override fun close() {
        try {
            mSocket?.close()
        } catch (_: IOException) {
        }
    }

    private fun setSNIHost(
        factory: SSLSocketFactory,
        socket: SSLSocket,
        hostname: String
    ) {
        if (factory is android.net.SSLCertificateSocketFactory &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
        ) {
            factory.setHostname(socket, hostname)
        } else {
            try {
                socket.javaClass.getMethod("setHostname", String::class.java)
                    .invoke(socket, hostname)
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val delay = 1000

        init {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
            } catch (error: NoClassDefFoundError) {
                error.printStackTrace()
            }
        }
    }
}
