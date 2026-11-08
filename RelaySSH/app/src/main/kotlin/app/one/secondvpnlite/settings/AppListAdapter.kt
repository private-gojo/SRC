package app.one.secondvpnlite.settings

/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import app.one.secondvpnlite.R
import app.one.secondvpnlite.util.SharedPref
import java.util.HashSet

class AppListAdapter(
    private val resolveInfoList: List<ResolveInfo>,
    private val packageManager: PackageManager,
    context: Context
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val sharedPref: SharedPref = SharedPref.getInstance(context)
    private val selectedApps: MutableSet<String> =
        sharedPref.getStringSet("selectedApps", HashSet()) ?: HashSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val resolveInfo = resolveInfoList[position]

        Thread {
            val drawable: Drawable = resolveInfo.loadIcon(packageManager)
            holder.itemView.post {
                holder.appIcon.setImageDrawable(drawable)
            }
        }.start()

        holder.appTitle.text = resolveInfo.loadLabel(packageManager)
        holder.appPackageName.text = resolveInfo.activityInfo.packageName

        holder.mSwitch.isChecked =
            selectedApps.contains(resolveInfo.activityInfo.packageName)

        holder.mSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(resolveInfo.activityInfo.packageName)
            } else {
                selectedApps.remove(resolveInfo.activityInfo.packageName)
                sharedPref.removeAppFromList(resolveInfo.activityInfo.packageName)
                sharedPref.putStringSet("selectedApps", selectedApps)
            }

            sharedPref.putStringSet("selectedApps", selectedApps)
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, isChecked)
        }
    }

    override fun getItemCount(): Int = resolveInfoList.size

    fun selectAll() {
        for (resolveInfo in resolveInfoList) {
            selectedApps.add(resolveInfo.activityInfo.packageName)
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, true)
        }
    }

    fun deselectAll() {
        for (resolveInfo in resolveInfoList) {
            selectedApps.remove(resolveInfo.activityInfo.packageName)
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, false)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appTitle: TextView = itemView.findViewById(R.id.appName)
        val mSwitch: SwitchCompat = itemView.findViewById(R.id.switchBtn)
        val appPackageName: TextView = itemView.findViewById(R.id.packageName)
    }
}
