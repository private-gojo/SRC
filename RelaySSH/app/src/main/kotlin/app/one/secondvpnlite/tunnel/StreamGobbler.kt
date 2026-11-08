package app.one.secondvpnlite.tunnel

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class StreamGobbler : Thread {
    private val reader: BufferedReader
    private var writer: MutableList<String>? = null
    private var listener: OnLineListener? = null

    constructor(inputStream: InputStream, outputList: MutableList<String>) : super() {
        reader = BufferedReader(InputStreamReader(inputStream))
        writer = outputList
    }

    constructor(inputStream: InputStream, onLineListener: OnLineListener) : super() {
        reader = BufferedReader(InputStreamReader(inputStream))
        listener = onLineListener
    }

    override fun run() {
        while (true) {
            try {
                val line = reader.readLine()
                if (line != null) {
                    writer?.add(line)
                    listener?.onLine(line)
                    continue
                }
            } catch (_: IOException) {
                // Preserve the original close-on-read-error behavior.
            }

            try {
                reader.close()
            } catch (_: IOException) {
                // Stream is already unavailable.
            }
            return
        }
    }

    interface OnLineListener {
        fun onLine(line: String)
    }
}
