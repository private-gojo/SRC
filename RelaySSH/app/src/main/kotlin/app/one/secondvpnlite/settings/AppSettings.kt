package app.one.secondvpnlite.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import app.one.secondvpnlite.MainActivity
import app.one.secondvpnlite.R
import app.one.secondvpnlite.SecondVPN

class AppSettings : AppCompatActivity(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var settingsToolbar: Toolbar
    private lateinit var enableUdp: SwitchCompat
    private lateinit var enableDns: SwitchCompat
    private lateinit var enableTethering: SwitchCompat
    private lateinit var enableWakeLock: SwitchCompat
    private lateinit var showLogs: SwitchCompat
    private lateinit var enableSshCompress: SwitchCompat
    private lateinit var disableTcpDelay: SwitchCompat
    private lateinit var typeUdp: EditText
    private lateinit var typeDns1: EditText
    private lateinit var typeDns2: EditText
    private lateinit var tlsVersion: TextView
    private lateinit var filterApps: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appsettings)

        settingsToolbar = findViewById(R.id.settings_toolbar)
        setSupportActionBar(settingsToolbar)
        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE)

        enableUdp = findViewById(R.id.enable_udp)
        enableDns = findViewById(R.id.enable_dns)
        enableTethering = findViewById(R.id.enable_thetering)
        enableWakeLock = findViewById(R.id.enable_wakelock)
        showLogs = findViewById(R.id.show_logs)
        enableSshCompress = findViewById(R.id.enable_sshcompress)
        disableTcpDelay = findViewById(R.id.disable_tcp_delay)

        tlsVersion = findViewById(R.id.set_tls_version)
        filterApps = findViewById(R.id.filter_apps_configure)
        typeUdp = findViewById(R.id.input_udp)
        typeDns1 = findViewById(R.id.input_dns_1)
        typeDns2 = findViewById(R.id.input_dns_2)

        if (SecondVPN.isEnableCustomUDP()) {
            enableUdp.isChecked = true
            typeUdp.visibility = View.VISIBLE
            typeUdp.setText(SecondVPN.getUDPResolver())
        } else {
            enableUdp.isChecked = false
            typeUdp.visibility = View.GONE
        }

        if (SecondVPN.isEnableCustomDNS()) {
            enableDns.isChecked = true
            typeDns1.visibility = View.VISIBLE
            typeDns2.visibility = View.VISIBLE
            typeDns1.setText(SecondVPN.customDNS1())
            typeDns2.setText(SecondVPN.customDNS2())
        } else {
            enableDns.isChecked = false
            typeDns1.visibility = View.GONE
            typeDns2.visibility = View.GONE
        }

        enableTethering.isChecked = SecondVPN.isEnableTethering()
        enableWakeLock.isChecked = SecondVPN.isEnableWakeLock()
        showLogs.isChecked = SecondVPN.isShowLogs()
        enableSshCompress.isChecked = SecondVPN.isEnableSSHCompress()
        disableTcpDelay.isChecked = SecondVPN.isEnableNoTCPDelay()

        enableUdp.setOnCheckedChangeListener(this)
        enableDns.setOnCheckedChangeListener(this)
        enableTethering.setOnCheckedChangeListener(this)
        enableWakeLock.setOnCheckedChangeListener(this)
        showLogs.setOnCheckedChangeListener(this)
        enableSshCompress.setOnCheckedChangeListener(this)
        disableTcpDelay.setOnCheckedChangeListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.set_tls_version -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.tls_version_title)
                val versions = arrayOf("Auto", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                builder.setItems(versions) { _, selected ->
                    when (selected) {
                        0 -> app_prefs!!.edit().putString("TLS_VERSION", "auto").apply()
                        1 -> app_prefs!!.edit().putString("TLS_VERSION", "TLSv1").apply()
                        2 -> app_prefs!!.edit().putString("TLS_VERSION", "TLSv1.1").apply()
                        3 -> app_prefs!!.edit().putString("TLS_VERSION", "TLSv1.2").apply()
                        4 -> app_prefs!!.edit().putString("TLS_VERSION", "TLSv1.3").apply()
                    }
                }
                builder.create().show()
            }

            R.id.filter_apps_configure -> {
                try {
                    startActivity(Intent(this, AllowedAppsActivity::class.java))
                } catch (error: Exception) {
                    Log.d("Settings", "Falhou ao iniciar allowed apps $error")
                    error.printStackTrace()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        savePreferences(true)
        MainActivity.updateUIAfterImport()
        val mainActivity = Intent(this, MainActivity::class.java)
        mainActivity.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(mainActivity)
        super.onBackPressed()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!enableUdp.isChecked) {
            enableDns.isChecked = true
        } else if (enableUdp.isChecked && typeUdp.text.toString().isEmpty()) {
            typeUdp.setText("127.0.0.1:7300")
        }

        if (enableDns.isChecked && typeDns1.text.toString().isEmpty()) {
            typeDns1.setText("1.1.1.1")
        }
        if (enableDns.isChecked && typeDns2.text.toString().isEmpty()) {
            typeDns2.setText("1.0.0.1")
        }
        savePreferences(false)
    }

    private fun savePreferences(isBackToHome: Boolean) {
        if (enableUdp.isChecked) {
            val udpText = typeUdp.text.toString()
            enableUdp.isChecked = true
            typeUdp.visibility = View.VISIBLE
            if (!isBackToHome) {
                if (udpText.isNotEmpty()) {
                    app_prefs!!.edit().putString("UDP_ADDR", typeUdp.text.toString()).apply()
                    typeUdp.setText(SecondVPN.getUDPResolver())
                }
            } else {
                if (udpText.isEmpty()) {
                    app_prefs!!.edit().putBoolean("IS_ENABLE_UDP", false).apply()
                } else {
                    app_prefs!!.edit().putBoolean("IS_ENABLE_UDP", true).apply()
                    app_prefs!!.edit().putString("UDP_ADDR", udpText).apply()
                }
            }
        } else {
            if (!isBackToHome) {
                enableUdp.isChecked = false
                typeUdp.visibility = View.GONE
            } else {
                app_prefs!!.edit().putBoolean("IS_ENABLE_UDP", false).apply()
            }
        }

        if (enableDns.isChecked) {
            val dns1Text = typeDns1.text.toString()
            val dns2Text = typeDns2.text.toString()
            enableDns.isChecked = true
            typeDns1.visibility = View.VISIBLE
            typeDns2.visibility = View.VISIBLE

            if (!isBackToHome) {
                if (dns1Text.isNotEmpty()) {
                    app_prefs!!.edit().putString("CUSTOM_DNS_1", dns1Text).apply()
                    typeDns1.setText(SecondVPN.customDNS1())
                } else if (dns2Text.isNotEmpty()) {
                    app_prefs!!.edit().putString("CUSTOM_DNS_2", dns2Text).apply()
                    typeDns2.setText(SecondVPN.customDNS2())
                }
            } else {
                if (dns1Text.isEmpty() && dns2Text.isEmpty()) {
                    app_prefs!!.edit().putBoolean("IS_ENABLE_DNS", false).apply()
                } else if (dns2Text.isEmpty()) {
                    app_prefs!!.edit().putBoolean("IS_ENABLE_DNS", false).apply()
                } else {
                    app_prefs!!.edit().putBoolean("IS_ENABLE_DNS", true).apply()
                    app_prefs!!.edit().putString("CUSTOM_DNS_1", dns1Text).apply()
                    app_prefs!!.edit().putString("CUSTOM_DNS_2", dns2Text).apply()
                }
            }
        } else {
            if (!isBackToHome) {
                enableDns.isChecked = false
                typeDns1.visibility = View.GONE
                typeDns2.visibility = View.GONE
            } else {
                app_prefs!!.edit().putBoolean("IS_ENABLE_DNS", false).apply()
            }
        }

        app_prefs!!.edit().putBoolean("IS_ENABLE_TETHERING", enableTethering.isChecked).apply()
        app_prefs!!.edit().putBoolean("IS_ENABLE_WAKELOCK", enableWakeLock.isChecked).apply()
        app_prefs!!.edit().putBoolean("IS_SHOW_LOGS", showLogs.isChecked).apply()
        app_prefs!!.edit().putBoolean("IS_ENABLE_SSH_COMPRESS", enableSshCompress.isChecked).apply()
        app_prefs!!.edit().putBoolean("IS_ENABLE_NO_TCP_DELAY", disableTcpDelay.isChecked).apply()
    }

    override fun onPause() {
        savePreferences(true)
        super.onPause()
    }

    override fun onDestroy() {
        savePreferences(true)
        super.onDestroy()
    }

    companion object {
        @JvmField
        var app_prefs: SharedPreferences? = null
    }
}
