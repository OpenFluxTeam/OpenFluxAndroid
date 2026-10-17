package io.openflux.android.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small secrets (a saved server password): AES-GCM with a key that lives in the Android Keystore and never leaves it;
 * only the encrypted value (IV + ciphertext) is written to the app's private preferences.
 */
class AndroidSecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)

    fun load(key: String): String? = runCatching {
        val blob = Base64.decode(prefs.getString(key, null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, blob, 0, IV_BYTES))
        String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()

    fun save(key: String, value: String): Boolean = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val blob = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(key, Base64.encodeToString(blob, Base64.NO_WRAP)).commit()
    }.getOrDefault(false)

    fun delete(key: String) {
        prefs.edit().remove(key).commit()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "openflux_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
