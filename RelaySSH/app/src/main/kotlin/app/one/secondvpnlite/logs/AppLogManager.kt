package app.one.secondvpnlite.logs

import app.one.secondvpnlite.R
import java.io.PrintWriter
import java.io.StringWriter
import java.util.LinkedList
import java.util.Vector

object AppLogManager {
    private val logbuffer = LinkedList<LogItem>()
    private val logListener = Vector<LogListener>()

    const val MAXLOGENTRIES = 1000

    enum class LogLevel(private val mValue: Int) {
        INFO(2),
        ERROR(-2),
        WARNING(1),
        VERBOSE(3),
        DEBUG(4);

        fun getInt(): Int = mValue

        companion object {
            @JvmStatic
            fun getEnumByValue(value: Int): LogLevel? = when (value) {
                2 -> INFO
                -2 -> ERROR
                1 -> WARNING
                3 -> VERBOSE
                4 -> DEBUG
                else -> null
            }
        }
    }

    interface LogListener {
        fun newLog(logItem: LogItem)
        fun onClear()
    }

    init {
        addLogrmation()
    }

    @JvmStatic
    @Synchronized
    fun clearLog() {
        logbuffer.clear()
        addLogrmation()
        for (listener in logListener) {
            listener.onClear()
        }
    }

    @JvmStatic
    @Synchronized
    fun getlogbuffer(): Array<LogItem> = logbuffer.toTypedArray()

    private fun addLogrmation() {
        addLog(R.string.app_mobile_info, "")
    }

    @JvmStatic
    @Synchronized
    fun addLogListener(listener: LogListener) {
        if (!logListener.contains(listener)) {
            logListener.add(listener)
        }
    }

    private fun newLogItem(logItem: LogItem, cachedLine: Boolean = false) {
        synchronized(this) {
            if (cachedLine) {
                logbuffer.addFirst(logItem)
            } else {
                logbuffer.addLast(logItem)
            }

            if (logbuffer.size > MAXLOGENTRIES + MAXLOGENTRIES / 2) {
                while (logbuffer.size > MAXLOGENTRIES) {
                    logbuffer.removeFirst()
                }
            }

            for (listener in logListener) {
                listener.newLog(logItem)
            }
        }
    }

    @JvmStatic
    fun logException(context: String?, exception: Exception) {
        logException(LogLevel.ERROR, context, exception)
    }

    @JvmStatic
    fun logException(level: LogLevel, context: String?, exception: Exception) {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        val item = if (context != null) {
            LogItem(level, String.format("%s: %s, %s", context, exception.message, writer.toString()))
        } else {
            LogItem(level, String.format("Erro: %s, %s", exception.message, writer.toString()))
        }
        newLogItem(item)
    }

    @JvmStatic
    fun logException(exception: Exception) {
        logException(LogLevel.ERROR, null, exception)
    }

    @JvmStatic
    fun addLog(message: String) {
        newLogItem(LogItem(LogLevel.INFO, message))
    }

    @JvmStatic
    fun logDebug(message: String) {
        newLogItem(LogItem(LogLevel.DEBUG, message))
    }

    @JvmStatic
    fun addLog(resourceId: Int, vararg args: Any?) {
        newLogItem(LogItem(LogLevel.INFO, resourceId, *args))
    }

    @JvmStatic
    fun logDebug(resourceId: Int, vararg args: Any?) {
        newLogItem(LogItem(LogLevel.DEBUG, resourceId, *args))
    }

    @JvmStatic
    fun logError(message: String) {
        newLogItem(LogItem(LogLevel.ERROR, message))
    }

    @JvmStatic
    fun logWarning(resourceId: Int, vararg args: Any?) {
        newLogItem(LogItem(LogLevel.WARNING, resourceId, *args))
    }

    @JvmStatic
    fun logWarning(message: String) {
        newLogItem(LogItem(LogLevel.WARNING, message))
    }

    @JvmStatic
    fun logError(resourceId: Int) {
        newLogItem(LogItem(LogLevel.ERROR, resourceId))
    }

    @JvmStatic
    fun logError(resourceId: Int, vararg args: Any?) {
        newLogItem(LogItem(LogLevel.ERROR, resourceId, *args))
    }
}
