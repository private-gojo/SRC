package app.one.secondvpnlite.tethering

import android.os.Parcel
import android.os.Parcelable

class ProxyProperties private constructor(
    private val mHost: String?,
    private val mPort: Int,
    private val mExclusionList: String?,
    private val mParsedExclusionList: Array<String>?
) : Parcelable {

    fun getHost(): String? = mHost

    fun getPort(): Int = mPort

    fun getExclusionList(): String? = mExclusionList

    override fun toString(): String {
        val builder = StringBuilder()
        if (mHost != null) {
            builder.append("[").append(mHost).append("] ")
            builder.append(mPort)
            if (mExclusionList != null) {
                builder.append(" xl=").append(mExclusionList)
            }
        } else {
            builder.append("[ProxyProperties.mHost == null]")
        }
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ProxyProperties) return false
        if (mExclusionList != null && mExclusionList != other.getExclusionList()) return false
        if (mHost != null && other.getHost() != null && mHost != other.getHost()) return false
        if (mHost != null && other.mHost == null) return false
        if (mHost == null && other.mHost != null) return false
        return mPort == other.mPort
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int =
        (mHost?.hashCode() ?: 0) + (mExclusionList?.hashCode() ?: 0) + mPort

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (mHost != null) {
            dest.writeByte(1)
            dest.writeString(mHost)
            dest.writeInt(mPort)
        } else {
            dest.writeByte(0)
        }
        dest.writeString(mExclusionList)
        dest.writeStringArray(mParsedExclusionList)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ProxyProperties> =
            object : Parcelable.Creator<ProxyProperties> {
                override fun createFromParcel(parcel: Parcel): ProxyProperties {
                    var host: String? = null
                    var port = 0
                    if (parcel.readByte().toInt() == 1) {
                        host = parcel.readString()
                        port = parcel.readInt()
                    }
                    val exclusionList = parcel.readString()
                    // Preserve the original parcel reconstruction behavior.
                    return ProxyProperties(host, port, exclusionList, null)
                }

                override fun newArray(size: Int): Array<ProxyProperties?> = arrayOfNulls(size)
            }
    }
}
