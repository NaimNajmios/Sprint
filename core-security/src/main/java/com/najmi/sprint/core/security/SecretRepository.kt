package com.najmi.sprint.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.najmi.sprint.core.domain.model.Secret
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secretDataStore: DataStore<Preferences> by preferencesDataStore(name = "secrets")

@Singleton
class SecretRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val secretsKey = stringPreferencesKey("project_secrets")

    private val secretsFlow: Flow<List<Secret>> = context.secretDataStore.data.map { prefs ->
        val jsonString = prefs[secretsKey]
        if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                Json.decodeFromString<List<Secret>>(jsonString)
            } catch (e: Exception) {
                // If corrupted, fallback to empty
                emptyList()
            }
        }
    }

    /**
     * Observes all secrets for a specific project.
     * Returns ONLY the metadata and encrypted payloads.
     */
    fun observeSecretsByProject(projectId: String): Flow<List<Secret>> {
        return secretsFlow.map { secrets ->
            secrets.filter { it.projectId == projectId }
        }
    }

    /**
     * Inserts or updates a secret. The plaintext value is encrypted before storage.
     */
    suspend fun insertSecret(secretId: String, projectId: String, label: String, plaintextValue: String) {
        val encrypted = cryptoManager.encrypt(plaintextValue)
        val newSecret = Secret(
            id = secretId,
            projectId = projectId,
            label = label,
            encryptedValue = encrypted,
            createdAt = Clock.System.now()
        )

        context.secretDataStore.edit { prefs ->
            val currentJson = prefs[secretsKey]
            val currentList = if (currentJson.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    Json.decodeFromString<List<Secret>>(currentJson)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // Remove existing with same ID and append new
            val updatedList = currentList.filter { it.id != secretId } + newSecret
            prefs[secretsKey] = Json.encodeToString(updatedList)
        }
    }

    /**
     * Deletes a secret by ID.
     */
    suspend fun deleteSecret(secretId: String) {
        context.secretDataStore.edit { prefs ->
            val currentJson = prefs[secretsKey] ?: return@edit
            val currentList = try {
                Json.decodeFromString<List<Secret>>(currentJson)
            } catch (e: Exception) {
                return@edit
            }

            val updatedList = currentList.filter { it.id != secretId }
            prefs[secretsKey] = Json.encodeToString(updatedList)
        }
    }

    /**
     * The ONLY method that decrypts a secret.
     *
     * // NEVER LOG the return value of this method.
     */
    suspend fun revealSecret(secretId: String): String? {
        var encryptedVal: ByteArray? = null
        
        // Update last accessed time while finding the secret
        context.secretDataStore.edit { prefs ->
            val currentJson = prefs[secretsKey] ?: return@edit
            val currentList = try {
                Json.decodeFromString<List<Secret>>(currentJson)
            } catch (e: Exception) {
                return@edit
            }

            val secretIndex = currentList.indexOfFirst { it.id == secretId }
            if (secretIndex != -1) {
                val secret = currentList[secretIndex]
                encryptedVal = secret.encryptedValue
                
                // Update accessed time
                val updatedList = currentList.toMutableList()
                updatedList[secretIndex] = secret.copy(lastAccessedAt = Clock.System.now())
                prefs[secretsKey] = Json.encodeToString(updatedList)
            }
        }

        return encryptedVal?.let { cryptoManager.decrypt(it) }
    }
}
