package com.najmi.sprint.core.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encryption and decryption of project secrets utilizing Tink's AEAD
 * backed by the Android Keystore.
 *
 * Explicitly avoids `EncryptedSharedPreferences` due to main-thread I/O and
 * keyset corruption issues on some OEMs.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val aead: Aead

    init {
        AeadConfig.register()

        // AndroidKeysetManager reads/writes the keyset to a private SharedPreferences file,
        // encrypting the keyset itself with a MasterKey residing in the Android Keystore.
        // This is the recommended Tink pattern.
        aead = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_PREF_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    /**
     * Encrypts a plaintext string into a ByteArray.
     */
    fun encrypt(plaintext: String): ByteArray {
        return aead.encrypt(plaintext.toByteArray(StandardCharsets.UTF_8), null)
    }

    /**
     * Decrypts a ByteArray back into the original plaintext string.
     */
    fun decrypt(ciphertext: ByteArray): String {
        val decrypted = aead.decrypt(ciphertext, null)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREF_FILE_NAME = "sprint_crypto_keys"
        private const val KEYSET_PREF_NAME = "sprint_keyset"
        private const val MASTER_KEY_URI = "android-keystore://sprint_master_key"
    }
}
