package app.one.secondvpnlite.tunnel

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class IpAddress {
    private var privateAddressValue: String = "172.16.0.1"

    init {
        val candidates = arrayListOf(
            "10.0.0.1",
            "172.16.0.1",
            "192.168.0.1",
            "169.254.1.1"
        )

        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            if (networkInterfaces != null) {
                for (networkInterface in Collections.list(networkInterfaces)) {
                    for (inetAddress in Collections.list(networkInterface.inetAddresses)) {
                        if (inetAddress is Inet4Address) {
                            val hostAddress = inetAddress.hostAddress
                            if (hostAddress.startsWith("10.")) {
                                candidates.remove("10.0.0.1")
                            } else if (
                                hostAddress.length >= 6 &&
                                hostAddress.substring(0, 6).compareTo("172.16") >= 0 &&
                                hostAddress.substring(0, 6).compareTo("172.31") <= 0
                            ) {
                                candidates.remove("172.16.0.1")
                            } else if (hostAddress.startsWith("192.168")) {
                                candidates.remove("192.168.0.1")
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Preserve the original fallback behavior.
        }

        privateAddressValue = candidates.firstOrNull() ?: "172.16.0.1"
    }

    fun getPrivateAddress(): String = privateAddressValue

    fun getPrefixLength(): Int = when (privateAddressValue) {
        "10.0.0.1" -> 8
        "172.16.0.1" -> 12
        "192.168.0.1" -> 16
        "169.254.1.1" -> 24
        else -> 0
    }

    fun getPrivateAddressRouter(): String? = when (privateAddressValue) {
        "10.0.0.1" -> "10.0.0.2"
        "172.16.0.1" -> "172.16.0.2"
        "192.168.0.1" -> "192.168.0.2"
        "169.254.1.1" -> "169.254.1.2"
        else -> null
    }
}
