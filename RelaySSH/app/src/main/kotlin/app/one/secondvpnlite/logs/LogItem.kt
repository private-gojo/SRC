package app.one.secondvpnlite.logs

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import app.one.secondvpnlite.R
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.FormatFlagsConversionMismatchException
import java.util.Locale
import java.util.UnknownFormatConversionException

class LogItem : Parcelable {
    private var mArgs: Array<out Any?>? = null
    private var mMessage: String? = null
    private var mResourceId: Int = 0
    private var mLevel: AppLogManager.LogLevel = AppLogManager.LogLevel.INFO
    private var logtime: Long = System.currentTimeMillis()
    private var mVerbosityLevel: Int = -1

    constructor(resId: Int, vararg args: Any?) {
        mResourceId = resId
        mArgs = args
    }

    constructor(logLevel: AppLogManager.LogLevel, verbosityLevel: Int, message: String) {
        mLevel = logLevel
        mMessage = message
        mVerbosityLevel = verbosityLevel
    }

    constructor(level: AppLogManager.LogLevel, resId: Int, vararg args: Any?) {
        mLevel = level
        mResourceId = resId
        mArgs = args
    }

    constructor(logLevel: AppLogManager.LogLevel, message: String) {
        mLevel = logLevel
        mMessage = message
    }

    constructor(logLevel: AppLogManager.LogLevel, resourceId: Int) {
        mResourceId = resourceId
        mLevel = logLevel
    }

    constructor(parcel: Parcel) {
        @Suppress("UNCHECKED_CAST")
        mArgs = parcel.readArray(Any::class.java.classLoader) as? Array<out Any?>
        mMessage = parcel.readString()
        mResourceId = parcel.readInt()
        mLevel = AppLogManager.LogLevel.getEnumByValue(parcel.readInt())
            ?: AppLogManager.LogLevel.INFO
        logtime = parcel.readLong()
    }

    override fun toString(): String = getString(null)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeArray(mArgs)
        dest.writeString(mMessage)
        dest.writeInt(mResourceId)
        dest.writeInt(mLevel.getInt())
        dest.writeLong(logtime)
    }

    fun getLogLevel(): AppLogManager.LogLevel = mLevel

    fun getLogtime(): Long = logtime

    fun getMessage(): String? = mMessage

    fun getString(context: Context?): String {
        try {
            if (mMessage != null) {
                return mMessage!!
            }
            if (context != null) {
                if (mResourceId == R.string.app_mobile_info) {
                    return getAppInfoString(context)
                }
                val args = mArgs
                return if (args == null) {
                    context.getString(mResourceId)
                } else {
                    context.getString(mResourceId, *args)
                }
            }

            var result = String.format(
                Locale.ENGLISH,
                "Log (no context) resid %d",
                mResourceId
            )
            if (mArgs != null) {
                result += join("|", mArgs!!)
            }
            return result
        } catch (error: UnknownFormatConversionException) {
            if (context != null) {
                throw UnknownFormatConversionException(
                    error.localizedMessage + getString(null)
                )
            }
            throw error
        } catch (error: FormatFlagsConversionMismatchException) {
            if (context != null) {
                throw FormatFlagsConversionMismatchException(
                    error.localizedMessage + getString(null),
                    error.conversion
                )
            }
            throw error
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun getAppInfoString(context: Context): String {
        context.packageManager
        val appSignature = ""
        var version = "Erro ao obter versão"
        try {
            @Suppress("DEPRECATION")
            @SuppressLint("PackageManagerGetSignatures")
            val raw = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures[0]
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(
                ByteArrayInputStream(raw.toByteArray())
            ) as X509Certificate
            val digest = MessageDigest.getInstance("SHA-1")
            val encoded = certificate.encoded
            digest.update(encoded)
            digest.digest()

            val packageInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            version = String.format(
                "%s Build %d",
                packageInfo.versionName,
                packageInfo.versionCode
            )
        } catch (_: PackageManager.NameNotFoundException) {
        } catch (_: CertificateException) {
        } catch (_: NoSuchAlgorithmException) {
        }
        return context.getString(R.string.app_mobile_info, version, appSignature)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LogItem> = object : Parcelable.Creator<LogItem> {
            override fun createFromParcel(parcel: Parcel): LogItem = LogItem(parcel)
            override fun newArray(size: Int): Array<LogItem?> = arrayOfNulls(size)
        }

        @JvmStatic
        fun join(delimiter: CharSequence, tokens: Array<out Any?>): String {
            val builder = StringBuilder()
            var firstTime = true
            for (token in tokens) {
                if (firstTime) {
                    firstTime = false
                } else {
                    builder.append(delimiter)
                }
                builder.append(token)
            }
            return builder.toString()
        }
    }
}
