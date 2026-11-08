package app.one.secondvpnlite.drawer

import android.app.Activity
import android.os.Handler
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.one.secondvpnlite.R

/**
 * Log drawer controller.
 * Original drawer setup and listener behavior retained.
 */
class DrawerLog(private val activity: Activity) : LogsAdapter.OnItemClickListener {

    private val handler = Handler()
    private var drawerLayoutRef: DrawerLayout? = null
    private lateinit var adapter: LogsAdapter

    fun setDrawer(listener: DrawerLayout.DrawerListener) {
        drawerLayoutRef = activity.findViewById(R.id.drawerLayout)
        val drawerListView: RecyclerView = activity.findViewById(R.id.recyclerDrawerView)

        drawerLayoutRef?.addDrawerListener(listener)

        val layoutManager = LinearLayoutManager(activity)
        adapter = LogsAdapter(layoutManager, activity)
        adapter.setOnItemClickListener(this)

        drawerListView.adapter = adapter
        drawerListView.layoutManager = layoutManager

        adapter.scrollToLastPosition()
    }

    fun getDrawerLayout(): DrawerLayout? = drawerLayoutRef

    fun clearLogs() {
        adapter.clearLog()
    }

    override fun onItemClick(view: View, position: Int, logText: String) {
    }

    override fun onItemLongClick(view: View, position: Int, logText: String) {
        // Original clipboard action remains disabled.
    }

    fun onResume() {
        adapter.setLogLevel(3)
    }

    fun onDestroy() {
        // Original log-listener removal remains disabled.
    }
}
