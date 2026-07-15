package com.najmi.sprint.core.domain

import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.FeatureFlags
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for core domain models.
 * Verifies data class construction, defaults, and enum coverage.
 */
class DomainModelTest {

    @Test
    fun `context default values are correct`() {
        val context = Context(
            id = UUID.randomUUID().toString(),
            name = "Internship",
            colorHex = "#4A90D9"
        )
        assertTrue(context.isActive)
    }

    @Test
    fun `context soft delete sets isActive to false`() {
        val context = Context(
            id = UUID.randomUUID().toString(),
            name = "Old Context",
            colorHex = "#FF0000",
            isActive = false
        )
        assertFalse(context.isActive)
    }

    @Test
    fun `task default status is BACKLOG`() {
        val now = Clock.System.now()
        val task = Task(
            id = UUID.randomUUID().toString(),
            contextId = "ctx-1",
            title = "Set up CI",
            createdAt = now,
            updatedAt = now,
            deviceId = "device-1"
        )
        assertEquals(TaskStatus.BACKLOG, task.status)
        assertNull(task.projectId)
        assertNull(task.estimatePoints)
    }

    @Test
    fun `task status transitions cover all states`() {
        val allStatuses = TaskStatus.entries
        assertEquals(4, allStatuses.size)
        assertTrue(allStatuses.contains(TaskStatus.BACKLOG))
        assertTrue(allStatuses.contains(TaskStatus.IN_PROGRESS))
        assertTrue(allStatuses.contains(TaskStatus.REVIEW))
        assertTrue(allStatuses.contains(TaskStatus.DONE))
    }

    @Test
    fun `session defaults are correct`() {
        val now = Clock.System.now()
        val session = Session(
            id = UUID.randomUUID().toString(),
            deviceId = "device-1",
            rawLabel = "com.android.chrome",
            startTime = now
        )
        assertEquals(SessionSource.APP_USAGE, session.source)
        assertNull(session.endTime)
        assertNull(session.contextId)
        assertNull(session.classificationConfidence)
        assertFalse(session.isManuallyCorrected)
    }

    @Test
    fun `session source enum covers all planned sources`() {
        val allSources = SessionSource.entries
        assertEquals(5, allSources.size)
    }

    @Test
    fun `feature flags defaults are sensible for MVP`() {
        assertTrue(FeatureFlags.APP_TRACKING)
        assertFalse(FeatureFlags.AI_CLASSIFICATION)
        assertFalse(FeatureFlags.SYNC)
        assertFalse(FeatureFlags.PHYSICAL_TRACKING)
        assertFalse(FeatureFlags.DESKTOP_TRACKING)
    }
}
