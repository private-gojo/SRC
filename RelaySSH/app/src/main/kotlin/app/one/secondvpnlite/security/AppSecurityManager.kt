package app.one.secondvpnlite.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AppSecurityManager {
    @JvmField val k1 = String(byteArrayOf(87, 97, 115, 106, 100, 101, 105, 106, 115, 64, 47))
    @JvmField val k2 = String(byteArrayOf(-61, -121, 80, -61, -93, 111, 79, 102, 50, 51, 49))
    @JvmField val k3 = String(byteArrayOf(35, 36, 37, -62, -88, 38, 42, 40, 41, 95, 113))
    @JvmField val k4 = String(byteArrayOf(113, 117, 38, 105, 74, 111, 62))
    @JvmField val k5 = "ç"

    @JvmField val iv1 = String(byteArrayOf(52, 48, 48, 50, 57, 56, 50))
    @JvmField val iv2 = String(byteArrayOf(68, 79, 83, 84, 84, 65, 83))
    @JvmField val ivf = String(byteArrayOf(77, 85, 78, 100, 105, 64, -61, -121))

    @JvmField val a = "a"
    @JvmField val i = "b"

    @JvmStatic
    @Throws(Exception::class)
    fun encrypt(ivStr: String, keyStr: String, bytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(ivStr.toByteArray())
        val ivBytes = md.digest()

        val sha = MessageDigest.getInstance("SHA-256")
        sha.update(keyStr.toByteArray())
        val keyBytes = sha.digest()

        return encrypt(ivBytes, keyBytes, bytes)
    }

    @Throws(Exception::class)
    private fun encrypt(ivBytes: ByteArray, keyBytes: ByteArray, bytes: ByteArray): ByteArray {
        val ivSpec: AlgorithmParameterSpec = IvParameterSpec(ivBytes)
        val newKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec)
        return cipher.doFinal(bytes)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun decrypt(ivStr: String, keyStr: String, bytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(ivStr.toByteArray())
        val ivBytes = md.digest()

        val sha = MessageDigest.getInstance("SHA-256")
        sha.update(keyStr.toByteArray())
        val keyBytes = sha.digest()

        return decrypt(ivBytes, keyBytes, bytes)
    }

    @Throws(Exception::class)
    private fun decrypt(ivBytes: ByteArray, keyBytes: ByteArray, bytes: ByteArray): ByteArray {
        val ivSpec: AlgorithmParameterSpec = IvParameterSpec(ivBytes)
        val newKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec)
        return cipher.doFinal(bytes)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun encryptStrAndToBase64(ivStr: String, keyStr: String, enStr: String): String {
        val bytes = encrypt(keyStr, keyStr, enStr.toByteArray(StandardCharsets.UTF_8))
        return String(Base64.encode(bytes, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun decryptStrAndFromBase64(ivStr: String, keyStr: String, deStr: String): String {
        val bytes = decrypt(
            keyStr,
            keyStr,
            Base64.decode(deStr.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
        )
        return String(bytes, StandardCharsets.UTF_8)
    }
}
