package com.najmi.sprint.tracking

import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackingEngineTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var usageStatsTracker: UsageStatsTracker
    private lateinit var engine: TrackingEngine

    @Before
    fun setup() {
        sessionRepository = mockk(relaxed = true)
        usageStatsTracker = mockk(relaxed = true)
        engine = TrackingEngine(sessionRepository, usageStatsTracker)
    }

    @Test
    fun `pollAndUpdate ignores empty results from UsageStatsTracker`() = runBlocking {
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns null

        engine.pollAndUpdate()

        coVerify(exactly = 0) { sessionRepository.insertSession(any()) }
        coVerify(exactly = 0) { sessionRepository.updateSession(any()) }
    }

    @Test
    fun `pollAndUpdate creates new session on first app detection`() = runBlocking {
        val eventTime = System.currentTimeMillis()
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app1", eventTime)

        engine.pollAndUpdate()

        val sessionSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.insertSession(capture(sessionSlot)) }
        
        val captured = sessionSlot.captured
        assertEquals("com.test.app1", captured.rawLabel)
        assertNull(captured.endTime)
    }

    @Test
    fun `pollAndUpdate ignores duplicate sequential apps`() = runBlocking {
        val eventTime = System.currentTimeMillis()
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app1", eventTime)

        // Poll 1: new app
        engine.pollAndUpdate()
        // Poll 2: same app again
        engine.pollAndUpdate()

        // Should only insert once, and never update/close
        coVerify(exactly = 1) { sessionRepository.insertSession(any()) }
        coVerify(exactly = 0) { sessionRepository.updateSession(any()) }
    }

    @Test
    fun `pollAndUpdate closes old session when new app detected`() = runBlocking {
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 60_000 // 1 min later

        // First app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app1", time1)
        engine.pollAndUpdate()

        // Capture the inserted session so we can mock the repository's getSessionById response
        val sessionSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.insertSession(capture(sessionSlot)) }
        val firstSession = sessionSlot.captured
        
        coEvery { sessionRepository.getSessionById(firstSession.id) } returns firstSession

        // Second app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app2", time2)
        engine.pollAndUpdate()

        // Verify it was updated (closed)
        val updateSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.updateSession(capture(updateSlot)) }
        
        val updatedSession = updateSlot.captured
        assertEquals(firstSession.id, updatedSession.id)
        assertNotNull(updatedSession.endTime)
        assertEquals(time2, updatedSession.endTime?.toEpochMilliseconds())

        // Verify the second one was inserted
        coVerify(exactly = 2) { sessionRepository.insertSession(any()) }
    }
}
