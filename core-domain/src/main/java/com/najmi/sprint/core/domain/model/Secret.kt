package com.najmi.sprint.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents an encrypted credential stored securely per-project.
 *
 * Designed to never be logged or synced cross-device. The actual [encryptedValue]
 * relies on local Tink keysets via Android Keystore.
 */
@Serializable
data class Secret(
    val id: String,
    val projectId: String,
    val label: String,              // e.g. "GROQ_API_KEY", "GITHUB_PAT"
    val encryptedValue: ByteArray,
    val createdAt: Instant,
    val lastAccessedAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Secret

        if (id != other.id) return false
        if (projectId != other.projectId) return false
        if (label != other.label) return false
        if (!encryptedValue.contentEquals(other.encryptedValue)) return false
        if (createdAt != other.createdAt) return false
        if (lastAccessedAt != other.lastAccessedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + projectId.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + encryptedValue.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (lastAccessedAt?.hashCode() ?: 0)
        return result
    }
}
