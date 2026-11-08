package app.one.secondvpnlite.tethering

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SocketConnect @Throws(IOException::class) constructor(fromSocket: Socket, toSocket: Socket) : Thread() {
    private val from: InputStream = fromSocket.getInputStream()
    private val to: OutputStream = toSocket.getOutputStream()

    init {
        start()
    }

    override fun run() {
        val buffer = ByteArray(512)
        try {
            while (true) {
                val read = from.read(buffer)
                if (read < 0) break
                to.write(buffer, 0, read)
            }
            from.close()
            to.close()
        } catch (io: IOException) {
            io.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun connect(first: Socket, second: Socket) {
            try {
                val firstToSecond = SocketConnect(first, second)
                val secondToFirst = SocketConnect(second, first)
                try {
                    firstToSecond.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                try {
                    secondToFirst.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
