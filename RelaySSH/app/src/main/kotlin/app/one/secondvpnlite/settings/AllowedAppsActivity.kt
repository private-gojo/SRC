package app.one.secondvpnlite.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.one.secondvpnlite.R

class AllowedAppsActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var splitTunnelRV: RecyclerView
    private lateinit var appAdapter: AppListAdapter
    private lateinit var selectAllBtn: Button
    private lateinit var deselectAllBtn: Button
    private lateinit var resolveInfoList: List<ResolveInfo>
    private lateinit var packageManager: PackageManager
    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowed_app)
        initializeViews()
        setAdapterPackageManager()
        setStatusBar(this, R.color.black)
    }

    private fun setAdapterPackageManager() {
        appAdapter = AppListAdapter(resolveInfoList, packageManager, this)
        splitTunnelRV.adapter = appAdapter
    }

    private fun initializeViews() {
        splitTunnelRV = findViewById(R.id.splitRecyclerView)
        selectAllBtn = findViewById(R.id.btnSelectAll)
        deselectAllBtn = findViewById(R.id.btnDeselectAll)
        toolbarTitle = findViewById(R.id.toolbar_title)
        btnBack = findViewById(R.id.btnBack)
        packageManager = getPackageManager()
        resolveInfoList = getInstalledApps(packageManager)

        toolbarTitle.visibility = View.VISIBLE
        toolbarTitle.text = "Split Tunneling"
        btnBack.visibility = View.VISIBLE

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        selectAllBtn.setOnClickListener {
            appAdapter.selectAll()
            ShowToast("Blocked All Apps")
            appAdapter.notifyDataSetChanged()
        }

        deselectAllBtn.setOnClickListener {
            appAdapter.deselectAll()
            ShowToast("Unblocked All Apps")
            appAdapter.notifyDataSetChanged()
        }
    }

    private fun ShowToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun getInstalledApps(manager: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(intent, 0)
    }

    companion object {
        @JvmStatic
        fun setStatusBar(activity: Activity, color: Int) {
            activity.window.statusBarColor = ContextCompat.getColor(activity, color)
        }
    }
}
