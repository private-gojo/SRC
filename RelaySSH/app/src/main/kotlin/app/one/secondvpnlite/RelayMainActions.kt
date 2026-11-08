package app.one.secondvpnlite

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import app.one.secondvpnlite.configs.ExportConfig
import app.one.secondvpnlite.configs.ImportConfig
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.settings.AppSettings

/**
 * Kotlin-owned MainActivity menu, click, and drawer actions.
 * MainActivity retains lifecycle callbacks and bridges to its private state.
 */
object RelayMainActions {

    @JvmStatic
    fun onClick(activity: MainActivity, view: View) {
        if (view.id == R.id.activity_StartConnection) {
            activity.setStartOrStopVPNFromActions()
        } else if (view.id == R.id.logs) {
            activity.showLogFromActions()
        }
    }

    @JvmStatic
    fun onDrawerClosed(activity: MainActivity, drawerView: View) {
        if (drawerView.id == R.id.activity_mainLogsDrawerLinear) {
            activity.app_toolbar.menu.clear()
            activity.menuInflater.inflate(R.menu.main_menu, activity.app_toolbar.menu)
        }
    }

    @JvmStatic
    fun handleOptionsMenu(activity: MainActivity, item: MenuItem) {
        when (item.itemId) {
            R.id.import_file -> {
                activity.setIntentToImportFromActions()
                if (isConnected(activity)) {
                    showOnlyDisconnected(activity, Toast.LENGTH_SHORT)
                } else {
                    activity.startActivity(Intent(activity, ImportConfig::class.java))
                }
            }
            R.id.clearlogs -> AppLogManager.clearLog()
            R.id.clearconfig -> {
                if (isConnected(activity)) {
                    showOnlyDisconnected(activity, Toast.LENGTH_SHORT)
                } else {
                    AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                        .setTitle(activity.getString(R.string.reset_app_config_title))
                        .setCancelable(true)
                        .setPositiveButton("Ok") { dialog, _ ->
                            SecondVPN.setDefaultPrefs()
                            activity.clearTextviewsFromActions()
                            MainActivity.updateUIAfterImport()
                        }
                        .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .create()
                        .show()
                }
            }
            R.id.forcestop -> android.os.Process.killProcess(android.os.Process.myPid())
            R.id.cleardata -> {
                if (isConnected(activity)) {
                    showOnlyDisconnected(activity, Toast.LENGTH_SHORT)
                } else {
                    AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                        .setTitle(activity.getString(R.string.reset_app_title))
                        .setMessage(activity.getString(R.string.reset_app_txt))
                        .setCancelable(true)
                        .setPositiveButton("Ok") { _, _ ->
                            try {
                                Runtime.getRuntime().exec(
                                    "pm clear " + activity.applicationContext.packageName
                                )
                            } catch (error: Exception) {
                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.operation_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                error.printStackTrace()
                            }
                        }
                        .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .create()
                        .show()
                }
            }
            R.id.export_file -> {
                activity.savePreferencesFromActions()
                if (isConnected(activity)) {
                    showOnlyDisconnected(activity, Toast.LENGTH_SHORT)
                } else if (!activity.checkConfigFromActions(true)) {
                    if (activity.isExportWithoutLoginFromActions()) {
                        if (!SecondVPN.getIsCustomFileIsLocked()) {
                            activity.startActivity(Intent(activity, ExportConfig::class.java))
                        } else {
                            showMessage(activity, R.string.block_by_config_author)
                        }
                    } else {
                        showMessage(activity, R.string.empty_configs_export)
                    }
                } else if (SecondVPN.getIsCustomFileLockSettings()) {
                    showMessage(activity, R.string.block_by_config_author)
                } else if (!SecondVPN.getIsCustomFileIsLocked()) {
                    activity.startActivity(Intent(activity, ExportConfig::class.java))
                } else {
                    showMessage(activity, R.string.block_by_config_author)
                }
            }
            R.id.settings -> {
                if (isConnected(activity)) {
                    showOnlyDisconnected(activity, Toast.LENGTH_SHORT)
                } else if (SecondVPN.getIsCustomFileLockSettings()) {
                    showMessage(activity, R.string.block_by_config_author)
                } else {
                    activity.startActivity(Intent(activity, AppSettings::class.java))
                }
            }
            R.id.minimize -> {
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(startMain)
            }
            R.id.exit_app -> activity.finishAffinity()
        }
    }

    private fun isConnected(activity: MainActivity): Boolean {
        return MainActivity.start_button!!.text.toString() ==
            activity.getString(R.string.stop)
    }

    private fun showOnlyDisconnected(activity: MainActivity, duration: Int) {
        Toast.makeText(activity, activity.getString(R.string.only_disconnected), duration).show()
    }

    private fun showMessage(activity: MainActivity, messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_LONG).show()
    }
}
