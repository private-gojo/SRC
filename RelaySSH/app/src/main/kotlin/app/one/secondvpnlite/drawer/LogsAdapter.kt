package app.one.secondvpnlite.drawer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Html
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.one.secondvpnlite.R
import app.one.secondvpnlite.logs.AppLogManager
import app.one.secondvpnlite.logs.LogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Vector

/**
 * Kotlin translation of the log drawer RecyclerView adapter.
 * Handler delivery, log limits, filtering, and click callbacks are preserved.
 */
class LogsAdapter(
    private val linearLayoutManager: LinearLayoutManager,
    private val context: Context
) : RecyclerView.Adapter<LogsAdapter.logViewHolder>(),
    AppLogManager.LogListener,
    Handler.Callback,
    View.OnTouchListener {

    companion object {
        private const val MESSAGE_NEWLOG = 0
        private const val MESSAGE_CLEARLOG = 1
        private const val MESSAGE_NEWTS = 2
        private const val MESSAGE_NEWLOGLEVEL = 3

        const val TIME_FORMAT_NONE = 0
        const val TIME_FORMAT_SHORT = 1
        const val TIME_FORMAT_ISO = 2
        private const val MAX_STORED_LOG_ENTRIES = 1000
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, logText: String)
        fun onItemLongClick(view: View, position: Int, logText: String)
    }

    inner class logViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textLog: TextView = itemView.findViewById(R.id.textLog)
    }

    private var allEntries = Vector<LogItem>()
    private val currentLevelEntries = Vector<LogItem>()
    private lateinit var handler: Handler
    private var itemClickListener: OnItemClickListener? = null
    private val observers = Vector<RecyclerView.AdapterDataObserver>()

    private val timeFormat = -100
    private var logLevel = 3
    private val lockAutoScroll = false

    init {
        initLogBuffer()
        handler = Handler(this)
        AppLogManager.addLogListener(this)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    private fun initLogBuffer() {
        allEntries.clear()
        allEntries.addAll(AppLogManager.getlogbuffer().toList())
        initCurrentMessages()
    }

    private fun initCurrentMessages() {
        currentLevelEntries.clear()
        for (item in allEntries) {
            if (item.getLogLevel().getInt() <= logLevel) {
                currentLevelEntries.add(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): logViewHolder {
        val logView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_log_drawer, parent, false)
        logView.setOnTouchListener(this)
        return logViewHolder(logView)
    }

    @SuppressLint("RecyclerView")
    @Suppress("DEPRECATION")
    override fun onBindViewHolder(viewHolder: logViewHolder, position: Int) {
        val text: String

        try {
            val logItem = currentLevelEntries[position]
            val message = logItem.getString(context)
            val time = getTime(logItem, timeFormat)
            text = (if (time.isNotEmpty()) "[$time] " else "") + message
            viewHolder.textLog.text = Html.fromHtml(text)
        } catch (error: Exception) {
            AppLogManager.logException(error)
            return
        }

        viewHolder.textLog.setOnClickListener { view ->
            itemClickListener?.onItemClick(view, position, text)
        }

        viewHolder.textLog.setOnLongClickListener { view ->
            itemClickListener?.onItemLongClick(view, position, text)
            true
        }
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.registerAdapterDataObserver(observer)
        observers.add(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        observers.remove(observer)
    }

    override fun getItemCount(): Int = currentLevelEntries.size

    override fun getItemId(position: Int): Long =
        currentLevelEntries[position].hashCode().toLong()

    fun isEmpty(): Boolean = currentLevelEntries.isEmpty()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        // Original auto-scroll pause behavior remains disabled.
        return false
    }

    private fun getTime(item: LogItem, format: Int): String {
        if (format == TIME_FORMAT_NONE) {
            return ""
        }

        val date = Date(item.getLogtime())
        val formatter: java.text.DateFormat = when (format) {
            TIME_FORMAT_ISO ->
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            TIME_FORMAT_SHORT -> SimpleDateFormat("HH:mm")
            else -> DateFormat.getTimeFormat(context)
        }
        return formatter.format(date)
    }

    override fun handleMessage(message: Message): Boolean {
        when (message.what) {
            MESSAGE_NEWLOG -> {
                val logMessage = message.data.getParcelable<LogItem>("logmessage")
                    ?: return true

                if (addLogMessage(logMessage)) {
                    for (observer in observers) {
                        observer.onChanged()
                    }
                    if (!lockAutoScroll) {
                        scrollToLastPosition()
                    }
                }
            }

            MESSAGE_CLEARLOG -> {
                for (observer in observers) {
                    observer.onChanged()
                }
                initLogBuffer()
            }

            MESSAGE_NEWTS -> {
                for (observer in observers) {
                    observer.onChanged()
                }
            }

            MESSAGE_NEWLOGLEVEL -> {
                initCurrentMessages()
                for (observer in observers) {
                    observer.onChanged()
                }
            }
        }
        return true
    }

    private fun addLogMessage(logMessage: LogItem): Boolean {
        allEntries.add(logMessage)

        if (allEntries.size > MAX_STORED_LOG_ENTRIES) {
            val oldEntries = allEntries
            allEntries = Vector(oldEntries.size)
            for (index in 50 until oldEntries.size) {
                allEntries.add(oldEntries.elementAt(index))
            }
            initCurrentMessages()
            return true
        }

        if (logMessage.getLogLevel().getInt() <= logLevel) {
            currentLevelEntries.add(logMessage)
            return true
        }
        return false
    }

    fun getItem(position: Int): LogItem = currentLevelEntries[position]

    fun clearLog() {
        AppLogManager.clearLog()
    }

    fun scrollToLastPosition() {
        linearLayoutManager.scrollToPosition(linearLayoutManager.itemCount - 1)
    }

    fun setLogLevel(level: Int) {
        logLevel = level
    }

    override fun newLog(logMessage: LogItem) {
        val message = Message.obtain()
        message.what = MESSAGE_NEWLOG
        val bundle = Bundle()
        bundle.putParcelable("logmessage", logMessage)
        message.data = bundle
        handler.sendMessage(message)
    }

    override fun onClear() {
        handler.sendEmptyMessage(MESSAGE_CLEARLOG)
    }
}
