package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object SecureKeystoreHelper {
    private const val KEY_ALIAS = "vvf_file_manager_vault_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): java.security.Key {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) ?: throw IllegalStateException("Key not found in Android Keystore")
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv ?: ByteArray(12)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted bytes
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedTextBase64: String): String {
        if (encryptedTextBase64.isBlank()) return ""
        return try {
            val combined = Base64.decode(encryptedTextBase64, Base64.NO_WRAP)
            val ivSize = 12 // AES GCM standard iv size
            if (combined.size <= ivSize) return ""
            
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            val encryptedBytes = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, ivSize, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
