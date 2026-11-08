package app.one.secondvpnlite.configs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import app.one.secondvpnlite.R
import app.one.secondvpnlite.SecondVPN
import app.one.secondvpnlite.security.AppSecurityManager
import app.one.secondvpnlite.service.TunnelManager
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class ImportConfig : AppCompatActivity() {
    private var isImportFromExternal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.empty)
        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE)

        val intent = intent
        val scheme = intent.scheme

        if (!TunnelManager.isServiceRunning) {
            if (scheme != null && (scheme == "file" || scheme == "content")) {
                isImportFromExternal = true
                val data = intent.data
                try {
                    ReadConfigFile(data!!)
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        R.string.error_file_config_incompatible,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                finish()
            } else {
                isImportFromExternal = false
                val uri = Uri.parse(
                    "file://" + Environment.getExternalStorageDirectory() + "/Download"
                )
                importConfigFileFromAPI(uri)
            }
        } else {
            Toast.makeText(this, R.string.only_disconnected, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun importConfigFileFromAPI(pickerInitialUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            startActivityForResult(intent, PICK_CONFIG_FILE)
        } catch (error: Exception) {
            Toast.makeText(this, R.string.error_file_config_incompatible, Toast.LENGTH_SHORT).show()
            finish()
            error.printStackTrace()
        }
    }

    fun getExtension(file: File): String {
        val filename = file.absolutePath
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1)
        }
        return ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONFIG_FILE) {
            try {
                val uri = data!!.data!!
                ReadConfigFile(uri)
            } catch (error: Exception) {
                finish()
                error.printStackTrace()
            }
        }
    }

    fun ReadConfigFile(uri: Uri) {
        var bufferedReader: BufferedReader
        var outputStream: FileOutputStream
        try {
            bufferedReader = BufferedReader(
                InputStreamReader(contentResolver.openInputStream(uri))
            )
            outputStream = openFileOutput("newFileName", Context.MODE_PRIVATE)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                outputStream.write(line!!.toByteArray())
                checkAndImportConfig(line!!)
            }
            bufferedReader.close()
            outputStream.close()
        } catch (error: IOException) {
            Toast.makeText(this, getString(R.string.import_file_error), Toast.LENGTH_SHORT).show()
            finish()
            error.printStackTrace()
        }
    }

    private fun checkAndImportConfig(config: String) {
        start_msg = getSharedPreferences(SecondVPN.FIRST_START, Context.MODE_PRIVATE)

        var finalConfig = ""
        try {
            finalConfig = AppSecurityManager.decryptStrAndFromBase64(i, k, config)
        } catch (error: Exception) {
            error.printStackTrace()
        }

        try {
            val jsonConfig = JSONObject(finalConfig)
            SecondVPN.setDefaultPrefs()
            Log.d("Import", jsonConfig.toString())

            if (jsonConfig.getInt("appVersion") >= 22) {
                app_prefs!!.edit().putLong(
                    "VALIDADE_CONFIG",
                    jsonConfig.getLong("ConfigValidityDate")
                ).apply()
                app_prefs!!.edit().putString(
                    "CONNECTION_MODE",
                    jsonConfig.getString("ConnectionMode")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_HTTP_DIRECT",
                    jsonConfig.getBoolean("isHTTPDirect")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "PAYLOAD_AFTER_TLS",
                    jsonConfig.getBoolean("isPayloadAfterTLS")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_CUSTOM_FILE_LOCKED",
                    jsonConfig.getBoolean("isLockConfig")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_CUSTOM_FILE_LOCK_SETTINGS",
                    jsonConfig.getBoolean("isLockAppSettings")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_ENABLE_UDP",
                    jsonConfig.getBoolean("isEnableUdp")
                ).apply()
                app_prefs!!.edit().putString(
                    "UDP_ADDR",
                    jsonConfig.getString("UDPAddr")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_ENABLE_DNS",
                    jsonConfig.getBoolean("isEnableDNS")
                ).apply()
                app_prefs!!.edit().putString(
                    "CUSTOM_DNS_1",
                    jsonConfig.getString("DNS1")
                ).apply()
                app_prefs!!.edit().putString(
                    "CUSTOM_DNS_2",
                    jsonConfig.getString("DNS2")
                ).apply()
                app_prefs!!.edit().putString(
                    "CONFIG_MSG",
                    jsonConfig.getString("ConfigMsg")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_CONFIG_ONLY_MOBILE_DATA",
                    jsonConfig.getBoolean("onlyMobileData")
                ).apply()
                app_prefs!!.edit().putString(
                    "TLS_VERSION",
                    jsonConfig.getString("TLSVersion")
                ).apply()
                app_prefs!!.edit().putString(
                    "PAYLOAD_KEY",
                    jsonConfig.getString("CurrentPayload")
                ).apply()
                app_prefs!!.edit().putString(
                    "SSH_SERVER_DOMAIN",
                    jsonConfig.getString("ServerSSH")
                ).apply()
                app_prefs!!.edit().putString(
                    "PROXY_IP_DOMAIN",
                    jsonConfig.getString("ServerProxy")
                ).apply()
                app_prefs!!.edit().putString(
                    "CUSTOM_SNI",
                    jsonConfig.getString("ServerSNI")
                ).apply()
                app_prefs!!.edit().putBoolean(
                    "IS_LOCK_LOGIN_EDIT",
                    jsonConfig.getBoolean("LockEditLogin")
                ).apply()
                app_prefs!!.edit().putString(
                    "SSH_AUTH_DATA",
                    jsonConfig.getString("ConfigAuthData")
                ).apply()

                showImportResult(R.string.import_sucess)
            } else {
                showImportResult(R.string.file_config_incompatible)
            }
        } catch (error: Exception) {
            error.printStackTrace()
            Toast.makeText(
                this,
                getString(R.string.file_config_incompatible),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun showImportResult(message: Int) {
        Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
        val activityIntent = Intent(this, dev.relay.ssh.RelayMainActivity::class.java)
        activityIntent.putExtra("IS_IMPORT", true)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(activityIntent)
        finish()
    }

    companion object {
        private const val PICK_CONFIG_FILE = 2

        @JvmField
        var app_prefs: SharedPreferences? = null

        @JvmField
        var start_msg: SharedPreferences? = null

        @JvmField
        val k: String = AppSecurityManager.k1 + AppSecurityManager.k2 +
            AppSecurityManager.k3 + AppSecurityManager.k4 + AppSecurityManager.k5

        @JvmField
        val i: String = AppSecurityManager.iv1 + AppSecurityManager.iv2 +
            AppSecurityManager.ivf
    }
}
