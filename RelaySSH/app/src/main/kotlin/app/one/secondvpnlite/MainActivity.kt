package app.one.secondvpnlite

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import app.one.secondvpnlite.drawer.DrawerLog
import app.one.secondvpnlite.service.TunnelManager
import app.one.secondvpnlite.tunnel.TunnelUtils

open class MainActivity : AppCompatActivity(), DrawerLayout.DrawerListener,
    View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private var mTunnelThread: Thread? = null
    private var mTunnelManager: TunnelManager? = null
    private var configIsOk = false
    private var exportWithoutLogin = false
    private var intentToImport = false
    var mDrawer: DrawerLog? = null
    lateinit var app_toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startApp()
    }

    private fun startApp() {
        RelayMainStartup.start(this)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
        super.onBackPressed()
    }

    private var duringisLoad = false

    fun getPermissionAndroid13() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun stopVPNService() {
        RelayMainVpnFlow.stopTunnel { startTunnelManagerFromFlow() }
    }

    private fun startVPNService() {
        RelayMainVpnFlow.requestPermission(this) {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null)
        }
    }

    private fun checkConfig(istoSave: Boolean): Boolean {
        val result = RelayMainValidation.validate(this, istoSave)
        configIsOk = result.configIsOk
        exportWithoutLogin = result.exportWithoutLogin
        return configIsOk
    }

    @JvmOverloads
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        RelayMainVpnFlow.handlePermissionResult(
            this,
            requestCode,
            resultCode,
            { checkConfig(false) },
            { showLog() },
            { startTunnelManagerFromFlow() }
        )
    }

    fun startTunnelManagerFromFlow() {
        mTunnelManager = TunnelManager()
        mTunnelThread = Thread(mTunnelManager)
        mTunnelThread!!.start()
    }

    private fun showLog() {
        if (mDrawer != null && !isFinishing) {
            val drawerLayout = mDrawer!!.getDrawerLayout()
            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        RelayMainEvents.onCheckedChanged(this, intentToImport) { savePreferences() }
    }

    override fun onPause() {
        savePreferences()
        super.onPause()
    }

    override fun onStop() {
        if (!duringisLoad) {
        }
        super.onStop()
    }

    override fun onResume() {
        if (!intentToImport) {
            savePreferences()
        } else {
            intentToImport = false
        }

        if (TunnelManager.isServiceRunning) {
            if (mInterstitialAd == null) {
                setup_ad()
            }
        }
        super.onResume()
    }

    override fun onDestroy() {
        savePreferences()
        mAdView!!.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        RelayMainActions.handleOptionsMenu(this, item)
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View) {
        RelayMainActions.onClick(this, view)
    }

    fun setStartOrStopVPNFromActions() {
        setStartOrStopVPN(this)
    }

    fun showLogFromActions() {
        showLog()
    }

    fun setIntentToImportFromActions() {
        intentToImport = true
    }

    fun checkConfigFromActions(istoSave: Boolean): Boolean = checkConfig(istoSave)

    fun isExportWithoutLoginFromActions(): Boolean = exportWithoutLogin

    fun clearTextviewsFromActions() {
        clear_textviews()
    }

    fun savePreferencesFromActions() {
        savePreferences()
    }

    private fun setStartOrStopVPN(mainActivity: MainActivity) {
        RelayMainVpnFlow.setStartOrStop(
            this,
            { savePreferences() },
            { startVPNService() },
            { stopVPNService() }
        )
    }

    private fun savePreferences() {
        RelayMainPreferences.save()
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    override fun onDrawerClosed(drawerView: View) {
        RelayMainActions.onDrawerClosed(this, drawerView)
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    private fun clear_textviews() {
        try {
            servidor_ssh!!.text = ""
            custom_payload!!.text = ""
            user_and_pass!!.text = ""
            servidor_proxy!!.text = ""
            direct_mode!!.text = ""
            custom_sni!!.text = ""
            user_and_pass!!.text = ""
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    fun showStartMsg() {
        RelayMainStartup.showStartMessage(this)
    }

    companion object {
        @JvmField
        var app_prefs: SharedPreferences? = null

        @JvmField
        var start_msg: SharedPreferences? = null

        const val START_VPN_PROFILE = 70
        private const val TAG = "MainActivity"

        @JvmField
        val UIHandler: Handler = Handler(Looper.getMainLooper())

        @JvmField
        var start_button: Button? = null

        @JvmField
        var servidor_ssh: TextView? = null

        @JvmField
        var servidor_proxy: TextView? = null

        @JvmField
        var custom_sni: TextView? = null

        @JvmField
        var custom_payload: TextView? = null

        @JvmField
        var user_and_pass: TextView? = null

        @JvmField
        var modo_http: AppCompatRadioButton? = null

        @JvmField
        var modo_https: AppCompatRadioButton? = null

        @JvmField
        var payload_after_tls: SwitchCompat? = null

        @JvmField
        var direct_mode: SwitchCompat? = null

        @JvmField
        var config_msg: TextView? = null

        @JvmField
        var servidor_ssh_layout: LinearLayout? = null

        @JvmField
        var servidor_proxy_layout: LinearLayout? = null

        @JvmField
        var custom_sni_layout: LinearLayout? = null

        @JvmField
        var payload_layout: LinearLayout? = null

        @JvmField
        var user_and_pass_layout: LinearLayout? = null

        @JvmField
        var mAdView: AdView? = null

        @JvmField
        var mInterstitialAd: InterstitialAd? = null

        @JvmField
        var sContext: Context? = null

        @JvmField
        var sActivity: Activity? = null

        @JvmField
        var wakeLock: PowerManager.WakeLock? = null

        var delay = 4000
        var isShowing = false

        @JvmStatic
        fun runOnUI(runnable: Runnable) {
            RelayMainUi.runOnUI(runnable)
        }

        @JvmStatic
        fun setButtonStatus(status: String) {
            RelayMainUi.setButtonStatus(status)
        }

        @JvmStatic
        fun updateUIAfterImport() {
            RelayMainUi.updateUIAfterImport()
        }

        @JvmStatic
        fun checkValidade(validadeDateMillis: Long): Boolean =
            RelayMainValidation.isExpired(validadeDateMillis)

        @JvmStatic
        fun updateLayoutWithConfig2() {
            RelayMainUi.updateLayoutWithConfig2()
        }

        @JvmStatic
        fun disableInterfaceOnConnect() {
            RelayMainUi.disableInterfaceOnConnect()
        }

        @JvmStatic
        fun enableInterfaceOnConnect() {
            RelayMainUi.enableInterfaceOnConnect()
        }

        @JvmStatic
        fun ladi() {
            RelayMainAds.loadInterstitialIfNeeded(sActivity!!, sContext!!, app_prefs!!, mAdView!!)
        }

        private fun setup_ad() {
            RelayMainAds.setup(sContext!!, mAdView!!)
        }
    }
}
