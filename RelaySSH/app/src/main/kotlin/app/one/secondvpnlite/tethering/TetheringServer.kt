package app.one.secondvpnlite.tethering

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import app.one.secondvpnlite.MainActivity
import app.one.secondvpnlite.R
import app.one.secondvpnlite.logs.AppLogManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Enumeration

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 */
class TetheringServer : Runnable {
    private var mPort = 0
    private var thread: Thread? = null

    fun getHotspotIpAddress(useIPv4: Boolean): List<InetAddress> {
        val addressList = ArrayList<InetAddress>()
        var networkInterfaces: Enumeration<NetworkInterface>? = null
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces()
        } catch (error: SocketException) {
            error.printStackTrace()
        }

        while (networkInterfaces!!.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            try {
                if (networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (useIPv4) {
                            if (!address.toString().contains(":") &&
                                !address.toString().contains("127.0.0.1") &&
                                address.toString().contains("192.168")
                            ) {
                                addressList.add(address)
                            }
                        } else {
                            addressList.add(address)
                        }
                    }
                }
            } catch (error: SocketException) {
                error.printStackTrace()
            }
        }
        return addressList
    }

    override fun run() {
        mContext = MainActivity.sContext
        if (!isToStopTethering) {
            thread = Thread {
                try {
                    try {
                        val address = getHotspotIpAddress(true)
                            .toString()
                            .replace("[", "")
                            .replace("]", "")
                            .replace(",", "<br>")
                        val port = 44355
                        AppLogManager.addLog(
                            "<strong></font><font color=\"#49C53C\">" +
                                mContext!!.getString(R.string.tethering_info) + " " +
                                "</strong><br><br><strong>IPs:</strong> " +
                                "<strong></font><font color=red>" +
                                address.replace("/", "") +
                                "</strong><br><strong>" +
                                mContext!!.getString(R.string.tethering_port) +
                                "</strong> <strong></font><font color=font color=red>" +
                                port + "</strong> "
                        )

                        try {
                            mIsRunning = true
                            startServerSocket.start()
                        } catch (error: Exception) {
                            startServerSocket.run()
                            error.printStackTrace()
                        }
                    } catch (error: Exception) {
                        AppLogManager.addLog("Tethering start service error: $error")
                        error.printStackTrace()
                    }
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }
            thread!!.start()
        } else {
            mIsRunning = false
            try {
                startServerSocket.interrupt()
                AppLogManager.addLog(
                    "<strong></font><font color=red>" +
                        mContext!!.getString(R.string.tethering_stopped) + " " +
                        "</strong>"
                )
                try {
                    serverSocket!!.close()
                } catch (error: IOException) {
                    error.printStackTrace()
                }
                try {
                    if (thread != null) {
                        thread!!.stop()
                        thread!!.interrupt()
                    }
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }

    class Handler(private val connection: Socket) : Thread() {
        override fun run() {
            try {
                val requestLine = getLine(connection.getInputStream())
                val splitLine = requestLine.split(" ")
                if (splitLine.size < 3) {
                    connection.close()
                    return
                }

                val requestType = splitLine[0]
                val httpVersion = splitLine[2]
                var urlString = splitLine[1]
                var url: URI? = null
                val host: String
                val port: Int

                if (requestType == CONNECT) {
                    val hostPortSplit = urlString.split(":")
                    host = hostPortSplit[0]
                    port = if (hostPortSplit.size < 2) {
                        443
                    } else {
                        try {
                            hostPortSplit[1].toInt()
                        } catch (_: NumberFormatException) {
                            connection.close()
                            return
                        }
                    }
                    urlString = "https://$host:$port"
                } else {
                    try {
                        url = URI(urlString)
                        host = url.host
                        port = if (url.port < 0) 80 else url.port
                    } catch (_: URISyntaxException) {
                        connection.close()
                        return
                    }
                }

                var proxies: List<Proxy> = Lists.newArrayList()
                try {
                    proxies = ProxySelector.getDefault()!!.select(URI(urlString))
                } catch (error: URISyntaxException) {
                    error.printStackTrace()
                }

                var server: Socket? = null
                for (proxy in proxies) {
                    try {
                        if (proxy != Proxy.NO_PROXY) {
                            val address = proxy.address() as InetSocketAddress
                            server = Socket(address.hostName, address.port)
                            sendLine(server, requestLine)
                        } else {
                            server = Socket(host, port)
                            if (requestType == CONNECT) {
                                skipToRequestBody(connection)
                                sendLine(connection, HTTP_OK)
                            } else {
                                sendAugmentedRequestToHost(
                                    connection,
                                    server,
                                    requestType,
                                    url!!,
                                    httpVersion
                                )
                            }
                        }
                    } catch (error: IOException) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Unable to connect to proxy $proxy", error)
                        }
                    }
                    if (server != null) break
                }

                if (proxies.isEmpty()) {
                    server = Socket(host, port)
                    if (requestType == CONNECT) {
                        skipToRequestBody(connection)
                        sendLine(connection, HTTP_OK)
                    } else {
                        sendAugmentedRequestToHost(
                            connection,
                            server,
                            requestType,
                            url!!,
                            httpVersion
                        )
                    }
                }

                if (server != null) {
                    SocketConnect.connect(connection, server)
                }
            } catch (error: Exception) {
                Log.d(TAG, "Problem Proxying", error)
            }

            try {
                connection.close()
            } catch (_: IOException) {
            }
        }

        private fun sendRequestLineWithPath(
            server: Socket,
            requestType: String,
            absoluteUri: URI,
            httpVersion: String
        ) {
            val absolutePath = getAbsolutePathFromAbsoluteURI(absoluteUri)
            val outgoingRequestLine =
                String.format("%s %s %s", requestType, absolutePath, httpVersion)
            sendLine(server, outgoingRequestLine)
        }

        private fun getAbsolutePathFromAbsoluteURI(uri: URI): String {
            val rawPath = uri.rawPath
            val rawQuery = uri.rawQuery
            val rawFragment = uri.rawFragment
            val absolutePath = StringBuilder()
            if (rawPath != null) absolutePath.append(rawPath) else absolutePath.append("/")
            if (rawQuery != null) absolutePath.append("?").append(rawQuery)
            if (rawFragment != null) absolutePath.append("#").append(rawFragment)
            return absolutePath.toString()
        }

        private fun getLine(inputStream: InputStream): String {
            val buffer = StringBuilder()
            var byteBuffer = inputStream.read()
            if (byteBuffer < 0) return ""
            do {
                if (byteBuffer != '\r'.code) {
                    buffer.append(byteBuffer.toChar())
                }
                byteBuffer = inputStream.read()
            } while (byteBuffer != '\n'.code && byteBuffer >= 0)
            return buffer.toString()
        }

        private fun sendLine(socket: Socket, line: String) {
            val outputStream: OutputStream = socket.getOutputStream()
            outputStream.write(line.toByteArray())
            outputStream.write('\r'.code)
            outputStream.write('\n'.code)
            outputStream.flush()
        }

        private fun skipToRequestBody(socket: Socket) {
            while (getLine(socket.getInputStream()).isNotEmpty()) {
            }
        }

        private fun sendAugmentedRequestToHost(
            source: Socket,
            destination: Socket,
            httpMethod: String,
            uri: URI,
            httpVersion: String
        ) {
            sendRequestLineWithPath(destination, httpMethod, uri, httpVersion)
            filterAndForwardRequestHeaders(source, destination)
            sendLine(destination, "Connection: close")
            sendLine(destination, "")
        }

        private fun filterAndForwardRequestHeaders(source: Socket, destination: Socket) {
            var line: String
            do {
                line = getLine(source.getInputStream())
                if (line.isNotEmpty() && !shouldRemoveHeaderLine(line)) {
                    sendLine(destination, line)
                }
            } while (line.isNotEmpty())
        }

        private fun shouldRemoveHeaderLine(line: String): Boolean {
            val colonIndex = line.indexOf(":")
            if (colonIndex != -1) {
                val headerName = line.substring(0, colonIndex).trim()
                return headerName.startsWith(HEADER_CONNECTION, ignoreCase = true) ||
                    headerName.startsWith(HEADER_PROXY_CONNECTION, ignoreCase = true)
            }
            return false
        }
    }

    companion object {
        private const val CONNECT = "CONNECT"
        private const val HTTP_OK = "HTTP/1.1 200 OK\n"
        private const val TAG = "ProxyServer"
        private const val HEADER_CONNECTION = "connection"
        private const val HEADER_PROXY_CONNECTION = "proxy-connection"

        @JvmField
        var mIsRunning = false

        @JvmField
        var serverSocket: ServerSocket? = null

        @SuppressLint("StaticFieldLeak")
        @JvmField
        var mContext: Context? = null

        @JvmField
        var isToStopTethering = false

        @JvmField
        val startServerSocket: Thread = object : Thread() {
            override fun run() {
                try {
                    val port = 44355
                    serverSocket = ServerSocket(port)
                    serverSocket!!.setReuseAddress(true)
                    var socket: Socket?
                    try {
                        while (serverSocket!!.accept().also { socket = it } != null) {
                            val client = socket!!
                            if (!mIsRunning) break
                            client.keepAlive = true
                            client.tcpNoDelay = true
                            Handler(client).start()
                        }
                    } catch (error: Exception) {
                        error.printStackTrace()
                    }
                } catch (error: IOException) {
                    Log.e(TAG, "Failed to start proxy server", error)
                }
            }
        }
    }
}
