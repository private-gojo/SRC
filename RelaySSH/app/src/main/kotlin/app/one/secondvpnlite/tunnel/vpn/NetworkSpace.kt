/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package app.one.secondvpnlite.tunnel.vpn

import android.os.Build
import androidx.annotation.NonNull
import app.one.secondvpnlite.BuildConfig
import java.math.BigInteger
import java.net.Inet6Address
import java.util.Collection
import java.util.Locale
import java.util.PriorityQueue
import java.util.TreeSet
import java.util.Vector

class NetworkSpace {

    class IpAddress : Comparable<IpAddress> {
        private var netAddress: BigInteger
        @JvmField
        var networkMask: Int
        internal var included: Boolean
        private var isV4: Boolean
        private var firstAddress: BigInteger? = null
        private var lastAddress: BigInteger? = null

        override fun compareTo(@NonNull another: IpAddress): Int {
            val comparison = getFirstAddress().compareTo(another.getFirstAddress())
            if (comparison != 0) return comparison

            return when {
                networkMask > another.networkMask -> -1
                another.networkMask == networkMask -> 0
                else -> 1
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other !is IpAddress) return super.equals(other)
            return networkMask == other.networkMask &&
                other.getFirstAddress() == getFirstAddress()
        }

        constructor(ip: CIDRIP, include: Boolean) {
            included = include
            netAddress = BigInteger.valueOf(ip.getInt())
            networkMask = ip.len
            isV4 = true
        }

        constructor(address: Inet6Address, mask: Int, include: Boolean) {
            networkMask = mask
            included = include
            var shift = 128
            netAddress = BigInteger.ZERO
            for (byte in address.address) {
                shift -= 8
                netAddress = netAddress.add(
                    BigInteger.valueOf((byte.toInt() and 0xFF).toLong()).shiftLeft(shift)
                )
            }
            isV4 = false
        }

        fun getLastAddress(): BigInteger {
            if (lastAddress == null) {
                lastAddress = getMaskedAddress(true)
            }
            return lastAddress!!
        }

        fun getFirstAddress(): BigInteger {
            if (firstAddress == null) {
                firstAddress = getMaskedAddress(false)
            }
            return firstAddress!!
        }

        private fun getMaskedAddress(setHostBits: Boolean): BigInteger {
            var numberAddress = netAddress
            val numberBits = if (isV4) 32 - networkMask else 128 - networkMask
            for (index in 0 until numberBits) {
                numberAddress = if (setHostBits) {
                    numberAddress.setBit(index)
                } else {
                    numberAddress.clearBit(index)
                }
            }
            return numberAddress
        }

        override fun toString(): String {
            return if (isV4) {
                String.format(Locale.US, "%s/%d", getIPv4Address(), networkMask)
            } else {
                String.format(Locale.US, "%s/%d", getIPv6Address(), networkMask)
            }
        }

        internal constructor(
            baseAddress: BigInteger,
            mask: Int,
            isIncluded: Boolean,
            ipv4: Boolean
        ) {
            netAddress = baseAddress
            networkMask = mask
            included = isIncluded
            isV4 = ipv4
        }

        fun split(): Array<IpAddress> {
            val firstHalf = IpAddress(
                getFirstAddress(),
                networkMask + 1,
                included,
                isV4
            )
            val secondHalf = IpAddress(
                firstHalf.getLastAddress().add(BigInteger.ONE),
                networkMask + 1,
                included,
                isV4
            )
            if (BuildConfig.DEBUG) {
                assertTrue(secondHalf.getLastAddress() == getLastAddress())
            }
            return arrayOf(firstHalf, secondHalf)
        }

        fun getIPv4Address(): String {
            if (BuildConfig.DEBUG) {
                assertTrue(isV4)
                assertTrue(netAddress.toLong() <= 0xffffffffL)
                assertTrue(netAddress.toLong() >= 0)
            }
            val ip = netAddress.toLong()
            return String.format(
                Locale.US,
                "%d.%d.%d.%d",
                (ip shr 24) % 256,
                (ip shr 16) % 256,
                (ip shr 8) % 256,
                ip % 256
            )
        }

        fun getIPv6Address(): String {
            if (BuildConfig.DEBUG) assertTrue(!isV4)
            var remaining = netAddress
            var ipv6String: String? = null
            var lastPart = true

            while (remaining.compareTo(BigInteger.ZERO) == 1) {
                val part = remaining.mod(BigInteger.valueOf(0x10000)).toLong()
                if (ipv6String != null || part != 0L) {
                    if (ipv6String == null && !lastPart) {
                        ipv6String = ":"
                    }
                    ipv6String = if (lastPart) {
                        String.format(Locale.US, "%x", part, ipv6String)
                    } else {
                        String.format(Locale.US, "%x:%s", part, ipv6String)
                    }
                }
                remaining = remaining.shiftRight(16)
                lastPart = false
            }
            return ipv6String ?: "::"
        }

