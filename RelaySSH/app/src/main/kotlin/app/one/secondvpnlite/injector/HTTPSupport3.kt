package app.one.secondvpnlite.injector

import android.content.SharedPreferences
import app.one.secondvpnlite.R
import app.one.secondvpnlite.SecondVPN
import app.one.secondvpnlite.logs.AppLogManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Random
import java.util.regex.Matcher
import java.util.regex.Pattern

class HTTPSupport3(private val incoming: Socket) {
    private var netDataString: String? = null
    private var bugHostRotate: Array<String>? = null
    private var bugHostRotate2: Array<String>? = null
    private var bugHostRotate3: Array<String>? = null
    private var countRotate = 0
    private var countRotate2 = 0
    private var countRotate3 = 0
    private var coA = 0
    private var coB = 0
    private var coC = 0
    private var coD = 0
    private var cow: String? = null
    private var cox: Array<String>? = null
    private var coy: Array<String>? = null
    private var coz: Array<String>? = null
    private var h = 0

    private var sp: SharedPreferences? = null

    fun socket(): Socket? {
        var socket: Socket? = null
        try {
            val remote = SecondVPN.getProxyIPDomain()
            val remoteParts = remote.trim().split(":")
            val proxyHost = remoteParts[0].trim()
            var proxyPort = 80
            if (remoteParts.size > 1) {
                try {
                    proxyPort = remoteParts[1].trim().toInt()
                } catch (_: NumberFormatException) {
                    proxyPort = 80
                }
            }

            socket = Socket(proxyHost, proxyPort)
            val payloadTemplate = SecondVPN.getPayloadKey()
            val outputStream = socket.getOutputStream()
            val bufferedReader = BufferedReader(InputStreamReader(incoming.getInputStream()))
            val readLine = bufferedReader.readLine()
            val requestLine = readLine!!
            val request = requestLine.split(" ")
            val host: String
            var destinationPort = "80"
            if (request[1].startsWith("http") || request[1].indexOf(":") <= 0) {
                host = request[1]
            } else {
                val hostParts = request[1].split(":")
                host = hostParts[0]
                destinationPort = hostParts[1]
            }

            netDataString = payloadTemplate.replace("realData", "netData")
            var payload = buildNetDataPayload(
                netDataString!!,
                request,
                host,
                destinationPort,
                requestLine + "\r\n\r\n",
                requestLine + "\r\n\r\n",
                false
            )

            payload = applySocketRotations(payload)
            payload = expandRepeatedMarkers(payload)

            writeSocketPayload(payload, outputStream)
            AppLogManager.addLog("HTTP: Sending payload")
        } catch (_: Exception) {
            AppLogManager.addLog("HTTP: Fail to remote proxy")
        }

        countRotate++
        countRotate2++
        countRotate3++
        return socket
    }

    private fun buildNetDataPayload(
        templateInput: String,
        request: List<String>,
        host: String,
        destinationPort: String,
        rawRequest: String,
        realRawRequest: String,
        adjustZeroIndex: Boolean
    ): String {
        val template = templateInput
        val method = request[0]
        val hostPort = request[1]
        val protocol = request[2]
        val markerIndex = template.indexOf("netData")

        if (markerIndex < 0) {
            return replacePayloadTokens(
                template, method, hostPort, host, destinationPort,
                protocol, rawRequest, realRawRequest
            )
        }

        var index = markerIndex
        if (adjustZeroIndex && index == 0) index = 1

        if (template.substring(markerIndex + 7, markerIndex + 8) == "@") {
            val matcher = Pattern.compile("\\[.*?@(.*?)\\]").matcher(template)
            var group = ""
            if (matcher.find()) group = matcher.group(1)
            val marker = "[netData@${group.trim()}]"
            val replaced = template.replace(
                marker,
                "$method $hostPort@${group.trim()} $protocol"
            )
            return replacePayloadTokens(
                replaced, method, hostPort, host, destinationPort,
                protocol, rawRequest, realRawRequest
            )
        }

        val groupMatcher = Pattern.compile("\\[(.*?)@.*?\\]").matcher(template)
        var group = ""
        if (groupMatcher.find()) group = groupMatcher.group(1)

        if (template.substring(index - 1, index) == "@") {
            val marker = "[${group.trim()}@netData]"
            val replaced = template.replace(
                marker,
                "$method ${group.trim()}@$hostPort $protocol"
            )
            return replacePayloadTokens(
                replaced, method, hostPort, host, destinationPort,
                protocol, rawRequest, realRawRequest
            )
        }

        val replaced = template.replace("[netData]", rawRequest)
        return replacePayloadTokens(
            replaced, method, hostPort, host, destinationPort,
            protocol, rawRequest, realRawRequest
        )
    }

