package app.one.secondvpnlite

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.VpnService
import android.os.PowerManager
import android.widget.Toast
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.service.TunnelManager
import java.util.concurrent.Callable

/**
 * Kotlin-owned VPN permission result and connection startup flow.
 * MainActivity keeps the Android lifecycle callback and private-state bridge.
 */
object RelayMainVpnFlow {

    @JvmStatic
    fun requestPermission(activity: MainActivity, onPermissionGranted: Runnable) {
        val permissionIntent = VpnService.prepare(activity)

        if (permissionIntent != null) {
            try {
                activity.startActivityForResult(
                    permissionIntent,
                    MainActivity.START_VPN_PROFILE
                )
            } catch (error: ActivityNotFoundException) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.error_request_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            onPermissionGranted.run()
        }
    }

    @JvmStatic
    fun handlePermissionResult(
        activity: MainActivity,
        requestCode: Int,
        resultCode: Int,
        validateConfig: Callable<Boolean>,
        showLogs: Runnable,
        startTunnelManager: Runnable
    ) {
        if (requestCode != MainActivity.START_VPN_PROFILE ||
            resultCode != Activity.RESULT_OK
        ) {
            return
        }

        if (validateConfig.call()) {
            // Preserve the original connection startup sequence.
            MainActivity.app_prefs!!.edit().putBoolean("LAST_A", false).apply()
            MainActivity.disableInterfaceOnConnect()
            AppLogManager.clearLog()

            if (SecondVPN.isShowLogs()) {
                showLogs.run()
            }

            if (SecondVPN.isEnableWakeLock()) {
                try {
                    MainActivity.wakeLock!! = (
                        activity.getSystemService(Context.POWER_SERVICE) as PowerManager
                    ).newWakeLock(1, "SecondVPNLite::WakeLock")
                } catch (error: Exception) {
                    error.printStackTrace()
                }
            }

            try {
                TunnelManager.isToStopService = false
                TunnelManager.stoppedLog = false
                TunnelManager.stoppingLog = false
                TunnelManager.vpnDestroyedLog = false
                startTunnelManager.run()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        } else {
            Toast.makeText(
                activity,
                activity.getString(R.string.empty_configs),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    @JvmStatic
    fun stopTunnel(startTunnelManager: Runnable) {
        try {
            TunnelManager.stopForwarderSocks()
            TunnelManager.isToStopService = true
            startTunnelManager.run()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @JvmStatic
    fun setStartOrStop(
        activity: MainActivity,
        savePreferences: Runnable,
        startVpn: Runnable,
        stopVpn: Runnable
    ) {
        if (!MainActivity.start_button!!.text.toString()
                .equals(activity.getString(R.string.stop))
        ) {
            savePreferences.run()
            startVpn.run()
            android.util.Log.d("MainActivity", "Start VPN Tunnel")
        } else {
            stopVpn.run()
            android.util.Log.d("MainActivity", "Stop VPN Tunnel")
        }
    }
}
