package app.one.secondvpnlite.configs

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import org.json.JSONObject
import app.one.secondvpnlite.R
import app.one.secondvpnlite.SecondVPN
import app.one.secondvpnlite.security.AppSecurityManager
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.util.Calendar

class ExportConfig : AppCompatActivity(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var exportToolbar: Toolbar
    private lateinit var exportConfigButton: Button
    private lateinit var configName: EditText
    private lateinit var configMsg: EditText
    private lateinit var lockConfigFromEdit: SwitchCompat
    private lateinit var lockSettingsApp: SwitchCompat
    private lateinit var onlyMobileData: SwitchCompat
    private var validateDate: SwitchCompat? = null
    private lateinit var lockLogin: SwitchCompat
    private var mValidade: Long = 0
    private var configToExportOnPermisson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.export_config)

        exportToolbar = findViewById(R.id.export_toolbar)
        setSupportActionBar(exportToolbar)
        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE)

        configName = findViewById(R.id.config_file_name)
        configMsg = findViewById(R.id.config_msg)
        lockConfigFromEdit = findViewById(R.id.lock_config)
        lockSettingsApp = findViewById(R.id.lock_config_settings)
        onlyMobileData = findViewById(R.id.lock_only_mobile_data)
        validateDate = findViewById(R.id.lock_config_validate)
        lockLogin = findViewById(R.id.lock_login_edit)
        exportConfigButton = findViewById(R.id.export_config_button)

        lockConfigFromEdit.setOnCheckedChangeListener(this)
        lockSettingsApp.setOnCheckedChangeListener(this)
        onlyMobileData.setOnCheckedChangeListener(this)
        validateDate?.setOnCheckedChangeListener(this)
        exportConfigButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.export_config_button -> {
                if (configName.text.toString().isEmpty()) {
                    Toast.makeText(this, getString(R.string.filename_empty), Toast.LENGTH_SHORT).show()
                } else if (lockLogin.isChecked && SecondVPN.getUsuarioAndPass().isEmpty()) {
                    Toast.makeText(this, getString(R.string.empty_login), Toast.LENGTH_SHORT).show()
                } else {
                    if (validateDate?.isChecked == true) {
                        validateDate = null
                    } else {
                        mValidade = 0
                    }

                    val configString = JSONObject()
                    val key = AppSecurityManager.k1 + AppSecurityManager.k2 +
                        AppSecurityManager.k3 + AppSecurityManager.k4 + AppSecurityManager.k5
                    val iv = AppSecurityManager.iv1 + AppSecurityManager.iv2 +
                        AppSecurityManager.ivf

                    try {
                        configString.put("appVersion", 22)
                        configString.put("ConfigValidityDate", mValidade)
                        configString.put("ConnectionMode", SecondVPN.getConnectionMode())
                        configString.put("isHTTPDirect", SecondVPN.isHTTPDirect())
                        configString.put("isPayloadAfterTLS", SecondVPN.isPayloadAfterTLS())
                        configString.put("isLockConfig", lockConfigFromEdit.isChecked)
                        configString.put("isLockAppSettings", lockSettingsApp.isChecked)
                        configString.put("isEnableUdp", SecondVPN.isEnableCustomUDP())
                        configString.put("UDPAddr", SecondVPN.getUDPResolver())
                        configString.put("isEnableDNS", SecondVPN.isEnableCustomDNS())
                        configString.put("DNS1", SecondVPN.customDNS1())
                        configString.put("DNS2", SecondVPN.customDNS2())
                        configString.put("ConfigMsg", configMsg.text.toString())
                        configString.put("onlyMobileData", onlyMobileData.isChecked)
                        configString.put("TLSVersion", SecondVPN.getTLSVersion())
                        configString.put("CurrentPayload", SecondVPN.getPayloadKey())
                        configString.put("ServerSSH", SecondVPN.getServidorSSHDomain())
                        configString.put("ServerProxy", SecondVPN.getProxyIPDomain())
                        configString.put("ServerSNI", SecondVPN.getSNI())
                        configString.put("LockEditLogin", lockLogin.isChecked)
                        configString.put("ConfigAuthData", SecondVPN.getUsuarioAndPass())
                    } catch (error: Exception) {
                        Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show()
                        error.printStackTrace()
                    }

                    var encryptedConfig: String? = ""
                    try {
                        encryptedConfig = AppSecurityManager.encryptStrAndToBase64(
                            iv,
                            key,
                            configString.toString()
                        )
                    } catch (error: Exception) {
                        encryptedConfig = null
                        error.printStackTrace()
                    }

                    configToExportOnPermisson = encryptedConfig!!.replace("\n", "")
                    val uri = Uri.parse(
                        "file://" + Environment.getExternalStorageDirectory() + "/Download"
                    )
                    createFile(uri)
                }
            }
        }
    }

    private fun createFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*text/plain"
        intent.putExtra(
            Intent.EXTRA_TITLE,
            configName.text.toString() + ".vpnlite"
        )
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        startActivityForResult(intent, CREATE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE) {
            if (resultCode == RESULT_OK) {
                try {
                    val uri = data!!.data!!
                    val outputStream: OutputStream =
                        contentResolver.openOutputStream(uri)!!
                    outputStream.write(configToExportOnPermisson!!.toByteArray())
                    outputStream.close()
                    Toast.makeText(this, getString(R.string.export_sucess), Toast.LENGTH_SHORT).show()
                    finish()
                } catch (error: IOException) {
                    Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show()
                    error.printStackTrace()
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (validateDate?.isChecked == true) {
            setValidadeDate()
        } else {
            mValidade = 0
        }
    }

    private fun setValidadeDate() {
        val calendar = Calendar.getInstance()
        val timeHoje = calendar.timeInMillis
        calendar.timeInMillis = timeHoje + (1000 * 60 * 60 * 24).toLong()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        mValidade = calendar.timeInMillis

        val dialog = DatePickerDialog(
            this,
            R.style.DATE_DIALOG_THEME,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selected = Calendar.getInstance()
                selected.set(selectedYear, selectedMonth, selectedDay)
                mValidade = selected.timeInMillis
            },
            year,
            month,
            day
        )

        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(R.string.ok)
        ) { _, _ ->
            val dateFormat = DateFormat.getDateInstance()
            val datePicker = dialog.datePicker
            val selected = Calendar.getInstance()
            selected.set(
                datePicker.year,
                datePicker.month,
                datePicker.dayOfMonth
            )
            mValidade = selected.timeInMillis

            if (mValidade < timeHoje) {
                mValidade = 0
                Toast.makeText(
                    applicationContext,
                    R.string.error_date_selected_invalid,
                    Toast.LENGTH_SHORT
                ).show()
                validateDate?.isChecked = false
            } else {
                val days = (mValidade - timeHoje) / 1000 / 60 / 60 / 24
                validateDate?.let {
                    it.visibility = View.VISIBLE
                    it.text = String.format("%s (%s)", days, dateFormat.format(mValidade))
                }
            }
        }

        dialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            getString(R.string.cancel)
        ) { _, _ ->
            mValidade = 0
            validateDate?.isChecked = false
        }

        dialog.setOnCancelListener {
            mValidade = 0
            validateDate?.isChecked = false
        }
        dialog.show()
    }

    companion object {
        @JvmField
        var app_prefs: SharedPreferences? = null

        private const val CREATE_FILE = 1
    }
}
