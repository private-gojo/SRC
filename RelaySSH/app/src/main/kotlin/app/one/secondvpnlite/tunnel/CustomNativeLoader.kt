package app.one.secondvpnlite.tunnel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object CustomNativeLoader {
    private const val TAG = "CNL"

    private fun loadFromZip(
        context: Context,
        libName: String,
        destination: File,
        architecture: String
    ): Boolean {
        var zipFile: ZipFile? = null
        var stream: InputStream? = null
        try {
            zipFile = ZipFile(context.applicationInfo.sourceDir)
            var entry: ZipEntry? =
                zipFile.getEntry("lib/$architecture/$libName.so")
            if (entry == null) {
                entry = zipFile.getEntry("jni/$architecture/$libName.so")
                if (entry == null) {
                    throw Exception("Unable to find file in apk:lib/$architecture/$libName")
                }
            }

            stream = zipFile.getInputStream(entry)
            val output: OutputStream = FileOutputStream(destination)
            val buffer = ByteArray(4096)
            var length: Int
            while (stream.read(buffer).also { length = it } > 0) {
                Thread.yield()
                output.write(buffer, 0, length)
            }
            output.close()

            destination.setReadable(true, false)
            destination.setExecutable(true, false)
            destination.setWritable(true)
            return true
        } catch (error: Exception) {
            Log.e(TAG, error.message ?: "")
        } finally {
            try {
                stream?.close()
            } catch (error: Exception) {
                Log.e(TAG, error.message ?: "")
            }
            try {
                zipFile?.close()
            } catch (error: Exception) {
                Log.e(TAG, error.message ?: "")
            }
        }
        return false
    }

    @JvmStatic
    fun loadNativeBinary(context: Context, libName: String, destination: File): File? {
        try {
            val nativeBinary = File(getNativeLibraryDir(context), "$libName.so")
            if (nativeBinary.exists()) {
                if (nativeBinary.canExecute()) return nativeBinary
                setExecutable(nativeBinary)
                if (nativeBinary.canExecute()) return nativeBinary
            } else if (destination.exists()) {
                if (destination.canExecute()) return destination
                setExecutable(destination)
                if (destination.canExecute()) return destination
            }

            val architectures = ArrayList<String>()
            if (Build.VERSION.SDK_INT >= 21) {
                val supportedAbis = Build.SUPPORTED_ABIS
                if (supportedAbis != null) {
                    for (abi in supportedAbis) {
                        if (!TextUtils.isEmpty(abi)) {
                            architectures.add(abi)
                        }
                    }
                }
            } else {
                val supportedAbis = arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
                for (abi in supportedAbis) {
                    if (!TextUtils.isEmpty(abi)) {
                        architectures.add(abi)
                    }
                }
            }

            for (abi in architectures) {
                val javaArchitecture = System.getProperty("os.arch")
                val folder = if (javaArchitecture != null && javaArchitecture.contains("686")) {
                    "x86"
                } else {
                    abi
                }

                if (loadFromZip(context, libName, destination, folder)) {
                    return destination
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, error.message ?: "", error)
        }
        return null
    }

    private fun setExecutable(file: File) {
        file.setReadable(true)
        file.setExecutable(true)
        file.setWritable(false)
        file.setWritable(true, true)
    }

    private fun getNativeLibraryDir(context: Context): String {
        val appInfo: ApplicationInfo = context.applicationInfo
        return appInfo.nativeLibraryDir
    }
}
