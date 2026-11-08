package app.one.secondvpnlite.injector

import android.annotation.SuppressLint
import org.conscrypt.Conscrypt
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TLSSocketFactory @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
constructor() : SSLSocketFactory() {

    private val internalSSLSocketFactory: SSLSocketFactory
    @JvmField
    val sslctx: SSLContext

    init {
        val trustAllCertificates = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(
                    certificates: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(
                    certificates: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }
            }
        )
        sslctx = SSLContext.getInstance("TLS")
        sslctx.init(null, trustAllCertificates, SecureRandom())
        internalSSLSocketFactory = sslctx.socketFactory
    }

    override fun getDefaultCipherSuites(): Array<String> =
        internalSSLSocketFactory.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> =
        internalSSLSocketFactory.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(): Socket =
        enableTLSOnSocket(internalSSLSocketFactory.createSocket())

    @Throws(IOException::class)
    override fun createSocket(
        socket: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket = enableTLSOnSocket(
        internalSSLSocketFactory.createSocket(socket, host, port, autoClose)
    )

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket =
        enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))

    @Throws(IOException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket = enableTLSOnSocket(
        internalSSLSocketFactory.createSocket(host, port, localHost, localPort)
    )

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket =
        enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket = enableTLSOnSocket(
        internalSSLSocketFactory.createSocket(address, port, localAddress, localPort)
    )

    private fun enableTLSOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = socket.supportedProtocols
        }
        return socket
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
