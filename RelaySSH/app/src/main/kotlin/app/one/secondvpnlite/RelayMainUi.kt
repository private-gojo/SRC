package app.one.secondvpnlite

import android.app.Activity
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import app.one.secondvpnlite.service.TunnelManager

/**
 * Kotlin-owned main-screen UI state and configuration binding.
 *
 * Public Java entry points remain on MainActivity during the incremental migration.
 */
object RelayMainUi {
    @JvmStatic
    fun runOnUI(runnable: Runnable) {
        MainActivity.UIHandler.post(runnable)
    }

    @JvmStatic
    fun setButtonStatus(status: String) {
        val button = MainActivity.start_button
        when (status) {
            "INICIANDO", "CONECTANDO", "CONECTADO" -> {
                runOnUI(Runnable { button?.setText(R.string.stop) })
                try {
                    button?.isEnabled = true
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }

            "DESCONECTADO" -> runOnUI(Runnable {
                enableInterfaceOnConnect()
                button?.setText(R.string.start)
                try {
                    button?.isEnabled = true
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            })

            "PARANDO" -> runOnUI(Runnable {
                button?.setText(R.string.stopping)
                try {
                    button?.isEnabled = false
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            })
        }
    }

    @JvmStatic
    fun updateUIAfterImport() {
        if (!TunnelManager.isServiceRunning) {
            runOnUI(Runnable {
                updateLayoutWithConfig1()
                updateLayoutWithConfig2()
                updateLayoutWithConfig3()
            })
        }
    }

    @JvmStatic
    fun updateLayoutWithConfig1() {
        val payloadAfterTls = MainActivity.payload_after_tls
        val directMode = MainActivity.direct_mode
        val httpMode = MainActivity.modo_http
        val httpsMode = MainActivity.modo_https
        val context = MainActivity.sActivity as? Activity ?: return
        val customSniLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputSNILayout)
        val proxyLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerPROXYIPORTLayout)
        val payloadLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputPayloadLinearLayout)

        if (!SecondVPN.getIsCustomFileIsLocked() && SecondVPN.getConnectionMode() == "MODO_HTTP") {
            payloadAfterTls?.isEnabled = false
            payloadAfterTls?.visibility = View.GONE
            directMode?.isEnabled = true
            directMode?.visibility = View.VISIBLE
            httpMode?.isChecked = true
            httpsMode?.isChecked = false
            customSniLayout?.visibility = View.GONE

            if (SecondVPN.isHTTPDirect()) {
                directMode?.isChecked = true
                proxyLayout?.visibility = View.GONE
            } else {
                directMode?.isChecked = false
                proxyLayout?.visibility = View.VISIBLE
            }
        }

        if (!SecondVPN.getIsCustomFileIsLocked() && SecondVPN.getConnectionMode() == "MODO_HTTPS") {
            payloadAfterTls?.isEnabled = true
            payloadAfterTls?.visibility = View.VISIBLE
            directMode?.isEnabled = false
            httpMode?.isChecked = false
            directMode?.visibility = View.GONE
            httpsMode?.isChecked = true
            customSniLayout?.visibility = View.VISIBLE
            proxyLayout?.visibility = View.GONE
            if (SecondVPN.isPayloadAfterTLS()) {
                payloadLayout?.visibility = View.VISIBLE
                payloadAfterTls?.isChecked = true
            } else {
                payloadAfterTls?.isChecked = false
                payloadLayout?.visibility = View.GONE
            }
        }
    }

    @JvmStatic
    fun updateLayoutWithConfig2() {
        val httpsMode = MainActivity.modo_https
        val httpMode = MainActivity.modo_http
        val directMode = MainActivity.direct_mode
        val payloadAfterTls = MainActivity.payload_after_tls
        val context = MainActivity.sActivity as? Activity ?: return
        val customSniLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputSNILayout)

