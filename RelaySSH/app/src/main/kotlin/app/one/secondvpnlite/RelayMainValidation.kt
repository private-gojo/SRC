package app.one.secondvpnlite

import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import app.one.secondvpnlite.tunnel.TunnelUtils
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Kotlin-owned VPN configuration validation.
 * Validation results are returned to the Java MainActivity compatibility layer.
 */
object RelayMainValidation {
    data class ValidationResult(
        val configIsOk: Boolean,
        val exportWithoutLogin: Boolean
    )

    @JvmStatic
    fun validate(activity: MainActivity, istoSave: Boolean): ValidationResult {
        var configIsOk = true
        var exportWithoutLogin = false

        // Network validation.
        if (!TunnelUtils.isNetworkOnline(activity)) {
            if (!istoSave) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.no_network),
                    Toast.LENGTH_SHORT
                ).show()
                configIsOk = false
            }
        }

        // Username and password validation.
        if (!istoSave) {
            exportWithoutLogin = true
            try {
                val ipSplit = javaSplit(SecondVPN.getUsuarioAndPass(), "@")
                val user = ipSplit[0]
                val passwd = ipSplit[1]

                if (user.isEmpty()) {
                    configIsOk = false
                }
                if (passwd.isEmpty()) {
                    configIsOk = false
                }
            } catch (error: Exception) {
                configIsOk = false
                error.printStackTrace()
            }
        } else {
            exportWithoutLogin = false
        }

        // SSH server validation.
        try {
            val ipSplit = javaSplit(SecondVPN.getServidorSSHDomain(), ":")
            val servidor = ipSplit[0]
            val porta = Integer.parseInt(ipSplit[1])

            if (servidor.isEmpty()) {
                configIsOk = false
            }
            if (porta == 0) {
                configIsOk = false
            }
        } catch (error: Exception) {
            configIsOk = false
            error.printStackTrace()
        }

        if (SecondVPN.getConnectionMode() == "MODO_HTTP") {
            if (MainActivity.direct_mode!!.isChecked) {
                if (SecondVPN.getServidorSSHDomain().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getPayloadKey().isEmpty()) {
                    configIsOk = false
                }
            } else {
                try {
                    val ipSplit = javaSplit(SecondVPN.getProxyIPDomain(), ":")
                    val servidor = ipSplit[0]
                    val porta = Integer.parseInt(ipSplit[1])

                    if (servidor.isEmpty()) {
                        configIsOk = false
                    }
                    if (porta == 0) {
                        configIsOk = false
                    }
                } catch (error: Exception) {
                    configIsOk = false
                    error.printStackTrace()
                }

                if (SecondVPN.getServidorSSHDomain().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getPayloadKey().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getProxyIPDomain().isEmpty()) {
                    configIsOk = false
                }
            }
        }

        if (SecondVPN.getConnectionMode() == "MODO_HTTPS") {
            if (MainActivity.payload_after_tls!!.isChecked) {
                if (SecondVPN.getServidorSSHDomain().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getSNI().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getPayloadKey().isEmpty()) {
                    configIsOk = false
                }
            } else {
                if (SecondVPN.getServidorSSHDomain().isEmpty()) {
                    configIsOk = false
                }
                if (SecondVPN.getSNI().isEmpty()) {
                    configIsOk = false
                }
            }
        }

        if (SecondVPN.getIsCustomFileIsLocked()) {
            if (SecondVPN.getIsCustomFileIsLocked() &&
                SecondVPN.isIsCustomConfigOnlyMobileData()
            ) {
                val wifiManager = activity.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                if (wifiManager.isWifiEnabled) {
                    configIsOk = false
                    if (!istoSave) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.only_mobile_data_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            if (isExpired(SecondVPN.data_validade_file())) {
                configIsOk = false
                if (!istoSave) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.file_validate_end),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return ValidationResult(configIsOk, exportWithoutLogin)
    }

    @JvmStatic
    fun isExpired(validityDateMillis: Long): Boolean {
        if (validityDateMillis == 0L) {
            return false
        }

        val currentDateMillis = Calendar.getInstance().timeInMillis
        return currentDateMillis >= validityDateMillis
    }

    // Match Java String.split(regex) behavior, including trailing-empty removal.
    private fun javaSplit(value: String, regex: String): Array<String> {
        return Pattern.compile(regex).split(value)
    }
}
