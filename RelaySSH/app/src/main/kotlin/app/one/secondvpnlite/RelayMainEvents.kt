package app.one.secondvpnlite

import android.view.View
import android.widget.LinearLayout

/**
 * Kotlin-owned HTTP/HTTPS mode and payload option event handling.
 * MainActivity keeps the Android listener interface and Java savePreferences callback.
 */
object RelayMainEvents {
    @JvmStatic
    fun onCheckedChanged(
        activity: MainActivity,
        intentToImport: Boolean,
        savePreferences: Runnable
    ) {
        val httpMode = MainActivity.modo_http!!
        val httpsMode = MainActivity.modo_https!!
        val payloadAfterTls = MainActivity.payload_after_tls!!
        val directMode = MainActivity.direct_mode!!

        val customSniLayout = activity.findViewById<LinearLayout>(
            R.id.activity_mainInputSNILayout
        )
        val payloadLayout = activity.findViewById<LinearLayout>(
            R.id.activity_mainInputPayloadLinearLayout
        )
        val proxyLayout = activity.findViewById<LinearLayout>(
            R.id.activity_mainInputServerPROXYIPORTLayout
        )

        // Preserve the original HTTP mode behavior.
        if (httpMode.isChecked) {
            MainActivity.app_prefs!!.edit()
                .putString("CONNECTION_MODE", "MODO_HTTP")
                .apply()

            payloadAfterTls.isEnabled = false
            directMode.isEnabled = true
            payloadAfterTls.visibility = View.GONE
            customSniLayout.visibility = View.GONE
            payloadLayout.visibility = View.VISIBLE
            directMode.visibility = View.VISIBLE

            if (directMode.isChecked) {
                if (!SecondVPN.getIsCustomFileIsLocked()) {
                    MainActivity.app_prefs!!.edit()
                        .putBoolean("IS_HTTP_DIRECT", true)
                        .apply()
                }
                proxyLayout.visibility = View.GONE
            } else {
                if (!SecondVPN.getIsCustomFileIsLocked()) {
                    MainActivity.app_prefs!!.edit()
                        .putBoolean("IS_HTTP_DIRECT", false)
                        .apply()
                }
                proxyLayout.visibility = View.VISIBLE
            }
        }

        // Preserve the original HTTPS mode behavior.
        if (httpsMode.isChecked) {
            MainActivity.app_prefs!!.edit()
                .putString("CONNECTION_MODE", "MODO_HTTPS")
                .apply()

            payloadAfterTls.isEnabled = true
            directMode.isEnabled = false
            payloadAfterTls.visibility = View.VISIBLE
            customSniLayout.visibility = View.VISIBLE
            directMode.visibility = View.GONE

            if (payloadAfterTls.isChecked) {
                if (!SecondVPN.getIsCustomFileIsLocked()) {
                    MainActivity.app_prefs!!.edit()
                        .putBoolean("PAYLOAD_AFTER_TLS", true)
                        .apply()
                }
                payloadLayout.visibility = View.VISIBLE
                proxyLayout.visibility = View.GONE
            } else {
                if (!SecondVPN.getIsCustomFileIsLocked()) {
                    MainActivity.app_prefs!!.edit()
                        .putBoolean("PAYLOAD_AFTER_TLS", false)
                        .apply()
                }
                payloadLayout.visibility = View.GONE
                proxyLayout.visibility = View.GONE
            }
        }

        if (!intentToImport) {
            savePreferences.run()
        }
    }
}
