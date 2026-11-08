package app.one.secondvpnlite.service

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.ProxyInfo
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.system.OsConstants
import android.text.TextUtils
import android.util.Log
import com.trilead.ssh2.Connection
import com.trilead.ssh2.ConnectionMonitor
import com.trilead.ssh2.DebugLogger
import com.trilead.ssh2.DynamicPortForwarder
import com.trilead.ssh2.HTTPProxyData
import com.trilead.ssh2.InteractiveCallback
import com.trilead.ssh2.KnownHosts
import com.trilead.ssh2.ProxyData
import com.trilead.ssh2.ServerHostKeyVerifier
import app.one.secondvpnlite.MainActivity
import app.one.secondvpnlite.R
import app.one.secondvpnlite.SecondVPN
import app.one.secondvpnlite.injector.HTTPSupport3
import app.one.secondvpnlite.injector.HTTPThread
import app.one.secondvpnlite.injector.HttpProxyCustom
import app.one.secondvpnlite.injector.SSLProxy
import app.one.secondvpnlite.injector.SSLTunnelProxy
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.notification.NotificationService
import app.one.secondvpnlite.tethering.TetheringServer
import app.one.secondvpnlite.tunnel.IPUtil
import app.one.secondvpnlite.tunnel.IpAddress
import app.one.secondvpnlite.tunnel.TunnelUtils
import app.one.secondvpnlite.tunnel.vpn.CIDRIP
import app.one.secondvpnlite.tunnel.vpn.NetworkSpace
import app.one.secondvpnlite.tunnel.vpn.Pdnsd
import app.one.secondvpnlite.tunnel.vpn.Tun2Socks
import app.one.secondvpnlite.tunnel.vpn.VpnUtils
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.Collections
import java.util.Enumeration
import java.util.HashSet
import java.util.Locale
import java.util.Scanner
import java.util.concurrent.CountDownLatch

