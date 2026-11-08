package app.one.secondvpnlite.tunnel.vpn

import java.util.Locale

class CIDRIP {
    @JvmField
    var mIp: String

    @JvmField
    var len: Int

    constructor(ip: String, mask: String) {
        mIp = ip
        len = calculateLenFromMask(mask)
    }

    constructor(address: String, prefixLength: Int) {
        len = prefixLength
        mIp = address
    }

    override fun toString(): String =
        String.format(Locale.ENGLISH, "%s/%d", mIp, len)

    fun normalise(): Boolean {
        val ip = getInt(mIp)
        val newIp = ip and (0xffffffffL shl (32 - len))
        if (newIp != ip) {
            mIp = String.format(
                Locale.US,
                "%d.%d.%d.%d",
                (newIp and 0xff000000L) shr 24,
                (newIp and 0xff0000L) shr 16,
                (newIp and 0xff00L) shr 8,
                newIp and 0xffL
            )
            return true
        }
        return false
    }

    fun getInt(): Long = getInt(mIp)

    companion object {
        @JvmStatic
        fun calculateLenFromMask(mask: String): Int {
            var netmask = getInt(mask)
            netmask += 1L shl 32

            var lenZeros = 0
            while ((netmask and 0x1L) == 0L) {
                lenZeros++
                netmask = netmask shr 1
            }

            return if (netmask != (0x1ffffffffL shr lenZeros)) {
                32
            } else {
                32 - lenZeros
            }
        }

        @JvmStatic
        fun getInt(ipaddr: String): Long {
            val parts = ipaddr.split(".")
            var ip = 0L
            ip += parts[0].toLong() shl 24
            ip += parts[1].toInt().toLong() shl 16
            ip += parts[2].toInt().toLong() shl 8
            ip += parts[3].toInt().toLong()
            return ip
        }
    }
}
