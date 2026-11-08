package app.one.secondvpnlite

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import app.one.secondvpnlite.util.SharedPref

class SecondVPN : Application() {
    override fun onCreate() {
        super.onCreate()
        app_prefs = SharedPref.getInstance(this)

        MobileAds.initialize(
            this,
            object : OnInitializationCompleteListener {
                override fun onInitializationComplete(status: InitializationStatus) {
                }
            }
        )
        appOpenManager = AppOpenManager(this)
    }

    companion object {
        private val TAG = SecondVPN::class.java.simpleName

        const val PREFS_GERAL = "SECONDVPN_PREFS"
        const val FIRST_START = "FIRST_START_PREFS"

        @JvmField
        var app_prefs: SharedPref? = null

        @JvmField
        var appOpenManager: AppOpenManager? = null

        private fun prefs(): SharedPref = app_prefs!!

        @JvmStatic fun isEnableCustomDNS(): Boolean = prefs().getBoolean("IS_ENABLE_DNS", true)
        @JvmStatic fun data_validade_file(): Long = prefs().getLong("VALIDADE_CONFIG", 0)
        @JvmStatic fun isEnableTethering(): Boolean = prefs().getBoolean("IS_ENABLE_TETHERING", false)
        @JvmStatic fun isEnableTetheringRoot(): Boolean = prefs().getBoolean("IS_ENABLE_TETHERING_ROOT", false)
        @JvmStatic fun isEnableCustomUDP(): Boolean = prefs().getBoolean("IS_ENABLE_UDP", true)
        @JvmStatic fun isEnableNotification(): Boolean = prefs().getBoolean("IS_ENABLE_NOTIFICATION", true)
        @JvmStatic fun isEnableNoTCPDelay(): Boolean = prefs().getBoolean("IS_ENABLE_NO_TCP_DELAY", true)
        @JvmStatic fun isEnableSSHCompress(): Boolean = prefs().getBoolean("IS_ENABLE_SSH_COMPRESS", true)
        @JvmStatic fun getIsCustomFileIsLocked(): Boolean = prefs().getBoolean("IS_CUSTOM_FILE_LOCKED", false)
        @JvmStatic fun getIsCustomFileLockSettings(): Boolean = prefs().getBoolean("IS_CUSTOM_FILE_LOCK_SETTINGS", false)
        @JvmStatic fun isPayloadAfterTLS(): Boolean = prefs().getBoolean("PAYLOAD_AFTER_TLS", false)
        @JvmStatic fun isHTTPDirect(): Boolean = prefs().getBoolean("IS_HTTP_DIRECT", false)
        @JvmStatic fun isLockLoginEdit(): Boolean = prefs().getBoolean("IS_LOCK_LOGIN_EDIT", false)
        @JvmStatic fun isEnableWakeLock(): Boolean = prefs().getBoolean("IS_ENABLE_WAKELOCK", true)
        @JvmStatic fun isShowLogs(): Boolean = prefs().getBoolean("IS_SHOW_LOGS", true)
        @JvmStatic fun isIsCustomConfigOnlyMobileData(): Boolean =
            prefs().getBoolean("IS_CONFIG_ONLY_MOBILE_DATA", false)

        @JvmStatic fun getUDPResolver(): String? = prefs().getString("UDP_ADDR", "127.0.0.1:7300")
        @JvmStatic fun customDNS1(): String? = prefs().getString("CUSTOM_DNS_1", "1.1.1.1")
        @JvmStatic fun customDNS2(): String? = prefs().getString("CUSTOM_DNS_2", "1.0.0.1")
        @JvmStatic fun getServidorSSHDomain(): String? = prefs().getString("SSH_SERVER_DOMAIN", "")
        @JvmStatic fun getSNI(): String? = prefs().getString("CUSTOM_SNI", "")
        @JvmStatic fun getProxyIPDomain(): String? = prefs().getString("PROXY_IP_DOMAIN", "")
        @JvmStatic fun getTLSVersion(): String? = prefs().getString("TLS_VERSION", "auto")
        @JvmStatic fun getConnectionMode(): String? = prefs().getString("CONNECTION_MODE", "")
        @JvmStatic fun getPayloadKey(): String? = prefs().getString("PAYLOAD_KEY", "")
        @JvmStatic fun getUsuarioAndPass(): String? = prefs().getString("SSH_AUTH_DATA", "")
        @JvmStatic fun getConfigMsgText(): String? = prefs().getString("CONFIG_MSG", "")
        @JvmStatic fun getCurrentVpnStatus(): String? =
            prefs().getString("LAST_VPN_STATUS", "DESCONECTADO")

        @JvmStatic
        fun setCurrentVpnStatus(status: String) {
            prefs().putString("LAST_VPN_STATUS", status)
        }

        @JvmStatic
        fun getLastA(): Boolean = prefs().getBoolean("LAST_A", false)

        @JvmStatic
        fun setDefaultPrefs() {
            prefs().clear()
            prefs().putString("CONNECTION_MODE", "MODO_HTTP")
            prefs().putBoolean("IS_HTTP_DIRECT", true)
            prefs().putBoolean("LAST_A", false)
            prefs().putBoolean("PAYLOAD_AFTER_TLS", false)
            prefs().putBoolean("IS_CUSTOM_FILE_LOCKED", false)
            prefs().putString("LAST_VPN_STATUS", "DESCONECTADO")
        }
    }
}