class TunnelManager : VpnService(), Runnable, ConnectionMonitor, InteractiveCallback,
    ServerHostKeyVerifier, DebugLogger {

    private val TAG = "TunnelManager"
    private val UTF_8: Charset = Charset.forName("UTF-8")
    private val ISO: Charset = Charset.forName("ISO-8859-1")
    private var mTunnelThreadStopSignal: CountDownLatch? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var mRunning = false
    private var mStopping = false
    private var mStarting = false
    private val listen_port = 9090
    private var coS: HTTPProxyData? = null
    private var mInjectThread: Thread? = null
    private var listen_socket: ServerSocket? = null
    private var input: Socket? = null
    var mReconnecting = false
    var fakeTunnelIsStarted = false
    private var connectivityManager: ConnectivityManager? = null
    private val mContext: Context =
        MainActivity.sContext ?: throw IllegalStateException("MainActivity context is not initialized")
    @JvmField
    var wakeLock: PowerManager.WakeLock? = MainActivity.wakeLock
    private var mTetheringThread: Thread? = null
    private var mTetheringServer: TetheringServer? = null

    private var mConnected = false
    private var servidor = ""
    private var servidor_domain = ""
    private var useProxy = false
    private var no_network_showed = false
    private var reconnectNotificationIsStarted = false
    private var privateIpAddress: String? = null
    private var prefixLength = 0
    private var pdnsdProcess: Process? = null
    private var mRouter: String? = null
    private var mMtu = 1500
    private var mPrivateAddress: VpnUtils.PrivateAddress? = null
    @JvmField
    var tunFd: ParcelFileDescriptor? = null
    private var mTun2Socks: Tun2Socks? = null
    private var mPdnsd: Pdnsd? = null
    private lateinit var mRoutes: NetworkSpace
    private lateinit var mRoutesv6: NetworkSpace
    private var isBypass = false
    private var tun2socksThread: Thread? = null
    private var tunFdDestroyLog = false
    private var transparentDns = !SecondVPN.isEnableCustomDNS()

    private var mConnection: Connection? = null
    private var sc1: HTTPThread? = null
    private var sc2: HTTPThread? = null

    private val newinjectTh = Runnable {
        try {
            listen_socket?.let {
                try {
                    it.close()
                } catch (error: IOException) {
                    throw RuntimeException(error)
                }
            }
            listen_socket = ServerSocket(listen_port)
            listen_socket!!.reuseAddress = true

            while (true) {
                input = listen_socket!!.accept()
                input!!.soTimeout = 0

                if (SecondVPN.getConnectionMode() == "MODO_HTTP" && !SecondVPN.isHTTPDirect()) {
                    output = HTTPSupport3(input!!).socket2()
                }

                if (SecondVPN.getConnectionMode() == "MODO_HTTPS" && SecondVPN.isPayloadAfterTLS()) {
                    output = SSLProxy(input!!).inject()
                }

                input?.keepAlive = true
                output?.keepAlive = true

                if (output == null) {
                    // Preserve the original null-output branch without dereferencing a null socket.
                } else if (output!!.isConnected) {
                    sc1 = HTTPThread(input!!, output!!, true)
                    sc2 = HTTPThread(output!!, input!!, false)
                    sc1!!.isDaemon = true
                    sc1!!.start()
                    sc2!!.isDaemon = true
                    sc2!!.start()
                }
            }
        } catch (_: Exception) {
            try {
                listen_socket?.close()
            } catch (error: IOException) {
                throw RuntimeException(error)
            }
        }
    }

    private fun setWakelock() {
        try {
            wakeLock!!.acquire()
        } catch (error: Exception) {
            Log.d("WAKELOCK", error.message ?: "")
        }
    }

    private fun unsetWakelock() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            try {
                wakeLock!!.release()
                AppLogManager.addLog(mContext.getString(R.string.wakelock_disabled))
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }

    fun stopMultiStatusInjectThread() {
        try {
            sc1?.interrupt()
            sc1 = null
            sc2?.interrupt()
            sc2 = null
            listen_socket?.close()
            listen_socket = null
            input?.close()
            input = null
            output?.close()
            output = null
            mInjectThread?.interrupt()
        } catch (_: Exception) {
        }
    }

    private fun isNetworkConnected(): Boolean {
        val manager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.activeNetworkInfo?.isConnected == true
    }

    private fun startMultiStatusInject() {
        try {
            mInjectThread?.interrupt()
            mInjectThread = Thread(newinjectTh, "mInjectThread")
            mInjectThread!!.start()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @Synchronized
    fun closeSSH() {
        stopForwarder()
        mConnection?.close()
    }

    @Synchronized
    fun closeSSH2() {
        if (!fakeTunnelIsStarted) {
            try {
                startFakeTunnelVpnService()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
        mConnection?.close()
        stopForwarderSocks()
    }

    private fun stopNetworkCallback() {
        if (networkCallback != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val manager =
                        mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    manager.unregisterNetworkCallback(networkCallback!!)
                }
            } catch (error: Exception) {
                AppLogManager.addLog(error.toString())
            }
        }
    }

    fun stopAll() {
        mStopping = true
        try {
            stopNetworkCallback()
        } catch (error: Exception) {
            throw RuntimeException(error)
        }

        if (!stoppingLog) {
            AppLogManager.addLog(mContext.getString(R.string.stopping))
            stoppingLog = true
        }

        SecondVPN.setCurrentVpnStatus("PARANDO")
        try {
            MainActivity.setButtonStatus("PARANDO")
        } catch (error: Exception) {
            error.printStackTrace()
        }

        if (SecondVPN.isEnableWakeLock()) {
            try {
                unsetWakelock()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }

        isToStopService = true
        Thread {
            mStopping = true
            mTunnelThreadStopSignal?.countDown()
            closeSSH()
            mConnection?.close()
            try {
                stopMultiStatusInjectThread()
            } catch (error: Exception) {
                error.printStackTrace()
            }
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
            }
            mRunning = false
            mStarting = false
            mReconnecting = false
        }.start()

        try {
            stopNotification()
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            closeSSH2()
            closeSSH()
            stopMultiStatusInjectThread()
            stopForwarderSocks()
            stopForwarder()
            if (SecondVPN.isEnableTethering()) StopTethering()
            if (SecondVPN.isEnableTetheringRoot()) stopTetheringRoot()

            if (!stoppedLog) {
                AppLogManager.addLog(
                    "<strong></font><font color=#FF0000>" +
                        mContext.getString(R.string.disconnected_log) + "</strong>"
                )
                stoppedLog = true
            }

            disconnectTunnel()
            SecondVPN.setCurrentVpnStatus("DESCONECTADO")
            try {
                MainActivity.setButtonStatus("DESCONECTADO")
            } catch (error: Exception) {
                error.printStackTrace()
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }

        stopSelf()
    }

    private fun stopNotification() {
        if (SecondVPN.isEnableNotification()) {
            val stopIntent = Intent(mContext, NotificationService::class.java)
            try {
                mContext.stopService(stopIntent)
            } catch (error: Exception) {
                NotificationService.stopNotification()
                error.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun resolveHostDomain(hostIP: String): String {
        return try {
            InetAddress.getByName(hostIP).hostAddress ?: hostIP
        } catch (_: Exception) {
            hostIP
        }
    }

    @Throws(Exception::class)
    fun startSSH() {
        if (SecondVPN.isEnableNotification()) {
            val notificationIntent = Intent(mContext, NotificationService::class.java)
            notificationIntent.putExtra("TITLE", mContext.getString(R.string.app_name))
            notificationIntent.putExtra("BODY", mContext.getString(R.string.starting_notification))
            notificationIntent.putExtra("IS_CONNECTED", false)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mContext.startForegroundService(notificationIntent)
                } else {
                    mContext.startService(notificationIntent)
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }

        if (!isToStopService) {
            try {
                startNetworkCallback()
            } catch (error: Exception) {
                AppLogManager.addLog(error.toString())
            }

            SecondVPN.setCurrentVpnStatus("INICIANDO")
            mStopping = false
            mRunning = true

            val ipSplit = SecondVPN.getServidorSSHDomain().split(":")
            val userAndPassSplit = SecondVPN.getUsuarioAndPass().split("@")
            val usuario = userAndPassSplit[0]
            val senha = userAndPassSplit[1]

            try {
                servidor = ipSplit[0]
                servidor_domain = ipSplit[0]
                currentIPAddr = resolveHostDomain(servidor)
            } catch (error: Exception) {
                error.printStackTrace()
            }

            if (servidor.isEmpty()) servidor = ipSplit[0]
            if (servidor.isEmpty()) servidor = ipSplit[0]
            val porta = ipSplit[1].toInt()
            val keyPath = ""
            val portaLocal = 1080

            try {
                conectar(servidor, porta)
                for (attempt in 0 until AUTH_TRIES) {
                    if (mStopping) return
                    try {
                        authentication(usuario, senha, keyPath)
                        break
                    } catch (error: IOException) {
                        if (attempt + 1 >= AUTH_TRIES) {
                            throw IOException("Autenticação falhou")
                        } else {
                            try {
                                Thread.sleep(3000)
                            } catch (_: InterruptedException) {
                                return
                            }
                        }
                    }
                }

                try {
                    MainActivity.setButtonStatus("CONECTADO")
                } catch (error: Exception) {
                    error.printStackTrace()
                }
                try {
                    MainActivity.ladi()
                } catch (error: Exception) {
                    error.printStackTrace()
                }

                AppLogManager.addLog(
                    "<strong></font><font color=#49C53C>" +
                        mContext.getString(R.string.connected) + "</strong>"
                )
                SecondVPN.setCurrentVpnStatus("CONECTADO")
                if (SecondVPN.isEnableWakeLock()) {
                    AppLogManager.addLog(mContext.getString(R.string.wakelock_enabled))
                }

                if (SecondVPN.isEnableNotification()) {
                    reconnectNotificationIsStarted = false
                    val connectedIntent = Intent(mContext, NotificationService::class.java)
                    connectedIntent.putExtra("TITLE", mContext.getString(R.string.app_name))
                    connectedIntent.putExtra("BODY", mContext.getString(R.string.connected_notification))
                    connectedIntent.putExtra("IS_CONNECTED", true)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mContext.startForegroundService(connectedIntent)
                        } else {
                            mContext.startService(connectedIntent)
                        }
                    } catch (error: Exception) {
                        error.printStackTrace()
                    }
                }

                if (SecondVPN.isEnableTetheringRoot()) {
                    AppLogManager.addLog(mContext.getString(R.string.tethering_root_enable))
                    AppLogManager.addLog(mContext.getString(R.string.tethering_root_not_avaliable))
                }

                startForwarder(portaLocal)
                Thread.sleep(3500)
            } catch (error: Exception) {
                mConnected = false
                throw error
            }
        } else {
            try {
                stopAll()
            } catch (error: Exception) {
                error.printStackTrace()
            }
            try {
                stopNotification()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }

    private fun startNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val manager =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val builder = NetworkRequest.Builder()
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (mConnected) {
                        connectionLost(Exception("A network error occurred (network lost)."))
                    }
                }

                override fun onLost(network: Network) {
                }
            }
            manager.registerNetworkCallback(builder.build(), networkCallback!!)
        }
    }

    private fun showCurrentDNS() {
        try {
            var dnsList = ""
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = connectivityManager!!.activeNetwork
                    val dnsServers =
                        connectivityManager!!.getLinkProperties(activeNetwork)!!.dnsServers
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        dnsList = dnsServers.joinToString(", ") { it.hostAddress ?: "" }
                    }
                }
            } catch (error: Exception) {
                dnsList = "?"
                error.printStackTrace()
            }
            AppLogManager.addLog("DNS: $dnsList")
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @Throws(Exception::class)
    protected fun startForwarder(portaLocal: Int) {
        if (!mConnected) throw Exception()
        startForwarderSocks(portaLocal)
        try {
            bypassToInject = false
            startTunnelVpnService()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @Synchronized
    @Throws(Exception::class)
    private fun startForwarderSocks(portaLocal: Int) {
        if (!mConnected) throw Exception()
        try {
            dpf = mConnection!!.createDynamicPortForwarder(portaLocal)
        } catch (error: Exception) {
            AppLogManager.addLog(error.toString())
        }
    }

    protected fun stopForwarder() {
        stopTunnelVpnService()
        stopForwarderSocks()
    }

    @Synchronized
    protected fun stopTunnelVpnService() {
        disconnectTunnel()
    }

    private fun StartTethering() {
        try {
            val wifiManager = mContext.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val apState = wifiManager.javaClass.getMethod("getWifiApState")
                .invoke(wifiManager) as Int
            if (apState == 13) {
                TetheringServer.isToStopTethering = false
                mTetheringServer = TetheringServer()
                mTetheringThread = Thread(mTetheringServer)
                mTetheringThread!!.start()
            } else {
                AppLogManager.addLog(
                    "<strong></font><font color=red>" +
                        mContext.getString(R.string.hostpot_isOFF) + " " + "</strong>"
                )
            }
        } catch (error: Exception) {
            AppLogManager.addLog(
                "<strong></font><font color=red>" +
                    mContext.getString(R.string.hostpot_error) + " " + "</strong>"
            )
            error.printStackTrace()
        }
    }

    private fun StopTethering() {
        try {
            TetheringServer.isToStopTethering = true
            mTetheringServer = TetheringServer()
            mTetheringThread = Thread(mTetheringServer)
            mTetheringThread!!.start()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private fun stopTetheringRoot() {
        AppLogManager.addLog(mContext.getString(R.string.tethering_root_stoppped))
    }

    private fun setDNS() {
        if (SecondVPN.isEnableCustomDNS()) {
            m_dnsResolvers = arrayOf(SecondVPN.customDNS1(), SecondVPN.customDNS2())
        } else {
            val dnsList = VpnUtils.getNetworkDnsServer(mContext)
            m_dnsResolvers = arrayOf(dnsList[0])
        }
    }

    protected fun startTunnelVpnService() {
        isBypass = false
        setDNS()
        startTunnelPrefs()
        establishVpn()
        fakeTunnelIsStarted = false
    }

    protected fun startFakeTunnelVpnService() {
        isBypass = true
        startTunnelPrefs()
        establishVpn()
        fakeTunnelIsStarted = true
    }

    private fun startTunnelPrefs() {
        mRoutes = NetworkSpace()
        mRoutesv6 = NetworkSpace()
        val ipAddress = IpAddress()
        privateIpAddress = ipAddress.getPrivateAddress()
        prefixLength = ipAddress.getPrefixLength()
        mRouter = ipAddress.getPrivateAddressRouter()
        try {
            mPrivateAddress = VpnUtils.selectPrivateAddress()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @Throws(Exception::class)
    protected fun conectar(servidor: String, porta: Int) {
        try {
            coS = HTTPProxyData("127.0.0.1", listen_port)
            mConnection = Connection(servidor, porta)

            if (SecondVPN.getConnectionMode() == "MODO_HTTP" && !SecondVPN.isHTTPDirect()) {
                coS?.let { mConnection!!.setProxyData(it) }
            }

            if (SecondVPN.getConnectionMode() == "MODO_HTTPS" && SecondVPN.isPayloadAfterTLS()) {
                coS?.let { mConnection!!.setProxyData(it) }
            }

            if (SecondVPN.isEnableNoTCPDelay()) mConnection!!.setTCPNoDelay(true)
            if (SecondVPN.isEnableSSHCompress()) {
                mConnection!!.setCompression(true)
                AppLogManager.addLog(mContext.getString(R.string.enabled_ssh_compress))
            }

            addProxy(mConnection!!)
            mConnection!!.addConnectionMonitor(this)

            if (Build.VERSION.SDK_INT >= 23) {
                val manager =
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val proxyInfo: ProxyInfo? = manager.defaultProxy
                if (proxyInfo != null) {
                    AppLogManager.addLog(
                        "<strong>Proxy:</strong> " +
                            String.format("%s:%d", proxyInfo.host, proxyInfo.port)
                    )
                }
            }

            AppLogManager.addLog(R.string.state_connecting)
            mConnection!!.connect(this, 10 * 1000, 20 * 1000)
            mConnected = true
        } catch (error: Exception) {
            val writer = StringWriter()
            error.printStackTrace(PrintWriter(writer))
            val cause = error.cause.toString()
            if (useProxy && cause.contains("Key exchange was not finished")) {
                AppLogManager.addLog(
                    "<strong></font><font color=#FF0000>Proxy: Key exchange was not finished</strong>"
                )
            } else if (!mStopping) {
                if (SecondVPN.getIsCustomFileIsLocked()) {
                    val causeRep = cause
                    AppLogManager.addLog(
                        "<strong></font><font color=#FF0000>SSH Error: </strong><strong>" +
                            causeRep.replace(servidor, "*").replace(servidor_domain, "*") +
                            "</strong>"
                    )
                } else {
                    AppLogManager.addLog(
                        "<strong></font><font color=#FF0000>SSH Error: </strong><strong>$cause</strong>"
                    )
                }

                if (!mReconnecting) {
                    try {
                        AppLogManager.addLog(
                            "<strong><font color=\"#ff8c00\">" +
                                mContext.getString(R.string.state_reconnecting) +
                                "...</strong>"
                        )
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                    }
                    reconnectSSH()
                }
            }
            throw Exception(error)
        }
    }

    @Throws(Exception::class)
    protected fun addProxy(conn: Connection) {
        useProxy = true
        val connectionMode = SecondVPN.getConnectionMode()
        if (connectionMode == "MODO_HTTP") {
            val ipSplit = SecondVPN.getServidorSSHDomain().split(":")
            val ip = ipSplit[0]
            val port = ipSplit[1].toInt()
            if (SecondVPN.isHTTPDirect()) {
                useProxy = false
                val customPayloadDirect = SecondVPN.getPayloadKey()
                try {
                    val proxyData: ProxyData = HttpProxyCustom(
                        ip, port, null, null, customPayloadDirect, true, mContext
                    )
                    conn.setProxyData(proxyData)
                } catch (_: Exception) {
                    throw Exception("Invalid Payload!")
                }
            } else {
                useProxy = true
            }
        } else if (connectionMode == "MODO_HTTPS") {
            val ipSplit = SecondVPN.getServidorSSHDomain().split(":")
            val ip = ipSplit[0]
            val port = ipSplit[1].toInt()
            Log.d(TAG, "Is HTTPS Proxy")
            if (SecondVPN.isPayloadAfterTLS()) {
                useProxy = false
            } else {
                useProxy = false
                val customSni = SecondVPN.getSNI()
                val tlsVersion = SecondVPN.getTLSVersion()
                try {
                    val proxyData: ProxyData =
                        SSLTunnelProxy(ip, port, customSni, tlsVersion, mContext)
                    conn.setProxyData(proxyData)
                } catch (error: Exception) {
                    AppLogManager.addLog(error.message ?: "")
                }
            }
        }
    }

    @Throws(IOException::class)
    protected fun authentication(usuario: String, senhaInput: String, keyPath: String) {
        if (!mConnected) throw IOException()
        var senha = senhaInput

        try {
            if (mConnection!!.isAuthMethodAvailable(usuario, AUTH_PASSWORD)) {
                AppLogManager.addLog(mContext.getString(R.string.validating_acess))
                if (mConnection!!.authenticateWithPassword(usuario, senha)) {
                    AppLogManager.addLog(mContext.getString(R.string.state_auth_success))
                }
            }
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Connection went away while we were trying to authenticate", error)
        } catch (error: Exception) {
            Log.e(TAG, "Problem during handleAuthentication()", error)
        }

        try {
            if (mConnection!!.isAuthMethodAvailable(usuario, AUTH_PUBLICKEY) &&
                keyPath.isNotEmpty()
            ) {
                val file = File(keyPath)
                if (file.exists()) {
                    if (senha.isEmpty()) senha = ""
                    AppLogManager.addLog(mContext.getString(R.string.autenticating_public_key))
                    if (mConnection!!.authenticateWithPublicKey(usuario, file, senha)) {
                        AppLogManager.addLog(
                            "<strong>" + mContext.getString(R.string.state_auth_success) + "</strong>"
                        )
                    }
                }
            }
        } catch (_: Exception) {
            Log.d(TAG, "Host does not support 'Public key' authentication.")
        }

        if (!mConnection!!.isAuthenticationComplete) {
            AppLogManager.addLog(
                "<strong></font><font color=#FF0000>" +
                    mContext.getString(R.string.login_error) + "</strong>"
            )
            throw IOException("Incorrect username or password")
        }
    }

    override fun log(level: Int, className: String, message: String) {
    }

    override fun verifyServerHostKey(
        hostname: String,
        port: Int,
        serverHostKeyAlgorithm: String,
        serverHostKey: ByteArray
    ): Boolean {
        val fingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey)
        AppLogManager.addLog(R.string.service_connect_server)
        AppLogManager.addLog("Fingerprint: $fingerprint")
        AppLogManager.addLog("Using algorithm: $serverHostKeyAlgorithm")
        return true
    }

    override fun connectionLost(reason: Throwable?) {
        if (mStarting || mReconnecting) return
        bypassToInject = true

        if (reason != null || !isToStopService) {
            if (SecondVPN.getIsCustomFileIsLocked()) {
                val reasonRep = reason!!.message!!
                if (!mStopping) {
                    AppLogManager.addLog(
                        "<strong></font><font color=#ff8c00>Connection Lost: </strong><strong>" +
                            reasonRep.replace(servidor, "*").replace(servidor_domain, "*") +
                            "</strong>"
                    )
                }
            } else if (!mStopping) {
                AppLogManager.addLog(
                    "<strong></font><font color=#ff8c00>Connection Lost: </strong><strong>" +
                        reason!!.message + "</strong>"
                )
            }

            if (!mStopping) {
                try {
                    AppLogManager.addLog(
                        "<strong><font color=\"#ff8c00\">" +
                            mContext.getString(R.string.state_reconnecting) + "...</strong>"
                    )
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                }
                reconnectSSH()
            }
        } else {
            stopAll()
        }
    }

    fun reconnectSSH() {
        if (!isToStopService) {
            bypassToInject = true
            if (mStarting || mStopping || mReconnecting) return
            mReconnecting = true

            if (!reconnectNotificationIsStarted && SecondVPN.isEnableNotification()) {
                reconnectNotificationIsStarted = true
                val notificationIntent = Intent(mContext, NotificationService::class.java)
                notificationIntent.putExtra("TITLE", mContext.getString(R.string.app_name))
                notificationIntent.putExtra("BODY", mContext.getString(R.string.state_nonetwork))
                notificationIntent.putExtra("IS_CONNECTED", false)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(notificationIntent)
                    } else {
                        mContext.startService(notificationIntent)
                    }
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }

            try {
                stopMultiStatusInjectThread()
                mInjectThread?.interrupt()
                if (SecondVPN.getConnectionMode() == "MODO_HTTP" && !SecondVPN.isHTTPDirect()) {
                    mInjectThread = Thread(newinjectTh, "mInjectThread")
                    mInjectThread!!.start()
                }
                if (SecondVPN.getConnectionMode() == "MODO_HTTPS" && SecondVPN.isPayloadAfterTLS()) {
                    mInjectThread = Thread(newinjectTh, "mInjectThread")
                    mInjectThread!!.start()
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }

            closeSSH2()
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
                mReconnecting = false
                return
            }

            for (attempt in 0 until RECONNECT_TRIES) {
                if (mStopping) {
                    mReconnecting = false
                    return
                }

                var sleepTime = 3
                if (!TunnelUtils.isNetworkOnline(mContext)) {
                    if (!no_network_showed) {
                        no_network_showed = true
                        AppLogManager.addLog(R.string.state_nonetwork)
                        if (!reconnectNotificationIsStarted && SecondVPN.isEnableNotification()) {
                            reconnectNotificationIsStarted = true
                            val notificationIntent =
                                Intent(mContext, NotificationService::class.java)
                            notificationIntent.putExtra("TITLE", mContext.getString(R.string.app_name))
                            notificationIntent.putExtra("BODY", mContext.getString(R.string.state_nonetwork))
                            notificationIntent.putExtra("IS_CONNECTED", false)
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    mContext.startForegroundService(notificationIntent)
                                } else {
                                    mContext.startService(notificationIntent)
                                }
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }
                    }
                } else {
                    no_network_showed = false
                    sleepTime = 3
                    mStarting = true
                    try {
                        if (!isToStopService) {
                            if (isRunning) {
                                try {
                                    startSSH()
                                    mStarting = false
                                    mReconnecting = false
                                } catch (error: Exception) {
                                    error.printStackTrace()
                                }
                            } else {
                                try {
                                    stopAll()
                                } catch (error: Exception) {
                                    error.printStackTrace()
                                }
                            }
                        } else {
                            try {
                                stopAll()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }
                        return
                    } catch (error: Exception) {
                        error.printStackTrace()
                    }
                    mStarting = false
                }

                try {
                    Thread.sleep(sleepTime * 1000L)
                    // Preserve the original retry behavior.
                    no_network_showed = no_network_showed
                } catch (_: InterruptedException) {
                    mReconnecting = false
                    return
                }
            }

            mReconnecting = false
            stopAll()
        } else {
            try {
                stopAll()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }

    override fun onReceiveInfo(infoId: Int, infoMsg: String) {
        if (infoId == ConnectionMonitor.SERVER_BANNER) {
            AppLogManager.addLog(
                "<strong><b>" + mContext.getString(R.string.server_message) +
                    "</b></strong> " + infoMsg
            )
        }
        bypassToInject = false
    }

    override fun replyToChallenge(
        name: String,
        instruction: String,
        numPrompts: Int,
        prompt: Array<String>,
        echo: BooleanArray
    ): Array<String> = emptyArray()

    override fun run() {
        tunFdDestroyLog = false
        stopAllThreads()

        try {
            Thread.sleep(50)
        } catch (error: Exception) {
            error.printStackTrace()
        }

        if (!isToStopService) {
            if (SecondVPN.isEnableNotification()) {
                val notificationIntent = Intent(mContext, NotificationService::class.java)
                notificationIntent.putExtra("TITLE", mContext.getString(R.string.app_name))
                notificationIntent.putExtra("BODY", mContext.getString(R.string.starting_notification))
                notificationIntent.putExtra("IS_CONNECTED", false)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(notificationIntent)
                    } else {
                        mContext.startService(notificationIntent)
                    }
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }

            isServiceRunning = true
            try {
                connectivityManager =
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            } catch (error: Exception) {
                error.printStackTrace()
            }

            try {
                bypassToInject = true
                startFakeTunnelVpnService()
            } catch (error: IOException) {
                error.printStackTrace()
            }

            mStarting = true
            mTunnelThreadStopSignal = CountDownLatch(1)
            var tries = 0
            while (!mStopping) {
                try {
                    if (!TunnelUtils.isNetworkOnline(mContext)) {
                        AppLogManager.addLog(R.string.state_nonetwork)
                        try {
                            Thread.sleep(2000)
                        } catch (_: InterruptedException) {
                            stopAll()
                            break
                        }
                    } else {
                        try {
                            MainActivity.setButtonStatus("INICIANDO")
                        } catch (error: Exception) {
                            error.printStackTrace()
                        }

                        try {
                            AppLogManager.addLog(
                                mContext.getString(R.string.current_network) + " " +
                                    TunnelUtils.getOperatorName(mContext) + " | " +
                                    TunnelUtils.getAPN(mContext)
                            )
                            val ipAddress = TunnelUtils.getLocalIpAddress()
                            val ipAddresses = ipAddress!!.split("\n")
                            for (ip in ipAddresses) {
                                AppLogManager.addLog(mContext.getString(R.string.local_ip) + " " + ip)
                            }
                        } catch (error: Exception) {
                            AppLogManager.addLog(error.toString())
                            error.printStackTrace()
                        }

                        AppLogManager.addLog(mContext.getString(R.string.starting_tunnel_service))
                        if (SecondVPN.isEnableWakeLock()) {
                            try {
                                setWakelock()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }

                        if (tries > 0) {
                            AppLogManager.addLog(
                                "<strong><font color=\"#ff8c00\">" +
                                    mContext.getString(R.string.state_reconnecting) + "...</strong>"
                            )
                        }

                        try {
                            Thread.sleep(100)
                        } catch (_: InterruptedException) {
                            stopAll()
                            break
                        }

                        if (SecondVPN.getConnectionMode() == "MODO_HTTP" && !SecondVPN.isHTTPDirect()) {
                            startMultiStatusInject()
                        }
                        if (SecondVPN.getConnectionMode() == "MODO_HTTPS" && SecondVPN.isPayloadAfterTLS()) {
                            startMultiStatusInject()
                        }

                        if (!isToStopService) {
                            startSSH()
                            mStarting = false
                            mReconnecting = false
                        } else {
                            try {
                                stopAll()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }
                        break
                    }
                } catch (_: Exception) {
                    closeSSH2()
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        stopAll()
                        break
                    }
                }
                tries++
            }

            mStarting = false
            if (!mStopping) {
                try {
                    mTunnelThreadStopSignal!!.await()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        while (!Thread.interrupted()) {
            if (isToStopService) {
                isServiceRunning = false
                stopAll()
                mStopping = true
                break
            }
        }
    }

    fun stopAllThreads() {
        try {
            mInjectThread?.let {
                it.interrupt()
                it.stop()
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            if (pdnsdProcess != null) {
                pdnsdProcess!!.destroy()
                mPdnsd?.interrupt()
                mPdnsd?.stop()
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            tun2socksThread?.interrupt()
            mTun2Socks?.interrupt()
            tun2socksThread?.stop()
            mTun2Socks?.stop()
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            tunFd?.close()
            tunFd?.detachFd()
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            output?.close()
            listen_socket?.close()
        } catch (error: Exception) {
            error.printStackTrace()
        }

        tunFd = null
        output = null
        listen_socket = null
        mTun2Socks = null
        mInjectThread = null

        try {
            Thread.sleep(500)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @Synchronized
    fun establishVpn(): Boolean {
        try {
            Locale.setDefault(Locale.ENGLISH)
            val builder = Builder()
            val privateAddress = mPrivateAddress!!
            builder.addAddress(privateAddress.mIpAddress, privateAddress.mPrefixLength)

            val release = Build.VERSION.RELEASE
            if (
                Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT &&
                !release.startsWith("4.4.3") &&
                !release.startsWith("4.4.4") &&
                !release.startsWith("4.4.5") &&
                !release.startsWith("4.4.6") &&
                mMtu < 1280
            ) {
                mMtu = 1280
            }

            builder.setMtu(mMtu)
            mRoutes.addIP(CIDRIP("0.0.0.0", 0), true)
            mRoutes.addIP(CIDRIP("10.0.0.0", 8), false)
            mRoutes.addIP(CIDRIP(privateAddress.mSubnet, privateAddress.mPrefixLength), true)

            if (!isBypass) {
                val hostString = currentIPAddr!!
                if (hostString.contains(":")) {
                    var ipv6: Inet6Address? = null
                    var ipv4: Inet4Address? = null
                    for (address in InetAddress.getAllByName(hostString)) {
                        if (address is Inet6Address) ipv6 = address
                        if (address is Inet4Address) ipv4 = address
                    }
                    if (ipv4 != null) {
                        mRoutes.addIP(CIDRIP(ipv4.hostAddress!!, 32), false)
                    }
                    if (ipv6 != null) {
                        val mask = if (hostString.contains("/")) GetIPV6Mask(hostString) else 128
                        mRoutesv6.addIPv6(ipv6!!, mask, true)
                    }
                } else {
                    mRoutes.addIP(CIDRIP(hostString, 32), false)
                }
            } else {
                mRoutes.addIP(CIDRIP("192.198.0.1", 32), false)
            }

            val allowUnsetAF = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            if (allowUnsetAF && !isBypass) {
                setAllowedVpnPackages(builder)
            }

            for (dns in m_dnsResolvers) {
                try {
                    if (dns.contains(":")) {
                        builder.addDnsServer("1.1.1.1")
                    } else {
                        builder.addDnsServer(dns)
                    }
                } catch (error: IllegalArgumentException) {
                    if (!isBypass) {
                        AppLogManager.addLog(
                            String.format(
                                "DNS error: <br> %s, %s",
                                dns,
                                error.localizedMessage
                            )
                        )
                    }
                }
            }

            // The original exclusion-route feature remains disabled by its false subroute flag.
            val multicastRange = NetworkSpace.IpAddress(CIDRIP("224.0.0.0", 3), true)
            for (route in mRoutes.getPositiveIPList()) {
                try {
                    if (!multicastRange.containsNet(route)) {
                        builder.addRoute(route.getIPv4Address(), route.networkMask)
                    }
                } catch (_: IllegalArgumentException) {
                }
            }

            for (route6 in mRoutesv6.getPositiveIPList()) {
                try {
                    builder.addRoute(route6.getIPv6Address(), route6.networkMask)
                } catch (_: IllegalArgumentException) {
                }
            }

            if (!isBypass) {
                try {
                    val checkV6 = TextUtils.join(", ", mRoutesv6.getNetworks(false))
                    if (SecondVPN.getIsCustomFileIsLocked()) {
                        addLog(
                            "Routes: " + TextUtils.join(", ", mRoutes.getNetworks(true))
                                .replaceFirst(currentIPAddr!!, "*")
                        )
                        addLog(
                            "Routes excluded (IPv4): " +
                                TextUtils.join(", ", mRoutes.getNetworks(false))
                                    .replaceFirst(currentIPAddr!!, "*")
                        )
                        if (checkV6 != "") {
                            addLog(
                                "Routes excluded (IPv6): " +
                                    TextUtils.join(", ", mRoutesv6.getNetworks(false))
                                        .replaceFirst(currentIPAddr!!, "*")
                            )
                        }
                    } else {
                        addLog("Routes: " + TextUtils.join(", ", mRoutes.getNetworks(true)))
                        addLog(
                            "Routes excluded (IPv4): " + TextUtils.join(", ", mRoutes.getNetworks(false))
                        )
                        if (checkV6 != "") {
                            addLog(
                                "Routes excluded (IPv6):" +
                                    TextUtils.join(", ", mRoutesv6.getNetworks(false))
                            )
                        }
                    }
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }

            if (!isBypass && SecondVPN.isEnableTethering()) {
                StartTethering()
            }

            if (isBypass) {
                builder.addDisallowedApplication(mContext.packageName)
            }

            tunFd = builder.setSession(getApplicationName()).establish()
            val socksServerAddress = String.format("127.0.0.1:%s", 1080)
            var udpResolver: String? =
                if (SecondVPN.isEnableCustomUDP()) SecondVPN.getUDPResolver() else null
            if (udpResolver != null &&
                !udpResolver.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}:\\d+$"))
            ) {
                udpResolver = null
            }

            connectTunnel(socksServerAddress, udpResolver)
            mRoutes.clear()
            return tunFd != null
        } catch (error: Exception) {
            AppLogManager.addLog("Failed to establish the VPN $error")
            return false
        }
    }

    @Synchronized
    fun connectTunnel(socksServerAddress: String?, udpResolver: String?) {
        if (socksServerAddress == null) {
            throw IllegalArgumentException("Must provide an IP address to a SOCKS server.")
        }
        if (tunFd == null) {
            throw IllegalStateException("Must establish the VPN before connecting the tunnel.")
        }
        if (tun2socksThread != null) {
            throw IllegalStateException("Tunnel already connected")
        }

        isRunning = true
        if (isBypass) return

        val pdnsdPort = VpnUtils.findAvailablePort(8091, 10)
        val dnsServers = m_dnsResolvers
        val dnsRelay = String.format("%s:%d", mPrivateAddress!!.mIpAddress, pdnsdPort)

        mPdnsd = Pdnsd(
            mContext,
            dnsServers,
            DNS_RESOLVER_PORT,
            mPrivateAddress!!.mIpAddress,
            pdnsdPort
        )
        val finalDnsRelay = dnsRelay
        mPdnsd!!.setOnPdnsdListener(object : Pdnsd.OnPdnsdListener {
            override fun onStart() {
                if (!isBypass) AppLogManager.addLog("DNS relay: $finalDnsRelay")
            }
            override fun onStop() {
            }
        })
        mPdnsd!!.start()

        mTun2Socks = Tun2Socks(
            mContext,
            tunFd,
            mMtu,
            mPrivateAddress!!.mRouter,
            VPN_INTERFACE_NETMASK,
            socksServerAddress,
            udpResolver,
            dnsRelay,
            transparentDns
        )
        mTun2Socks!!.setOnTun2SocksListener(object : Tun2Socks.OnTun2SocksListener {
            override fun onStart() {
                if (!isBypass) AppLogManager.addLog("Socks local: $socksServerAddress")
            }
            override fun onStop() {
            }
        })
        mTun2Socks!!.start()

        if (!isBypass) {
            try {
                Thread.sleep(1000)
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }

        if (!isBypass) {
            addLog("<b><font color=#49C53C>Tunnel VPN Conectado!</font></b>")
            showCurrentDNS()
        }
    }

    @Synchronized
    fun disconnectTunnel() {
        try {
            Thread.sleep(300)
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            if (!tunFdDestroyLog) {
                if (!vpnDestroyedLog) {
                    AppLogManager.addLog(
                        "<strong>" + mContext.getString(R.string.vpn_destroyed) + "</strong>"
                    )
                    vpnDestroyedLog = true
                }
                tunFdDestroyLog = true
            }
            tunFd!!.close()
            tunFd = null
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            if (mTun2Socks != null && mTun2Socks!!.isAlive) mTun2Socks!!.interrupt()
            mTun2Socks = null
            if (mPdnsd != null && mPdnsd!!.isAlive) mPdnsd!!.interrupt()
            mPdnsd = null
        } catch (error: Exception) {
            error.printStackTrace()
        }

        if (pdnsdProcess != null) {
            pdnsdProcess!!.destroy()
            pdnsdProcess = null
        }

        try {
            if (tun2socksThread != null) {
                tun2socksThread!!.join()
                tun2socksThread = null
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        isRunning = false
    }

    private fun allowAllAFFamilies(builder: Builder) {
        builder.allowFamily(OsConstants.AF_INET)
        builder.allowFamily(OsConstants.AF_INET6)
    }

    private fun setAllowedVpnPackages(builder: Builder) {
        val excludedApps = SecondVPN.app_prefs?.getStringSet("selectedApps", HashSet()) ?: emptySet()
        for (packageName in excludedApps) {
            try {
                if (packageName != mContext.packageName) {
                    builder.addDisallowedApplication(packageName)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                AppLogManager.addLog(
                    "<strong></font><font color=red>" +
                        mContext.getString(R.string.app_no_longer_exists) +
                        " </strong>$packageName"
                )
            }
        }
    }

    private fun getActiveNetworkDnsResolver(context: Context): List<String> {
        val dnsResolvers = getActiveNetworkDnsResolvers(context)
        if (dnsResolvers.isEmpty()) throw Exception("no active network DNS resolver")
        val result = ArrayList<String>()
        var max = 2
        for (address in dnsResolvers) {
            var value = address.toString()
            if (value.startsWith("/")) value = value.substring(1)
            if (value.contains(":")) continue
            result.add(value)
            max--
            if (max <= 0) break
        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getActiveNetworkDnsResolvers(context: Context): Collection<InetAddress> {
        val errorMessage = "getActiveNetworkDnsResolvers failed"
        val dnsAddresses = ArrayList<InetAddress>()
        try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val linkPropertiesClass = Class.forName("android.net.LinkProperties")
            val method = ConnectivityManager::class.java.getMethod(
                "getActiveLinkProperties",
                *emptyArray<Class<*>>()
            )
            val linkProperties = method.invoke(manager)
            if (linkProperties != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val dnsMethod = linkPropertiesClass.getMethod(
                        "getDnses",
                        *emptyArray<Class<*>>()
                    )
                    val dnses = dnsMethod.invoke(linkProperties) as Collection<*>
                    for (dns in dnses) dnsAddresses.add(dns as InetAddress)
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

    @Synchronized
    fun getLocalServerAddress(port: String): String =
        String.format(Locale.ROOT, "%s:%s", LOCAL_SERVER_ADDRESS, port)

    internal fun addLog(message: String) {
        if (!isBypass) AppLogManager.addLog(message)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getApplicationName(): String {
        val manager = mContext.packageManager
        val appInfo: ApplicationInfo = manager.getApplicationInfo(mContext.packageName, 0)
        return manager.getApplicationLabel(appInfo).toString()
    }

    private fun GetIPV6Mask(cidr: String): Int {
        if (cidr.contains("/")) {
            val index = cidr.indexOf("/")
            val networkPart = cidr.substring(index + 1)
            return networkPart.toInt()
        }
        throw IllegalArgumentException("not an valid CIDR format!")
    }

    companion object {
        private const val AUTH_PUBLICKEY = "publickey"
        private const val AUTH_PASSWORD = "password"
        private const val AUTH_KEYBOARDINTERACTIVE = "keyboard-interactive"
        private const val AUTH_TRIES = 5
        private const val RECONNECT_TRIES = 15
        private const val LOCAL_SERVER_ADDRESS = "127.0.0.1"
        private const val DNS = "DNSCUSTOM"
        private const val DNS_RESOLVER_PORT = 53
        private const val VPN_INTERFACE_NETMASK = "255.255.255.0"

        @JvmField
        var output: Socket? = null

        @JvmField
        var bypassToInject = false

        @JvmField
        var isToStopService = false

        @JvmField
        var stoppingLog = false

        @JvmField
        var stoppedLog = false

        @JvmField
        var vpnDestroyedLog = false

        @JvmField
        var isServiceRunning = false

        @JvmField
        var isRunning = false

        @JvmField
        var m_dnsResolvers: Array<String> = arrayOf("1.1.1.1", "1.0.0.1")

        @JvmField
        var currentIPAddr: String? = null

        private var dpf: DynamicPortForwarder? = null

        @JvmStatic
        @Synchronized
        fun stopForwarderSocks() {
            if (dpf != null) {
                try {
                    dpf!!.close()
                } catch (error: IOException) {
                    error.printStackTrace()
                }
                dpf = null
            }
        }

        @JvmStatic
        fun b(context: Context, resourceId: Int): String {
            val scanner = Scanner(
                context.resources.openRawResource(resourceId),
                "UTF-8"
            ).useDelimiter("\\A")
            val result = StringBuilder()
            while (scanner.hasNext()) {
                result.append(scanner.next())
            }
            scanner.close()
            return result.toString()
        }

        init {
            System.loadLibrary("tun2socks")
        }
    }
}
