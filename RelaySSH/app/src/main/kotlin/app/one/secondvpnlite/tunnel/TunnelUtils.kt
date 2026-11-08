package app.one.secondvpnlite.tunnel

import android.util.ArrayMap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.VpnService
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Patterns
import app.one.secondvpnlite.logs.AppLogManager
import java.io.IOException
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections
import java.util.Enumeration
import java.util.HashMap
import java.util.Random
import java.util.regex.Matcher
import java.util.regex.Pattern

object TunnelUtils {
    @JvmField
    var BBCODES_LIST: MutableMap<String, CharSequence>? = null

    private val lastRotateList = ArrayMap<Int, Int>()
    private var lastPayload = ""
    private var output: Socket? = null

    @JvmStatic
    fun formatCustomPayload(hostname: String, port: Int, payloadInput: String): String {
        val bbCodes = ArrayMap<String, CharSequence>()
        BBCODES_LIST = bbCodes

        bbCodes["[method]"] = "CONNECT"
        bbCodes["[host]"] = hostname
        bbCodes["[ip]"] = hostname
        bbCodes["[protocol]"] = "HTTP/1.0"
        bbCodes["[port]"] = port.toString()
        bbCodes["[host_port]"] = String.format("%s:%d", hostname, port)
        bbCodes["[ssh]"] = String.format("%s:%d", hostname, port)
        bbCodes["[vpn]"] = String.format("%s:%d", hostname, port)
        bbCodes["[crlf]"] = "\r\n"
        bbCodes["[cr]"] = "\r"
        bbCodes["[lf]"] = "\n"
        bbCodes["[lfcr]"] = "\n\r"
        bbCodes["\\n"] = "\n"
        bbCodes["\\r"] = "\r"

        val userAgent = System.getProperty("http.agent")
        bbCodes["[ua]"] = userAgent
            ?: "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36"

        var payload = payloadInput
        if (payload.isNotEmpty()) {
            for (keyEntry in bbCodes.keys) {
                val key = keyEntry.lowercase()
                payload = payload.replace(key, bbCodes[key].toString())
            }
            payload = parseRandom(parseRotate(payload))
        }
        return d(payload)
    }

    @JvmStatic
    fun isNetworkOnline(context: android.content.Context): Boolean {
        val manager =
            context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnectedOrConnecting
    }

