package app.one.secondvpnlite.tunnel.vpn

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.regex.Pattern

object VpnUtils {
    @JvmStatic
    @Throws(IOException::class)
    fun findProcessId(command: String): Int {
        val commands = arrayOf("ps -ef", "ps -A", "toolbox ps")
        for (commandLine in commands) {
            val process = Runtime.getRuntime().exec(commandLine)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line!!
                if (!currentLine.contains("PID") && currentLine.contains(command)) {
                    val parts = currentLine.split("\\s+".toRegex())
                    try {
                        return parts[1].toInt()
                    } catch (_: NumberFormatException) {
                        return parts[0].toInt()
                    } finally {
                        try {
                            process.destroy()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        return -1
    }

    @JvmStatic
    @Throws(Exception::class)
    fun killProcess(fileProcBin: File) {
        killProcess(fileProcBin, "-9")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun killProcess(fileProcBin: File, signal: String): Int {
        var processId = -1
        var killAttempts = 0

        while (findProcessId(fileProcBin.name).also { processId = it } != -1) {
            killAttempts++
            val pidString = processId.toString()
            val commands = arrayOf("", "busybox ", "toolbox ")

            for (prefix in commands) {
                try {
                    Runtime.getRuntime().exec(prefix + "killall " + signal + " " + fileProcBin.name)
                } catch (_: IOException) {
                }
                try {
                    Runtime.getRuntime().exec(
                        prefix + "killall " + signal + " " + fileProcBin.canonicalPath
                    )
                } catch (_: IOException) {
                }
            }

            killProcess(pidString, signal)
            try {
                Thread.sleep(1000)
            } catch (_: InterruptedException) {
            }
            if (killAttempts > 4) {
                throw Exception("Cannot kill: " + fileProcBin.absolutePath)
            }
        }
        return processId
    }

    @JvmStatic
    @Throws(Exception::class)
    fun killProcess(pidString: String, signal: String) {
        val commands = arrayOf("", "toolbox ", "busybox ")
        for (prefix in commands) {
            try {
                Runtime.getRuntime().exec(prefix + "kill " + signal + " " + pidString)
            } catch (_: IOException) {
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getActiveNetworkDnsResolver(context: Context): List<String> {
        val dnsResolvers = getActiveNetworkDnsResolvers(context)
        if (dnsResolvers.isEmpty()) {
            throw Exception("no active network DNS resolver")
        }

        val result = ArrayList<String>()
        var max = 2
        val iterator = dnsResolvers.iterator()
        while (iterator.hasNext()) {
            var dnsResolver = iterator.next().toString()
            if (dnsResolver.startsWith("/")) {
                dnsResolver = dnsResolver.substring(1)
            }
            if (dnsResolver.contains(":")) continue
            result.add(dnsResolver)
            max--
            if (max <= 0) break
        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(Exception::class)
    private fun getActiveNetworkDnsResolvers(context: Context): Collection<InetAddress> {
        val errorMessage = "getActiveNetworkDnsResolvers failed"
        val dnsAddresses = ArrayList<InetAddress>()
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val linkPropertiesClass = Class.forName("android.net.LinkProperties")
            val method: Method = ConnectivityManager::class.java.getMethod(
                "getActiveLinkProperties",
                *emptyArray<Class<*>>()
            )
            val linkProperties = method.invoke(connectivityManager)
            if (linkProperties != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val getDnsesMethod = linkPropertiesClass.getMethod(
                        "getDnses",
                        *emptyArray<Class<*>>()
                    )
                    val dnses = getDnsesMethod.invoke(linkProperties) as Collection<*>
                    for (dns in dnses) {
                        dnsAddresses.add(dns as InetAddress)
                    }
                } else {
                    for (dns in (linkProperties as LinkProperties).dnsServers) {
                        dnsAddresses.add(dns)
                    }
                }
            }
        } catch (error: ClassNotFoundException) {
            throw Exception(errorMessage, error)
        } catch (error: NoSuchMethodException) {
            throw Exception(errorMessage, error)
        } catch (error: IllegalArgumentException) {
            throw Exception(errorMessage, error)
        } catch (error: IllegalAccessException) {
            throw Exception(errorMessage, error)
        } catch (error: InvocationTargetException) {
            throw Exception(errorMessage, error)
        } catch (error: NullPointerException) {
            throw Exception(errorMessage, error)
        }
        return dnsAddresses
    }

    @JvmStatic
    fun getNetworkDnsServer(context: Context): List<String> {
        val dnsResolver = ArrayList<String>()
        try {
            dnsResolver.addAll(getDns(context))
        } catch (_: Exception) {
            dnsResolver.add(DEFAULT_PRIMARY_DNS_SERVER)
            dnsResolver.add(DEFAULT_SECONDARY_DNS_SERVER)
        }
        return dnsResolver
    }

    private fun getDns(context: Context): List<String> {
        val dnsResolver = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val dnsList: String
            try {
                dnsList = connectivityManager
                    .getLinkProperties(connectivityManager.activeNetwork)!!
                    .dnsServers
                    .toString()
            } catch (error: Exception) {
                error.printStackTrace()
                dnsList = "8.8.8.8"
            }
            val networkDns = dnsList
                .replace("[", "")
                .replace("]", "")
                .replace("/", "")
                .replace(" ", "")
            val dnsListItems = ArrayList(Arrays.asList(*networkDns.split(",".toRegex()).toTypedArray()))
            dnsResolver.add(dnsListItems[0])
        } else {
            dnsResolver.add("8.8.8.8")
        }
        return dnsResolver
    }

    @JvmStatic
    fun isIPv4Address(input: String): Boolean = IPV4_PATTERN.matcher(input).matches()

    @JvmStatic
    fun isIPv6StdAddress(input: String): Boolean = IPV6_STD_PATTERN.matcher(input).matches()

    @JvmStatic
    fun isIPv6HexCompressedAddress(input: String): Boolean =
        IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches()

    @JvmStatic
    fun isIPv6Address(input: String): Boolean =
        isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input)

    class PrivateAddress(
        @JvmField val mIpAddress: String,
        @JvmField val mSubnet: String,
        @JvmField val mPrefixLength: Int,
        @JvmField val mRouter: String
    )

    @JvmStatic
    @Throws(Exception::class)
    fun selectPrivateAddress(): PrivateAddress {
        val candidates = HashMap<String, PrivateAddress>()
        candidates["10"] = PrivateAddress("10.0.0.1", "10.0.0.0", 8, "10.0.0.2")
        candidates["172"] = PrivateAddress("172.16.0.1", "172.16.0.0", 12, "172.16.0.2")
        candidates["192"] = PrivateAddress("192.168.0.1", "192.168.0.0", 16, "192.168.0.2")
        candidates["169"] = PrivateAddress("169.254.1.1", "169.254.1.0", 24, "169.254.1.2")

        val networkInterfaces: List<NetworkInterface>
        try {
            networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        } catch (error: SocketException) {
            error.printStackTrace()
            throw Exception("selectPrivateAddress failed", error)
        }

        for (networkInterface in networkInterfaces) {
            for (inetAddress in Collections.list(networkInterface.inetAddresses)) {
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    val ipAddress = inetAddress.hostAddress
                    if (ipAddress.startsWith("10.")) {
                        candidates.remove("10")
                    } else if (
                        ipAddress.length >= 6 &&
                        ipAddress.substring(0, 6).compareTo("172.16") >= 0 &&
                        ipAddress.substring(0, 6).compareTo("172.31") <= 0
                    ) {
                        candidates.remove("172")
                    } else if (ipAddress.startsWith("192.168")) {
                        candidates.remove("192")
                    }
                }
            }
        }

        if (candidates.isNotEmpty()) {
            return candidates.values.iterator().next()
        }
        throw Exception("no private address available")
    }

    private fun isPortAvailable(port: Int): Boolean {
        val socket = Socket()
        val socketAddress: SocketAddress = InetSocketAddress("127.0.0.1", port)
        try {
            socket.connect(socketAddress, 1000)
            return false
        } catch (_: SocketTimeoutException) {
            return false
        } catch (_: IOException) {
            return true
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    @JvmStatic
    fun findAvailablePort(startPort: Int, maxIncrement: Int): Int {
        for (port in startPort until (startPort + maxIncrement)) {
            if (isPortAvailable(port)) return port
        }
        return 0
    }

    private const val DEFAULT_PRIMARY_DNS_SERVER = "8.8.8.8"
    private const val DEFAULT_SECONDARY_DNS_SERVER = "8.8.4.4"

    private val IPV4_PATTERN: Pattern = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"
    )
    private val IPV6_STD_PATTERN: Pattern = Pattern.compile(
        "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    )
    private val IPV6_HEX_COMPRESSED_PATTERN: Pattern = Pattern.compile(
        "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"
    )
}