        fun containsNet(network: IpAddress): Boolean {
            val ourFirst = getFirstAddress()
            val ourLast = getLastAddress()
            val networkFirst = network.getFirstAddress()
            val networkLast = network.getLastAddress()
            val firstContained = ourFirst.compareTo(networkFirst) != 1
            val lastContained = ourLast.compareTo(networkLast) != -1
            return firstContained && lastContained
        }
    }

    @JvmField
    val mIpAddresses: TreeSet<IpAddress> = TreeSet()

    fun getNetworks(included: Boolean): Collection<IpAddress> {
        val ips = Vector<IpAddress>()
        for (ip in mIpAddresses) {
            if (ip.included == included) ips.add(ip)
        }
        return ips
    }

    fun clear() {
        mIpAddresses.clear()
    }

    fun addIP(cidrIp: CIDRIP, include: Boolean) {
        mIpAddresses.add(IpAddress(cidrIp, include))
    }

    fun addIPSplit(cidrIp: CIDRIP, include: Boolean) {
        val newIp = IpAddress(cidrIp, include)
        for (split in newIp.split()) {
            mIpAddresses.add(split)
        }
    }

    fun addIPv6(address: Inet6Address, mask: Int, included: Boolean) {
        mIpAddresses.add(IpAddress(address, mask, included))
    }

    internal fun generateIPList(): TreeSet<IpAddress> {
        val networks = PriorityQueue<IpAddress>(mIpAddresses)
        val ipsDone = TreeSet<IpAddress>()

        var currentNet = networks.poll() ?: return ipsDone

        while (true) {
            val nextNet = networks.poll()
            if (BuildConfig.DEBUG) assertTrue(currentNet != null)

            if (nextNet == null ||
                currentNet.getLastAddress().compareTo(nextNet.getFirstAddress()) == -1
            ) {
                ipsDone.add(currentNet)
                if (nextNet == null) break
                currentNet = nextNet
            } else {
                if (currentNet.getFirstAddress() == nextNet.getFirstAddress() &&
                    currentNet.networkMask >= nextNet.networkMask
                ) {
                    if (currentNet.included == nextNet.included) {
                        currentNet = nextNet
                    } else {
                        val newNets = nextNet.split()
                        if (!networks.contains(newNets[1])) {
                            networks.add(newNets[1])
                        }
                        if (newNets[0].getLastAddress() == currentNet.getLastAddress()) {
                            if (BuildConfig.DEBUG) {
                                assertTrue(newNets[0].networkMask == currentNet.networkMask)
                            }
                        } else if (!networks.contains(newNets[0])) {
                            networks.add(newNets[0])
                        }
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        assertTrue(currentNet.networkMask < nextNet.networkMask)
                        assertTrue(
                            nextNet.getFirstAddress().compareTo(currentNet.getFirstAddress()) == 1
                        )
                        assertTrue(
                            currentNet.getLastAddress().compareTo(nextNet.getLastAddress()) != -1
                        )
                    }
                    if (currentNet.included != nextNet.included) {
                        val newNets = currentNet.split()
                        if (newNets[1].networkMask == nextNet.networkMask) {
                            if (BuildConfig.DEBUG) {
                                assertTrue(
                                    newNets[1].getFirstAddress() == nextNet.getFirstAddress()
                                )
                                assertTrue(
                                    newNets[1].getLastAddress() == currentNet.getLastAddress()
                                )
                            }
                            networks.add(nextNet)
                        } else {
                            networks.add(newNets[1])
                            networks.add(nextNet)
                        }
                        currentNet = newNets[0]
                    }
                }
            }
        }
        return ipsDone
    }

    fun getPositiveIPList(): Collection<IpAddress> {
        val ipsSorted = generateIPList()
        val ips = Vector<IpAddress>()
        for (address in ipsSorted) {
            if (address.included) ips.add(address)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            for (originalIp in mIpAddresses) {
                if (!originalIp.included) continue
                if (ipsSorted.contains(originalIp)) continue

                var skipIp = false
                for (calculatedIp in ipsSorted) {
                    if (!calculatedIp.included && originalIp.containsNet(calculatedIp)) {
                        skipIp = true
                        break
                    }
                }
                if (skipIp) continue
                ips.add(originalIp)
            }
        }
        return ips
    }

    companion object {
        @JvmStatic
        internal fun assertTrue(value: Boolean) {
            if (!value) throw IllegalStateException()
        }
    }
}