    private fun replacePayloadTokens(
        value: String,
        method: String,
        hostPort: String,
        host: String,
        destinationPort: String,
        protocol: String,
        rawRequest: String,
        realRawRequest: String
    ): String {
        return value
            .replace("[METHOD]", method)
            .replace("[method]", method)
            .replace("[SSH]", hostPort)
            .replace("[IP_PORT]", hostPort)
            .replace("[ip_port]", hostPort)
            .replace("[IP]", host)
            .replace("[ip]", host)
            .replace("[PORT]", destinationPort)
            .replace("[port]", destinationPort)
            .replace("[cr]", "\r")
            .replace("[lf]", "\n")
            .replace("[crlf]", "\r\n")
            .replace("[lfcr] ", "\n\r")
            .replace("[protocol]", protocol)
            .replace("[host]", host)
            .replace("[host_port]", hostPort)
            .replace("[ssh]", hostPort)
            .replace("[ua]", ua())
            .replace("[raw]", rawRequest)
            .replace("[real_raw]", realRawRequest)
            .replace("[auth]", auth())
            .replace("\\r", "\r")
            .replace("\\n", "\n")
    }

    private fun applySocketRotations(input: String): String {
        var result = input
        var matcher = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            bugHostRotate2 = group.split(";").toTypedArray()
            val values = bugHostRotate2!!
            if (countRotate2 + 1 > values.size) countRotate2 = 0
            result = result.replace("[rotation_method=$group]", values[countRotate2])
            matcher = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(result)
        }

