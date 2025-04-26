package com.avs.avs_ekyc.Constant

import android.util.Base64
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AESCryptoUtil {

    private const val encryptionKey = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val salt = byteArrayOf(
        0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d, 0x65, 0x64,
        0x76, 0x65, 0x64, 0x65, 0x76
    )

    fun encrypt(input: String): String {
        val clearBytes = input.toByteArray(Charsets.UTF_16LE) // Unicode equivalent
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec: KeySpec = PBEKeySpec(encryptionKey.toCharArray(), salt, 1000, 384)
        val tmp = factory.generateSecret(spec)
        val secret = SecretKeySpec(tmp.encoded.copyOfRange(0, 32), "AES")
        val iv = IvParameterSpec(tmp.encoded.copyOfRange(32, 48))
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secret, iv)
        val encryptedBytes = cipher.doFinal(clearBytes)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun decrypt(encrypted: String): String? {
        return try {
            val encryptedText = encrypted.replace(" ", "+")
            val cipherBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec: KeySpec = PBEKeySpec(encryptionKey.toCharArray(), salt, 1000, 384)
            val tmp = factory.generateSecret(spec)
            val secret = SecretKeySpec(tmp.encoded.copyOfRange(0, 32), "AES")
            val iv = IvParameterSpec(tmp.encoded.copyOfRange(32, 48))
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, secret, iv)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_16LE)
        } catch (e: Exception) {
            null
        }
    }
}
