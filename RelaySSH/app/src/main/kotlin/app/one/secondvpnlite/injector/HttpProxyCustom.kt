package app.one.secondvpnlite.injector

import android.content.Context
import com.trilead.ssh2.HTTPProxyException
import com.trilead.ssh2.ProxyData
import com.trilead.ssh2.crypto.Base64
import com.trilead.ssh2.sftp.Packet
import com.trilead.ssh2.transport.ClientServerHello
import com.trilead.ssh2.transport.TransportManager
import app.one.secondvpnlite.R
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.tunnel.TunnelUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset

/**
 * By Skank3r
 */
class HttpProxyCustom private constructor(
    private val proxyHost: String,
    private val proxyPort: Int,
    private val proxyUser: String?,
    private val proxyPass: String?,
    private val requestPayload: String?,
    private val modoDropbear: Boolean,
    private val mContext: Context
) : ProxyData {

    private var sock: Socket? = null

    constructor(
        proxyHost: String?,
        proxyPort: Int,
        proxyUser: String?,
        proxyPass: String?,
        context: Context
    ) : this(
        proxyHost ?: throw IllegalArgumentException("proxyHost must be non-null"),
        proxyPort.also {
            if (it < 0) throw IllegalArgumentException("proxyPort must be non-negative")
        },
        proxyUser,
        proxyPass,
        null,
        false,
        context
    )

    constructor(
        proxyHost: String?,
        proxyPort: Int,
        proxyUser: String?,
        proxyPass: String?,
        requestPayload: String?,
        modoDropbear: Boolean,
        context: Context
    ) : this(
        proxyHost ?: throw IllegalArgumentException("proxyHost must be non-null"),
        proxyPort.also {
            if (it < 0) throw IllegalArgumentException("proxyPort must be non-negative")
        },
        proxyUser,
        proxyPass,
        requestPayload,
        modoDropbear,
        context
    )

    @Throws(IOException::class)
    override fun openConnection(
        hostname: String,
        port: Int,
        connectTimeout: Int,
        readTimeout: Int
    ): Socket {
        val socket = Socket()
        sock = socket

        val address = TransportManager.createInetAddress(proxyHost)
        socket.connect(InetSocketAddress(address, proxyPort), connectTimeout)
        socket.soTimeout = readTimeout

        val payload = getRequestPayload(hostname, port)
        AppLogManager.addLog(R.string.state_proxy_inject)

        val output: OutputStream = socket.getOutputStream()
        if (!TunnelUtils.injectSplitPayload(payload, output)) {
            try {
                output.write(payload.toByteArray(Charset.forName("ISO-8859-1")))
            } catch (_: UnsupportedEncodingException) {
                output.write(payload.toByteArray())
            }
            output.flush()
        }

        if (modoDropbear) return socket

        val buffer = ByteArray(1024)
        val input: InputStream = socket.getInputStream()
        var length = ClientServerHello.readLineRN(input, buffer)

        val responseFirstLine = try {
            String(buffer, 0, length, Charset.forName("ISO-8859-1"))
        } catch (_: UnsupportedEncodingException) {
            String(buffer, 0, length)
        }

        AppLogManager.addLog("<strong>$responseFirstLine</strong>")
        val responseAll = responseFirstLine
        val responseSplit = responseAll.split("\r\n".toRegex()).toTypedArray()
        val responseCode = responseFirstLine.substring(9, 12).toInt()

        if (responseCode == 200) {
            return socket
        } else if (responseCode != 200) {
            return socket
        }

        var completeResponse = responseFirstLine
        while (ClientServerHello.readLineRN(input, buffer).also { length = it } != 0) {
            completeResponse += "\n"
            completeResponse += try {
                String(buffer, 0, length, "ISO-8859-1")
            } catch (_: UnsupportedEncodingException) {
                String(buffer, 0, length)
            }
        }

        if (completeResponse.isNotEmpty()) {
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

        return socket
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
        val socket = sock ?: return
        try {
            socket.close()
        } catch (_: IOException) {
            // Original close failure was ignored.
        }
    }
}