    @JvmStatic
    fun getLocalIpAddress(): String? {
        var ip: String? = null
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val currentAddress = address.hostAddress
                        ip = currentAddress
                        return currentAddress
                    }
                }
            }

            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    val currentAddress = address.hostAddress
                    if (currentAddress.length < 17) {
                        println("IPv4 Address: $currentAddress")
                    } else if (ip == null) {
                        ip = currentAddress
                    }
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
        return ip
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
                    repeated += replacement
                }
                result = result.replace(marker + count + "]", repeated)
            } else {
                break
            }
        }
        return result
    }

    @JvmStatic
    @Throws(IOException::class)
    fun injectSplitPayload(requestPayload: String, outputStream: OutputStream): Boolean {
        if (requestPayload.contains("[delay_split]")) {
            val split = Pattern.quote("[delay_split]").let { Pattern.compile(it).split(requestPayload) }
            for (index in split.indices) {
                val part = split[index]
                if (!injectSimpleSplit(part, outputStream)) {
                    writePayload(outputStream, part)
                    outputStream.flush()
                }
                try {
                    if (index != split.size - 1) Thread.sleep(1000)
                } catch (_: InterruptedException) {
                }
            }
            return true
        } else if (injectSimpleSplit(requestPayload, outputStream)) {
            return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun injectSimpleSplit(requestPayload: String, outputStream: OutputStream): Boolean {
        val delimiters = arrayOf("[split]", "[splitNoDelay]", "[instant_split]", "[delay]", "[split_delay]")
        for (delimiter in delimiters) {
            if (requestPayload.contains(delimiter)) {
                val parts = Pattern.compile(Pattern.quote(delimiter)).split(requestPayload)
                for (index in parts.indices) {
                    writePayload(outputStream, parts[index])
                    outputStream.flush()
                    if ((delimiter == "[delay]" || delimiter == "[split_delay]") &&
                        index != parts.size - 1
                    ) {
                        try {
                            Thread.sleep(1000)
                        } catch (_: InterruptedException) {
                        }
                    }
                }
                return delimiter == "[split_delay]" || delimiter == "[delay]" ||
                    delimiter == "[split]" || delimiter == "[splitNoDelay]" ||
                    delimiter == "[instant_split]"
            }
        }
        return false
    }

    @Throws(IOException::class)
    private fun writePayload(outputStream: OutputStream, value: String) {
        try {
            outputStream.write(value.toByteArray(Charsets.ISO_8859_1))
        } catch (_: UnsupportedOperationException) {
            outputStream.write(value.toByteArray())
        }
    }

    @JvmStatic
    fun parseRotate(payloadInput: String): String {
        var payload = payloadInput
        val matcher = Pattern.compile("\\[rotate=(.*?)\\]").matcher(payload)

        if (lastPayload != payload) {
            restartRotateAndRandom()
            lastPayload = payload
        }

        var index = 0
        while (matcher.find()) {
            val group = matcher.group(1)!!
            val parts = Pattern.compile(";").split(group)
            if (parts.isEmpty()) continue

            var selectedIndex = if (lastRotateList.containsKey(index)) {
                lastRotateList[index]!! + 1
            } else {
                0
            }
            if (selectedIndex >= parts.size) selectedIndex = 0

            val host = parts[selectedIndex]
            payload = payload.replace(matcher.group(0)!!, host)
            lastRotateList[index] = selectedIndex
            index++
        }
        return payload
    }

    @JvmStatic
    fun parseRandom(payloadInput: String): String {
        var payload = payloadInput
        val matcher = Pattern.compile("\\[random=(.*?)\\]").matcher(payload)
        while (matcher.find()) {
            val group = matcher.group(1)!!
            val parts = Pattern.compile(";").split(group)
            if (parts.isEmpty()) continue

            var selectedIndex = Random().nextInt(parts.size)
            if (selectedIndex >= parts.size || selectedIndex < 0) selectedIndex = 0
            payload = payload.replace(matcher.group(0)!!, parts[selectedIndex])
        }
        return payload
    }

    @JvmStatic
    fun restartRotateAndRandom() {
        lastRotateList.clear()
    }

    @JvmStatic
    fun isActiveVpn(context: android.content.Context): Boolean {
        val connectivityManager =
            context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network: Network? = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            val info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)
            info != null && info.isConnectedOrConnecting
        }
    }

    @JvmStatic
    fun setSocket(socket: Socket) {
        output = socket
    }

    @JvmStatic
    fun protect(vpnService: VpnService): Boolean {
        val socket = output
        if (socket == null) {
            addLog("Vpn Protect Socket is null")
            return false
        } else if (socket.isClosed) {
            addLog("Vpn Protect Socket is closed")
            return false
        } else if (!socket.isConnected) {
            addLog("Vpn Protect Socket not connected")
            return false
        } else if (vpnService.protect(socket)) {
            addLog("Vpn Protect Socket has protected")
            return true
        } else {
            addLog("Vpn Protect Failed to protecting socket, reboot this device required")
            return false
        }
    }

    private fun addLog(message: String) {
        AppLogManager.addLog(message)
    }

    @JvmStatic
    fun getAPN(context: android.content.Context): String {
        try {
            val connectivityManager =
                context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (network in connectivityManager.allNetworks) {
                val networkInfo = connectivityManager.getNetworkInfo(network)!!
                if (networkInfo.type == ConnectivityManager.TYPE_WIFI) return "Wi-Fi"
                if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    try {
                        val apn = networkInfo.extraInfo.toString()
                        if (!apn.contains("ims")) return apn
                    } catch (_: Exception) {
                        return "Unknown"
                    }
                }
            }
        } catch (_: Exception) {
            return "Unknown"
        }
        return "Unknown"
    }

    @JvmStatic
    fun getOperatorName(context: android.content.Context): String {
        try {
            val connectivityManager =
                context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi: NetworkInfo? =
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val telephonyManager =
                context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
            assert(wifi != null)
            if (wifi!!.isConnectedOrConnecting) {
                return "Wi-Fi"
            }
            if (Build.VERSION.SDK_INT >= 24) {
                val manager = telephonyManager.createForSubscriptionId(
                    SubscriptionManager.getDefaultDataSubscriptionId()
                )
                return manager.networkOperatorName.toString()
            }
            return telephonyManager.networkOperatorName.toString()
        } catch (_: Exception) {
            return "Default"
        }
    }
}
