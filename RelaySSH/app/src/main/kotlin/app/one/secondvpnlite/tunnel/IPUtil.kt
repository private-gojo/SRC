package app.one.secondvpnlite.tunnel

import android.util.Log
import androidx.annotation.NonNull
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.ArrayList
import kotlin.math.floor
import kotlin.math.ln

object IPUtil {
    private const val TAG = "NetGuard.IPUtil"

    @JvmStatic
    @Throws(UnknownHostException::class)
    fun toCIDR(start: String, end: String): List<CIDR> =
        toCIDR(InetAddress.getByName(start), InetAddress.getByName(end))

    @JvmStatic
    fun toCIDR(start: InetAddress, end: InetAddress): List<CIDR> {
        val result = ArrayList<CIDR>()
        Log.i(TAG, "toCIDR(${start.hostAddress},${end.hostAddress})")

        var from = inet2long(start)
        val to = inet2long(end)
        while (to >= from) {
            var prefix = 32
            while (prefix > 0) {
                val mask = prefix2mask(prefix - 1)
                if ((from and mask) != from) break
                prefix--
            }

            val max = (32 - floor(ln((to - from + 1).toDouble()) / ln(2.0))).toInt()
            if (prefix < max) prefix = max

            result.add(CIDR(long2inet(from)!!, prefix))
            from += 1L shl (32 - prefix)
        }

        for (cidr in result) {
            Log.i(TAG, cidr.toString())
        }
        return result
    }

    private fun prefix2mask(bits: Int): Long =
        (0xFFFFFFFF00000000L shr bits) and 0xFFFFFFFFL

    private fun inet2long(address: InetAddress?): Long {
        var result = 0L
        if (address != null) {
            for (byte in address.address) {
                result = (result shl 8) or (byte.toLong() and 0xFFL)
            }
        }
        return result
    }

    private fun long2inet(address: Long): InetAddress? {
        return try {
            var value = address
            val bytes = ByteArray(4)
            for (index in bytes.indices.reversed()) {
                bytes[index] = (value and 0xFFL).toByte()
                value = value shr 8
            }
            InetAddress.getByAddress(bytes)
        } catch (_: UnknownHostException) {
            null
        }
    }

    @JvmStatic
    fun minus1(address: InetAddress): InetAddress? =
        long2inet(inet2long(address) - 1)

    @JvmStatic
    fun plus1(address: InetAddress): InetAddress? =
        long2inet(inet2long(address) + 1)

    class CIDR : Comparable<CIDR> {
        @JvmField
        var address: InetAddress? = null

        @JvmField
        var prefix: Int = 0

        constructor(address: InetAddress, prefix: Int) {
            this.address = address
            this.prefix = prefix
        }

        constructor(ip: String, prefix: Int) {
            try {
                address = InetAddress.getByName(ip)
                this.prefix = prefix
            } catch (exception: UnknownHostException) {
                Log.e(TAG, exception.toString() + "\n" + Log.getStackTraceString(exception))
            }
        }

        fun getStart(): InetAddress? =
            long2inet(inet2long(address) and prefix2mask(prefix))

        fun getEnd(): InetAddress? =
            long2inet(
                (inet2long(address) and prefix2mask(prefix)) +
                    (1L shl (32 - prefix)) - 1
            )

        override fun toString(): String {
            val currentAddress = address!!
            return currentAddress.hostAddress + "/" + prefix + "=" +
                getStart()!!.hostAddress + "..." + getEnd()!!.hostAddress
        }

        override fun compareTo(@NonNull other: CIDR): Int {
            val current = inet2long(address)
            val compared = inet2long(other.address)
            return current.compareTo(compared)
        }
    }
}
