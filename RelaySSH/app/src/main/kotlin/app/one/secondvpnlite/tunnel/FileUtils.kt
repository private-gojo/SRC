package app.one.secondvpnlite.tunnel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.one.secondvpnlite.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Scanner

object FileUtils {
    private const val TAG = "FileUtils"

    @JvmStatic
    fun requestForPermissionExternalStorage(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                promptForPermissionsDialog(
                    activity,
                    activity.getString(R.string.error_request_permission),
                    DialogInterface.OnClickListener { _, _ ->
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            100
                        )
                    }
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
            }
        }
    }

    private fun promptForPermissionsDialog(
        context: Context,
        message: String,
        onClickListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.ok), onClickListener)
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }

    @JvmStatic
    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    @JvmStatic
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state ||
            Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copiarArquivo(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }
        input.close()
        output.close()
    }

    @JvmStatic
    fun readFromRaw(context: Context, resId: Int): String {
        val input = context.resources.openRawResource(resId)
        val scanner = Scanner(input, "UTF-8").useDelimiter("\\A")
        val result = StringBuilder()
        while (scanner.hasNext()) {
            result.append(scanner.next())
        }
        scanner.close()
        return result.toString()
    }

    @JvmStatic
    fun readTextFile(file: File): String? {
        val text = StringBuilder()
        return try {
            val reader = BufferedReader(FileReader(file))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                text.append(line).append("\n")
            }
            reader.close()
            text.toString()
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun saveTextFile(file: File, contents: String): Boolean {
        return try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val writer = FileWriter(file, false)
            writer.write(contents)
            writer.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
