package app.one.secondvpnlite.tunnel.vpn

import android.content.Context
import android.util.Log
import app.one.secondvpnlite.R
import app.one.secondvpnlite.tunnel.CustomNativeLoader
import app.one.secondvpnlite.tunnel.FileUtils
import app.one.secondvpnlite.tunnel.StreamGobbler
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class Pdnsd(
    private val mContext: Context,
    private val mDnsHosts: Array<String>,
    private val mDnsPort: Int,
    private val mPdnsdHost: String,
    private val mPdnsdPort: Int
) : Thread() {

    private var mListener: OnPdnsdListener? = null
    private var mProcess: Process? = null
    private var filePdnsd: File? = null

    interface OnPdnsdListener {
        fun onStart()
        fun onStop()
    }

    override fun run() {
        mListener?.onStart()
        try {
            filePdnsd = CustomNativeLoader.loadNativeBinary(
                mContext,
                PDNSD_BIN,
                File(mContext.filesDir, PDNSD_BIN)
            )

            val executable = filePdnsd
                ?: throw IOException("Bin Pdnsd não encontrada")

            val config = makePdnsdConf(
                mContext.filesDir,
                mDnsHosts,
                mDnsPort,
                mPdnsdHost,
                mPdnsdPort
            )

            val command = executable.canonicalPath + " -v9 -c " + config
            mProcess = Runtime.getRuntime().exec(command)

            val listener = object : StreamGobbler.OnLineListener {
                override fun onLine(line: String) {
                    // Original Pdnsd output logging remains disabled.
                }
            }

            val stdoutGobbler = StreamGobbler(mProcess!!.inputStream, listener)
            val stderrGobbler = StreamGobbler(mProcess!!.errorStream, listener)
            stdoutGobbler.start()
            stderrGobbler.start()
            mProcess!!.waitFor()
        } catch (_: IOException) {
            // Original Pdnsd error logging remains disabled.
        } catch (_: Exception) {
            // Original Pdnsd error logging remains disabled.
        }

        mProcess = null
        mListener?.onStop()
    }

    @Synchronized
    override fun interrupt() {
        super.interrupt()
        mProcess?.destroy()
        try {
            filePdnsd?.let { VpnUtils.killProcess(it) }
        } catch (_: Exception) {
        }
        mProcess = null
        filePdnsd = null
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun makePdnsdConf(
        fileDir: File,
        dnsHosts: Array<String>,
        dnsPort: Int,
        pdnsdHost: String,
        pdnsdPort: Int
    ): File {
        val content = FileUtils.readFromRaw(mContext, R.raw.pdnsd_local)

        val serverDns = StringBuilder()
        for (index in dnsHosts.indices) {
            val dnsHost = dnsHosts[index]
            serverDns.append(
                String.format(
                    PDNSD_SERVER,
                    "server" + (index + 1),
                    dnsHost,
                    dnsPort
                )
            )
        }

        val config = String.format(
            content,
            serverDns.toString(),
            fileDir.canonicalPath,
            pdnsdHost,
            pdnsdPort
        )
        Log.d(TAG, "pdnsd conf:" + config)

        val configFile = File(fileDir, "pdnsd.conf")
        if (configFile.exists()) {
            configFile.delete()
        }
        FileUtils.saveTextFile(configFile, config)

        val cacheFile = File(fileDir, "pdnsd.cache")
        if (!cacheFile.exists()) {
            try {
                cacheFile.createNewFile()
            } catch (_: Exception) {
            }
        }
        return configFile
    }

    fun setOnPdnsdListener(listener: OnPdnsdListener) {
        mListener = listener
    }

    companion object {
        private const val TAG = "PdnsdThread"
        private const val PDNSD_SERVER =
            "server {\n label= \"%1\$s\";\n ip = %2\$s;\n port = %3\$d;\n uptest = none;\n }\n"
        private const val PDNSD_BIN = "libpdnsd"
    }
}
