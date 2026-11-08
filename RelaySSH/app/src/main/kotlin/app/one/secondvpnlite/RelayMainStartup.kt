package app.one.secondvpnlite

import android.app.AlertDialog
import android.content.Context
import android.net.wifi.WifiManager
import android.widget.LinearLayout
import app.one.secondvpnlite.drawer.DrawerLog
import app.one.secondvpnlite.service.TunnelManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Kotlin-owned MainActivity startup and initial view wiring.
 * MainActivity retains its lifecycle entry point and Java-compatible callbacks.
 */
object RelayMainStartup {
    @JvmStatic
    fun start(activity: MainActivity) {
        // Preserve the original startup order.
        activity.mDrawer = DrawerLog(activity)
        activity.setContentView(R.layout.activity_main_drawer)
        activity.app_toolbar = activity.findViewById(R.id.app_toolbar)
        activity.setSupportActionBar(activity.app_toolbar)

        // Keep the original toolbar focus behavior.
        activity.app_toolbar.requestFocus()

        MainActivity.sActivity = activity
        MainActivity.sContext = activity

        MobileAds.initialize(
            activity,
            object : OnInitializationCompleteListener {
                override fun onInitializationComplete(
                    initializationStatus: InitializationStatus
                ) {
                }
            }
        )

        val testDeviceIds = listOf(
            "74961B0CF01927ADD5D88020D3CC1F9C",
            "6F24778EBA50C09BF8ABE74236319D29"
        )
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)

        MainActivity.mAdView = activity.findViewById(R.id.adView)

        activity.mDrawer.setDrawer(activity)

        MainActivity.app_prefs = activity.getSharedPreferences(
            SecondVPN.PREFS_GERAL,
            Context.MODE_PRIVATE
        )
        MainActivity.start_msg = activity.getSharedPreferences(
            SecondVPN.FIRST_START,
            Context.MODE_PRIVATE
        )

        val showFirstTime = MainActivity.start_msg.getBoolean("default_config", true)
        val intent = activity.intent
        if (showFirstTime && intent != null && !intent.getBooleanExtra("IS_IMPORT", false)) {
            SecondVPN.setDefaultPrefs()
            MainActivity.start_msg.edit().putBoolean("default_config", false).apply()
            activity.showStartMsg()
        } else if (showFirstTime && intent != null && intent.getBooleanExtra("IS_IMPORT", false)) {
            MainActivity.start_msg.edit().putBoolean("default_config", false).apply()
            activity.showStartMsg()
        }

        MainActivity.servidor_ssh_layout = activity.findViewById(
            R.id.activity_mainInputServerSSHIPORTLayout
        )
        MainActivity.servidor_proxy_layout = activity.findViewById(
            R.id.activity_mainInputServerPROXYIPORTLayout
        )
        MainActivity.custom_sni_layout = activity.findViewById(
            R.id.activity_mainInputSNILayout
        )
        MainActivity.payload_layout = activity.findViewById(
            R.id.activity_mainInputPayloadLinearLayout
        )
        MainActivity.user_and_pass_layout = activity.findViewById(
            R.id.activity_mainInputUserandPassLayout
        )
        MainActivity.start_button = activity.findViewById(R.id.activity_StartConnection)

        val logs: FloatingActionButton = activity.findViewById(R.id.logs)
        logs.setOnClickListener(activity)

        if (TunnelManager.isServiceRunning) {
            MainActivity.disableInterfaceOnConnect()
            MainActivity.start_button.setText(R.string.stop)
        } else {
            MainActivity.enableInterfaceOnConnect()
            MainActivity.start_button.setText(R.string.start)
        }

        MainActivity.user_and_pass = activity.findViewById(R.id.activity_mainInputUserandPass)
        MainActivity.servidor_ssh = activity.findViewById(R.id.activity_mainInputServerSSHIPORT)
        MainActivity.servidor_proxy = activity.findViewById(R.id.activity_mainInputServerPROXYIPORT)
        MainActivity.custom_sni = activity.findViewById(R.id.activity_mainInputSNI)
        MainActivity.custom_payload = activity.findViewById(R.id.activity_mainInputPayloadEditText)
        MainActivity.modo_http = activity.findViewById(R.id.modo_http)
        MainActivity.modo_https = activity.findViewById(R.id.modo_https)
        MainActivity.payload_after_tls = activity.findViewById(R.id.activity_mainPayloadAfterTLS)
        MainActivity.direct_mode = activity.findViewById(R.id.activity_mainDirectMode)
        MainActivity.config_msg = activity.findViewById(R.id.config_msg_textview)

        RelayMainAds.setup(activity, MainActivity.mAdView)

        RelayMainUi.updateLayoutWithConfig1()
        RelayMainUi.updateLayoutWithConfig2()
        MainActivity.start_button.setOnClickListener(activity)
        MainActivity.modo_https.setOnCheckedChangeListener(activity)
        MainActivity.modo_http.setOnCheckedChangeListener(activity)
        MainActivity.payload_after_tls.setOnCheckedChangeListener(activity)
        MainActivity.direct_mode.setOnCheckedChangeListener(activity)

        RelayMainUi.updateLayoutWithConfig3()
        if (TunnelManager.isServiceRunning) {
            MainActivity.disableInterfaceOnConnect()
            MainActivity.start_button.setText(R.string.stop)
        } else {
            MainActivity.enableInterfaceOnConnect()
            MainActivity.start_button.setText(R.string.start)
        }

        val wifiManager = activity.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            if (intent != null && !intent.getBooleanExtra("IS_IMPORT", false)) {
                MainActivity.ladi()
            }
        } else if (TunnelManager.isServiceRunning) {
            if (intent != null && !intent.getBooleanExtra("IS_IMPORT", true)) {
                MainActivity.ladi()
            }
        }
    }

    @JvmStatic
    fun showStartMessage(activity: MainActivity) {
        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogTheme)
            .setTitle(activity.getString(R.string.start_msg_title))
            .setCancelable(true)
            .setMessage(activity.getString(R.string.start_msg_text))
            .setPositiveButton("OK") { dialog1, _ ->
                MainActivity.app_prefs.edit().putBoolean("default_config", false).apply()
                dialog1.dismiss()
                activity.getPermissionAndroid13()
            }
        dialog.show()
    }
}