        if (SecondVPN.getConnectionMode() == "MODO_HTTPS") {
            httpsMode?.isChecked = true
            httpMode?.isChecked = false
            directMode?.visibility = View.GONE
            payloadAfterTls?.visibility = View.VISIBLE
            customSniLayout?.visibility = View.VISIBLE
            payloadAfterTls?.isChecked = SecondVPN.isPayloadAfterTLS()
        }

        if (SecondVPN.getConnectionMode() == "MODO_HTTP") {
            httpsMode?.isChecked = false
            httpMode?.isChecked = true
            directMode?.visibility = View.VISIBLE
            payloadAfterTls?.visibility = View.GONE
            customSniLayout?.visibility = View.GONE
            directMode?.isChecked = SecondVPN.isHTTPDirect()
        }
    }

    @JvmStatic
    fun updateLayoutWithConfig3() {
        val context = MainActivity.sActivity as? Activity ?: return
        val userAndPass = MainActivity.user_and_pass
        val sshServer = MainActivity.servidor_ssh
        val proxyServer = MainActivity.servidor_proxy
        val customSni = MainActivity.custom_sni
        val customPayload = MainActivity.custom_payload
        val httpMode = MainActivity.modo_http
        val httpsMode = MainActivity.modo_https
        val payloadAfterTls = MainActivity.payload_after_tls
        val directMode = MainActivity.direct_mode
        val configMessage = MainActivity.config_msg

        val proxyLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerPROXYIPORTLayout)
        val payloadLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputPayloadLinearLayout)

        if (SecondVPN.getIsCustomFileIsLocked()) {
            if (SecondVPN.isLockLoginEdit()) {
                userAndPass?.isEnabled = false
                userAndPass?.text = SecondVPN.getUsuarioAndPass().replace(Regex("."), "*")
            } else {
                userAndPass?.text = SecondVPN.getUsuarioAndPass()
                userAndPass?.isEnabled = true
            }

            sshServer?.isEnabled = false
            sshServer?.text = SecondVPN.getServidorSSHDomain().replace(Regex("."), "*")
            proxyServer?.isEnabled = false
            proxyServer?.text = SecondVPN.getProxyIPDomain().replace(Regex("."), "*")
            customSni?.isEnabled = false
            customSni?.text = SecondVPN.getSNI().replace(Regex("."), "*")
            customPayload?.isEnabled = false
            customPayload?.text = SecondVPN.getPayloadKey().replace(Regex("."), "*")

            if (httpMode?.isChecked == true) {
                if (directMode?.isChecked == true) {
                    proxyLayout?.visibility = View.GONE
                    directMode.isChecked = true
                    directMode.isEnabled = false
                    payloadLayout?.visibility = View.VISIBLE
                    payloadAfterTls?.visibility = View.GONE
                    directMode.visibility = View.VISIBLE
                } else {
                    proxyLayout?.visibility = View.VISIBLE
                    payloadLayout?.visibility = View.VISIBLE
                    payloadAfterTls?.visibility = View.GONE
                    directMode?.visibility = View.VISIBLE
                    directMode?.isChecked = false
                    directMode?.isEnabled = false
                }
            }

            if (httpsMode?.isChecked == true) {
                if (payloadAfterTls?.isChecked == true) {
                    proxyLayout?.visibility = View.GONE
                    payloadAfterTls.isChecked = true
                    payloadAfterTls.isEnabled = false
                    payloadLayout?.visibility = View.VISIBLE
                    payloadAfterTls.visibility = View.VISIBLE
                } else {
                    proxyLayout?.visibility = View.GONE
                    payloadAfterTls?.isChecked = false
                    payloadAfterTls?.isEnabled = false
                    payloadLayout?.visibility = View.GONE
                    payloadAfterTls?.visibility = View.VISIBLE
                }
            }

            httpMode?.isEnabled = false
            httpsMode?.isEnabled = false
            configMessage?.isEnabled = true
            configMessage?.visibility = View.VISIBLE
            configMessage?.text = Html.fromHtml(SecondVPN.getConfigMsgText())
        } else {
            sshServer?.isEnabled = true
            sshServer?.text = SecondVPN.getServidorSSHDomain()
            proxyServer?.isEnabled = true
            proxyServer?.text = SecondVPN.getProxyIPDomain()
            customSni?.isEnabled = true
            customSni?.text = SecondVPN.getSNI()
            customPayload?.isEnabled = true
            customPayload?.text = SecondVPN.getPayloadKey()
            httpMode?.isEnabled = true
            httpsMode?.isEnabled = true
            payloadAfterTls?.isEnabled = true
            directMode?.isEnabled = true
            userAndPass?.isEnabled = true
            userAndPass?.text = SecondVPN.getUsuarioAndPass()
            configMessage?.isEnabled = true
            configMessage?.visibility = View.VISIBLE
            configMessage?.text = Html.fromHtml(SecondVPN.getConfigMsgText())
        }
    }

    @JvmStatic
    fun disableInterfaceOnConnect() {
        try {
            val context = MainActivity.sActivity as? Activity ?: return
            val sshLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerSSHIPORTLayout)
            val proxyLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerPROXYIPORTLayout)
            val userLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputUserandPassLayout)
            val payloadLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputPayloadLinearLayout)

            if (sshLayout?.visibility == View.VISIBLE) MainActivity.servidor_ssh?.isEnabled = false
            if (proxyLayout?.visibility == View.VISIBLE) MainActivity.servidor_proxy?.isEnabled = false
            if (userLayout?.visibility == View.VISIBLE) MainActivity.user_and_pass?.isEnabled = false
            if (payloadLayout?.visibility == View.VISIBLE) MainActivity.custom_payload?.isEnabled = false
            if (MainActivity.direct_mode?.visibility == View.VISIBLE) MainActivity.direct_mode?.isEnabled = false
            if (MainActivity.payload_after_tls?.visibility == View.VISIBLE) MainActivity.payload_after_tls?.isEnabled = false
            if (MainActivity.custom_sni?.visibility == View.VISIBLE) MainActivity.custom_sni?.isEnabled = false
            if (MainActivity.modo_http?.visibility == View.VISIBLE) MainActivity.modo_http?.isEnabled = false
            if (MainActivity.modo_https?.visibility == View.VISIBLE) MainActivity.modo_https?.isEnabled = false
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @JvmStatic
    fun enableInterfaceOnConnect() {
        try {
            val context = MainActivity.sActivity as? Activity ?: return
            val sshLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerSSHIPORTLayout)
            val proxyLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputServerPROXYIPORTLayout)
            val userLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputUserandPassLayout)
            val payloadLayout = context.findViewById<LinearLayout>(R.id.activity_mainInputPayloadLinearLayout)

            MainActivity.user_and_pass?.isEnabled = !SecondVPN.isLockLoginEdit()
            if (!SecondVPN.getIsCustomFileIsLocked()) {
                if (sshLayout?.visibility == View.VISIBLE) MainActivity.servidor_ssh?.isEnabled = true
                if (proxyLayout?.visibility == View.VISIBLE) MainActivity.servidor_proxy?.isEnabled = true
                if (userLayout?.visibility == View.VISIBLE) MainActivity.user_and_pass?.isEnabled = true
                if (payloadLayout?.visibility == View.VISIBLE) MainActivity.custom_payload?.isEnabled = true
                if (MainActivity.direct_mode?.visibility == View.VISIBLE) MainActivity.direct_mode?.isEnabled = true
                if (MainActivity.payload_after_tls?.visibility == View.VISIBLE) MainActivity.payload_after_tls?.isEnabled = true
                if (MainActivity.custom_sni?.visibility == View.VISIBLE) MainActivity.custom_sni?.isEnabled = true
                if (MainActivity.modo_http?.visibility == View.VISIBLE) MainActivity.modo_http?.isEnabled = true
                if (MainActivity.modo_https?.visibility == View.VISIBLE) MainActivity.modo_https?.isEnabled = true
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
}