        matcher = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            bugHostRotate = group.split(";").toTypedArray()
            val values = bugHostRotate!!
            if (countRotate + 1 > values.size) countRotate = 0
            result = result.replace("[rotation=$group]", values[countRotate])
            matcher = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(result)
        }

        matcher = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            bugHostRotate3 = group.split(";").toTypedArray()
            val values = bugHostRotate3!!
            if (countRotate3 + 1 > values.size) countRotate3 = 0
            result = result.replace("[rotate=$group]", values[countRotate3])
            matcher = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(result)
        }
        return result
    }

    private fun applySocket2Rotations(input: String): String {
        var result = input
        var matcher = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            cox = group.split(";").toTypedArray()
            if (coA + 1 > cox!!.size) coA = 0
            result = result.replace("[rotation_method=$group]", cox!![coA])
            matcher = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(result)
        }

        matcher = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            coy = group.split(";").toTypedArray()
            if (coB + 1 > coy!!.size) coB = 0
            result = result.replace("[rotation=$group]", coy!![coB])
            matcher = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(result)
        }

        matcher = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(result)
        while (matcher.find()) {
            val group = matcher.group(1)
            coz = group.split(";").toTypedArray()
            if (coC + 1 > coz!!.size) coC = 0
            result = result.replace("[rotate=$group]", coz!![coC])
            matcher = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(result)
        }
        return result
    }

    private fun writeSocketPayload(payloadInput: String, outputStream: OutputStream) {
        val payload = d(payloadInput)
        val delimiters = arrayOf(
            "[split]", "[splitNoDelay]", "[instant_split]",
            "[delay]", "[delay_split]", "[split_delay]"
        )
        val delimiter = delimiters.firstOrNull { payload.contains(it) }
        if (delimiter == null) {
            AppLogManager.addLog(payload.replace("\r", "\\r").replace("\n", "\\n"))
            outputStream.write(payload.toByteArray())
            outputStream.flush()
            return
        }

        val parts = Pattern.compile(Pattern.quote(delimiter)).split(payload)
        for (index in parts.indices) {
            if (delimiter != "[split]") {
                AppLogManager.addLog(
                    parts[index].replace("\r", "\\r").replace("\n", "\\n")
                )
            }
            outputStream.write(parts[index].toByteArray())
            outputStream.flush()
            if ((delimiter == "[delay]" || delimiter == "[delay_split]" ||
                    delimiter == "[split_delay]") && index != parts.size - 1
            ) {
                Thread.sleep(1000)
            }
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
            if (!matcher.find()) break
            val count = matcher.group(1).toInt()
            val repeated = replacement.repeat(count)
            result = result.replace(marker + count + "]", repeated)
        }
        return result
    }

    private fun auth(): String {
        return ""
    }

    fun ua(): String =
        System.getProperty("http.agent")
            ?: "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36"

    fun inject(): Socket? {
        try {
            val bufferedReader = BufferedReader(InputStreamReader(incoming.getInputStream()))
            val requestBuilder = StringBuilder()
            while (true) {
                val readLine = bufferedReader.readLine()
                if (readLine != null && readLine.isNotEmpty()) {
                    requestBuilder.append(readLine).append("\r\n")
                }
                if (requestBuilder.toString().isEmpty()) {
                    AppLogManager.addLog("Get request data failed, empty requestline")
                    return null
                }
                val payload = c(requestBuilder.toString())
                if (payload != null) {
                    AppLogManager.addLog("Try connect...")
                    val address = SecondVPN.getProxyIPDomain().split(":")
                    val socket = Socket()
                    socket.connect(InetSocketAddress(address[0], address[1].toInt()), 15000)
                    injectPayload(payload, socket)
                    return socket
                }
                return null
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun c(value: String?): String? {
        if (value.isNullOrEmpty()) return null
        return try {
            val firstLine = value.split("\r\n".toRegex())[0]
            val request = firstLine.split(" ")
            val hostPort = request[1].split(":")
            val host = hostPort[0]
            val port = hostPort[1]
            d(SecondVPN.getPayloadKey())
                .replace("[real_raw]", value)
                .replace("[raw]", firstLine)
                .replace("[method]", request[0])
                .replace("[host_port]", request[1])
                .replace("[host]", host)
                .replace("[port]", port)
                .replace("[protocol]", request[2])
                .replace("[ua]", ua())
                .replace("[cr]", "\r")
                .replace("[lf]", "\n")
                .replace("[crlf]", "\r\n")
                .replace("[lfcr]", "\n\r")
                .replace("\\r", "\r")
                .replace("\\n", "\n")
        } catch (_: Exception) {
            null
        }
    }

    @Throws(Exception::class)
    private fun injectPayload(payloadInput: String, socket: Socket) {
        AppLogManager.addLog("<b><font color=#49C53C>Sending payload...</font></b>")
        var payload = payloadInput
        val outputStream = socket.getOutputStream()

        if (payload.contains("[random]")) {
            val parts = payload.split(Pattern.quote("[random]").toRegex()).toTypedArray()
            payload = parts[Random().nextInt(parts.size)]
        }
        if (payload.contains("[repeat]")) {
            val parts = payload.split(Pattern.quote("[repeat]").toRegex()).toTypedArray()
            payload = parts[h]
            h++
            if (h > parts.size - 1) h = 0
        }

        if (payload.contains("[split_delay]")) {
            val parts = payload.split(Pattern.quote("[split_delay]").toRegex()).toTypedArray()
            for (part in parts) {
                if (writeSplitPart(part, socket, outputStream)) {
                    outputStream.write(part.toByteArray())
                    outputStream.flush()
                }
            }
        } else if (writeSplitPart(payload, socket, outputStream)) {
            outputStream.write(payload.toByteArray())
            outputStream.flush()
        }
    }

    private fun writeSplitPart(value: String, socket: Socket, outputStream: OutputStream): Boolean {
        if (!value.contains("[split]")) return true
        val parts = value.split(Pattern.quote("[split]").toRegex())
        for (part in parts) {
            outputStream.write(part.toByteArray())
            outputStream.flush()
        }
        return false
    }

    fun b(value: String): String = "*".repeat(value.length)

    fun socket2(): Socket? {
        var socket: Socket? = null
        var index = 0
        try {
            val payloadTemplate = SecondVPN.getPayloadKey()
            val remote = SecondVPN.getProxyIPDomain()
            val remoteParts = remote.trim().split(":")
            val proxyHost = remoteParts[0].trim()
            var proxyPort = 80
            if (remoteParts.size > 1) {
                try {
                    proxyPort = remoteParts[1].trim().toInt()
                } catch (_: NumberFormatException) {
                    proxyPort = 80
                }
            }

            socket = Socket(proxyHost, proxyPort)
            val outputStream = socket.getOutputStream()
            val readLine = BufferedReader(
                InputStreamReader(incoming.getInputStream())
            ).readLine()!!
            val request = readLine.split(" ")
            var host: String = request[1]
            var destinationPort: String = "80"

            if (request[0] == "CONNECT") {
                val split = request[1].split(":")
                host = split[0]
                destinationPort = if (split.size < 2) "443" else split[1]
            }

            var rotatingPayload = payloadTemplate
            if (rotatingPayload.contains("[random]")) {
                val parts = rotatingPayload.split(Pattern.quote("[random]").toRegex())
                    .toTypedArray()
                rotatingPayload = parts[Random().nextInt(parts.size)]
            }
            cow = rotatingPayload

            rotatingPayload = payloadTemplate
            if (rotatingPayload.contains("[repeat]")) {
                val parts = rotatingPayload.split(Pattern.quote("[repeat]").toRegex())
                    .toTypedArray()
                rotatingPayload = parts[coD]
                if (coD + 1 > parts.size) coD = 0
            }
            cow = rotatingPayload

            cow = payloadTemplate.replace("realData", "netData")
            val template = cow!!
            val markerIndex = template.indexOf("netData")
            val payload: String

            if (markerIndex < 0) {
                payload = replacePayloadTokens(
                    template, request[0], request[1], host, destinationPort,
                    request[2], readLine + "\r\n\r\n", readLine + "\r\n\r\n"
                )
            } else if (template.substring(markerIndex + 7, markerIndex + 8) == "@") {
                val matcher = Pattern.compile("\\[.*?@(.*?)\\]").matcher(template)
                var group = ""
                if (matcher.find()) group = matcher.group(1)
                val marker = "[netData@${group.trim()}]"
                val replaced = template.replace(
                    marker,
                    "${request[0]} ${request[1]}@${group.trim()} ${request[2]}"
                )
                payload = replacePayloadTokens(
                    replaced, request[0], request[1], host, destinationPort,
                    request[2], readLine + "\r\n\r\n", readLine + "\r\n\r\n"
                )
            } else {
                val adjusted = if (markerIndex == 0) 1 else markerIndex
                val groupMatcher = Pattern.compile("\\[(.*?)@.*?\\]").matcher(template)
                var group = ""
                if (groupMatcher.find()) group = groupMatcher.group(1)
                if (template.substring(adjusted - 1, adjusted) == "@") {
                    val marker = "[${group.trim()}@netData]"
                    val replaced = template.replace(
                        marker,
                        "${request[0]} ${group.trim()}@${request[1]} ${request[2]}"
                    )
                    payload = replacePayloadTokens(
                        replaced, request[0], request[1], host, destinationPort,
                        request[2], readLine + "\r\n\r\n", readLine + "\r\n\r\n"
                    )
                } else {
                    val replaced = template.replace("[netData]", readLine)
                    payload = replacePayloadTokens(
                        replaced, request[0], request[1], host, destinationPort,
                        request[2], readLine + "\r\n\r\n", readLine + "\r\n\r\n"
                    )
                }
            }

            var result = applySocket2Rotations(payload)
            result = d(result)

            when {
                result.contains("[split]") -> {
                    val parts = result.split("\\[split\\]".toRegex()).toTypedArray()
                    while (index < parts.size) {
                        outputStream.write(parts[index].toByteArray())
                        outputStream.flush()
                        index++
                    }
                }
                result.contains("[splitNoDelay]") -> {
                    val parts = result.split("\\[splitNoDelay\\]".toRegex()).toTypedArray()
                    while (index < parts.size) {
                        outputStream.write(parts[index].toByteArray())
                        outputStream.flush()
                        index++
                    }
                }
                result.contains("[instant_split]") -> {
                    val parts = result.split("\\[instant_split\\]".toRegex()).toTypedArray()
                    while (index < parts.size) {
                        outputStream.write(parts[index].toByteArray())
                        outputStream.flush()
                        index++
                    }
                }
                result.contains("[delay]") -> {
                    val parts = result.split("\\[delay\\]".toRegex()).toTypedArray()
                    while (index < parts.size) {
                        outputStream.write(parts[index].toByteArray())
                        outputStream.flush()
                        if (index != parts.size - 1) Thread.sleep(1000)
                        index++
                    }
                }
                result.contains("[delay_split]") -> {
                    val parts = result.split("\\[delay_split\\]".toRegex()).toTypedArray()
                    while (index < parts.size) {
                        outputStream.write(parts[index].toByteArray())
                        outputStream.flush()
                        if (index != parts.size - 1) Thread.sleep(1000)
                        index++
                    }
                }
                result.contains("[split_delay]") -> {
                    val parts = result.split("\\[split_delay\\]".toRegex()).toTypedArray()
                    for (partIndex in parts.indices) {
                        outputStream.write(parts[partIndex].toByteArray())
                        outputStream.flush()
                        if (partIndex != parts.size - 1) Thread.sleep(1000)
                    }
                }
                else -> {
                    outputStream.write(result.toByteArray())
                    outputStream.flush()
                }
            }

            AppLogManager.addLog(R.string.state_proxy_inject)
            coB++
            coA++
            coC++
            coD++
            return socket
        } catch (error: Exception) {
            AppLogManager.addLog("Error: $error")
            return null
        }
    }
}
