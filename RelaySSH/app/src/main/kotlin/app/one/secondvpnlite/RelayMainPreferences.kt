package app.one.secondvpnlite

/**
 * Kotlin-owned persistence for the main connection form.
 * SharedPreferences keys and conditional save behavior match the Java implementation.
 */
object RelayMainPreferences {

    @JvmStatic
    fun save() {
        if (SecondVPN.getIsCustomFileIsLocked()) {
            if (!SecondVPN.isLockLoginEdit()) {
                MainActivity.app_prefs!!.edit()
                    .putString(
                        "SSH_AUTH_DATA",
                        MainActivity.user_and_pass!!.text.toString()
                    )
                    .apply()
            }
        }

        if (!SecondVPN.getIsCustomFileIsLocked()) {
            // Preserve the original separate preference writes.
            MainActivity.app_prefs!!.edit()
                .putString(
                    "SSH_AUTH_DATA",
                    MainActivity.user_and_pass!!.text.toString()
                )
                .apply()

            if (MainActivity.modo_http!!.isChecked) {
                if (MainActivity.direct_mode!!.isChecked) {
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "SSH_SERVER_DOMAIN",
                            MainActivity.servidor_ssh!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "PAYLOAD_KEY",
                            MainActivity.custom_payload!!.text.toString()
                        )
                        .apply()
                } else {
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "SSH_SERVER_DOMAIN",
                            MainActivity.servidor_ssh!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "PAYLOAD_KEY",
                            MainActivity.custom_payload!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "PROXY_IP_DOMAIN",
                            MainActivity.servidor_proxy!!.text.toString()
                        )
                        .apply()
                }
            }

            if (MainActivity.modo_https!!.isChecked) {
                if (MainActivity.payload_after_tls!!.isChecked) {
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "SSH_SERVER_DOMAIN",
                            MainActivity.servidor_ssh!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "PAYLOAD_KEY",
                            MainActivity.custom_payload!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "CUSTOM_SNI",
                            MainActivity.custom_sni!!.text.toString()
                        )
                        .apply()
                } else {
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "SSH_SERVER_DOMAIN",
                            MainActivity.servidor_ssh!!.text.toString()
                        )
                        .apply()
                    MainActivity.app_prefs!!.edit()
                        .putString(
                            "CUSTOM_SNI",
                            MainActivity.custom_sni!!.text.toString()
                        )
                        .apply()
                }
            }
        }
    }
}
