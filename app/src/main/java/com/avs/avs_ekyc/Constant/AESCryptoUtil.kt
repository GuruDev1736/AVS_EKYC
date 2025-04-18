package com.avs.avs_ekyc.Constant

import android.util.Base64
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AESCryptoUtil {

    private const val encryptionKey = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val salt = byteArrayOf(
        0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d,
        0x65, 0x64, 0x76, 0x65, 0x64, 0x65, 0x76
    )

    private fun getSecretKey(): Pair<SecretKeySpec, IvParameterSpec> {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec = PBEKeySpec(encryptionKey.toCharArray(), salt, 1000, 384)
        val tmp = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(tmp.copyOfRange(0, 32), "AES")
        val ivSpec = IvParameterSpec(tmp.copyOfRange(32, 48))
        return Pair(secretKey, ivSpec)
    }

    fun encrypt(input: String): String {
        val (key, iv) = getSecretKey()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(input.toByteArray(Charsets.UTF_16LE))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedInput: String): String? {
        return try {
            val (key, iv) = getSecretKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decoded = Base64.decode(encryptedInput.replace(" ", "+"), Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_16LE)
        } catch (e: Exception) {
            null
        }
    }
}
