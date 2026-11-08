package app.one.secondvpnlite.tunnel.vpn

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import app.one.secondvpnlite.tunnel.CustomNativeLoader
import app.one.secondvpnlite.tunnel.StreamGobbler
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

class Tun2Socks(
    private val mContext: Context,
    private val mVpnInterfaceFileDescriptor: ParcelFileDescriptor?,
    private val mVpnInterfaceMTU: Int,
    private val mVpnIpAddress: String,
    private val mVpnNetMask: String,
    private val mSocksServerAddress: String,
    private val mUdpgwServerAddress: String?,
    private val mDnsResolverAddress: String?,
    private val mUdpgwTransparentDNS: Boolean
) : Thread(), StreamGobbler.OnLineListener {

    private var mListener: OnTun2SocksListener? = null
    private var tun2SocksProcess: Process? = null
    private var fileTun2Socks: File? = null

    interface OnTun2SocksListener {
        fun onStart()
        fun onStop()
    }

    override fun run() {
        println("Tun2Socks: Iniciando Tun2Socks")
        println("Tun2Socks: vpnInterfaceFileDescriptor: $mVpnInterfaceFileDescriptor")
        println("Tun2Socks: vpnInterfaceMTU: $mVpnInterfaceMTU")
        println("Tun2Socks: vpnIpAddress: $mVpnIpAddress")
        println("Tun2Socks: vpnNetMask: $mVpnNetMask")
        println("Tun2Socks: socksServerAddress: $mSocksServerAddress")
        println("Tun2Socks: udpgwServerAddress: $mUdpgwServerAddress")
        println("Tun2Socks: dnsResolverAddress: $mDnsResolverAddress")
        println("Tun2Socks: udpgwTransparentDNS: $mUdpgwTransparentDNS")

        mListener?.onStart()
        try {
            val command = StringBuilder()
            fileTun2Socks = CustomNativeLoader.loadNativeBinary(
                mContext,
                TUN2SOCKS_BIN,
                File(mContext.filesDir, TUN2SOCKS_BIN)
            )

            val executable = fileTun2Socks
                ?: throw IOException("Bin Tun2Socks não encontrado")

            val vpnDescriptor = mVpnInterfaceFileDescriptor
            if (vpnDescriptor != null) {
                val filePath = File(ContextCompat.getDataDir(mContext), "sock_path")
                try {
                    if (!filePath.exists()) {
                        filePath.createNewFile()
                    }
                } catch (_: IOException) {
                    throw IOException("Falha ao criar arquivo: " + filePath.canonicalPath)
                }

                command.append(executable.canonicalPath)
                command.append(" --netif-ipaddr $mVpnIpAddress")
                command.append(" --netif-netmask $mVpnNetMask")
                command.append(" --socks-server-addr $mSocksServerAddress")
                command.append(" --tunmtu $mVpnInterfaceMTU")
                command.append(" --tunfd ${vpnDescriptor.fd}")
                command.append(" --sock ${filePath.absolutePath}")
                command.append(" --loglevel 3")

                if (mUdpgwServerAddress != null) {
                    if (mUdpgwTransparentDNS) {
                        command.append(" --udpgw-transparent-dns")
                    }
                    command.append(" --udpgw-remote-server-addr $mUdpgwServerAddress")
                }

                if (mDnsResolverAddress != null) {
                    command.append(" --dnsgw $mDnsResolverAddress")
                }

                tun2SocksProcess = Runtime.getRuntime().exec(command.toString())
                val process = tun2SocksProcess!!
                val stdoutGobbler = StreamGobbler(process.inputStream, this)
                val stderrGobbler = StreamGobbler(process.errorStream, this)
                stdoutGobbler.start()
                stderrGobbler.start()

                if (!sendFd(vpnDescriptor, filePath)) {
                    throw IOException(
                        "Falha ao enviar Fd para sock, talvez isso não seja suportado em seu aparelho. Entre em contato com o desenvolvedor."
                    )
                }
                process.waitFor()
            }
        } catch (_: IOException) {
            // Original Tun2Socks error logging remains disabled.
        } catch (_: Exception) {
            // Original Tun2Socks error logging remains disabled.
        }

        tun2SocksProcess = null
        mListener?.onStop()
    }

    @Synchronized
    override fun interrupt() {
        super.interrupt()
        tun2SocksProcess?.destroy()
        try {
            fileTun2Socks?.let { VpnUtils.killProcess(it) }
        } catch (_: Exception) {
        }
        tun2SocksProcess = null
        fileTun2Socks = null
    }

    fun setOnTun2SocksListener(listener: OnTun2SocksListener) {
        mListener = listener
    }

    override fun onLine(log: String) {
        // Original Tun2Socks output logging remains disabled.
    }

    @Throws(InterruptedException::class)
    private fun sendFd(fileDescriptor: ParcelFileDescriptor, toFile: File): Boolean {
        for (tries in 10 downTo 0) {
            try {
                val localSocket = LocalSocket()
                localSocket.connect(
                    LocalSocketAddress(
                        toFile.absolutePath,
                        LocalSocketAddress.Namespace.FILESYSTEM
                    )
                )
                localSocket.setFileDescriptorsForSend(
                    arrayOf<FileDescriptor>(fileDescriptor.fileDescriptor)
                )
                localSocket.outputStream.write(42)
                localSocket.shutdownOutput()
                localSocket.close()
                return true
            } catch (_: IOException) {
                Thread.sleep(500)
            }
        }
        return false
    }

    companion object {
        private val TAG = Tun2Socks::class.java.simpleName
        private const val TUN2SOCKS_BIN = "libtun2socks"
    }
}
