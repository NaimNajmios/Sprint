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
import org.junit.After
import org.junit.Test
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.every
import com.najmi.sprint.core.domain.logger.AppLogger

class TrackingEngineTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var ruleRepository: com.najmi.sprint.core.domain.repository.RuleRepository
    private lateinit var usageStatsTracker: UsageStatsTracker
    private lateinit var mockContext: android.content.Context
    private lateinit var engine: TrackingEngine

    @Before
    fun setup() {
        mockkObject(AppLogger)
        every { AppLogger.d(any(), any()) } returns Unit
        every { AppLogger.e(any(), any(), any()) } returns Unit
        every { AppLogger.i(any(), any()) } returns Unit
        every { AppLogger.w(any(), any(), any()) } returns Unit

        sessionRepository = mockk(relaxed = true)
        ruleRepository = mockk(relaxed = true)
        usageStatsTracker = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        engine = TrackingEngine(sessionRepository, ruleRepository, usageStatsTracker, mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
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

    @Test
    fun `pollAndUpdate debounces sessions under 30 seconds`() = runBlocking {
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 20_000 // 20 seconds later (under 30s debounce threshold)

        // First app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app1", time1)
        engine.pollAndUpdate()

        val sessionSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.insertSession(capture(sessionSlot)) }
        
        // Mock getSessionById for when we close it
        val firstSession = sessionSlot.captured.copy(endTime = kotlinx.datetime.Instant.fromEpochMilliseconds(time2))
        coEvery { sessionRepository.getSessionById(firstSession.id) } returns firstSession

        // Second app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app2", time2)
        engine.pollAndUpdate()

        // Verify it was deleted due to debounce
        coVerify(exactly = 1) { sessionRepository.deleteSession(firstSession.id) }
    }

    @Test
    fun `pollAndUpdate merges session when switching back within 2 minutes`() = runBlocking {
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 60_000 // 1 minute later (inside 2 min merge threshold)

        // Setup the "last closed session" which was com.test.app1
        val previousApp1Session = Session(
            id = "old-session",
            deviceId = "local-device",
            source = com.najmi.sprint.core.domain.model.SessionSource.APP_USAGE,
            rawLabel = "com.test.app1",
            startTime = kotlinx.datetime.Instant.fromEpochMilliseconds(time1 - 30_000), // opened 30s ago
            endTime = kotlinx.datetime.Instant.fromEpochMilliseconds(time1), // closed at time1
            contextId = null,
            projectId = null,
            classificationConfidence = null,
            isManuallyCorrected = false
        )
        
        coEvery { sessionRepository.getLastClosedSession() } returns previousApp1Session

        // Detect com.test.app1 again at time2
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.app1", time2)
        engine.pollAndUpdate()

        // It should update (reopen) the old session rather than inserting a new one
        val updateSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.updateSession(capture(updateSlot)) }
        coVerify(exactly = 0) { sessionRepository.insertSession(any()) }
        
        val reopenedSession = updateSlot.captured
        assertEquals("old-session", reopenedSession.id)
        assertNull(reopenedSession.endTime) // endTime cleared to reopen it
    }

    @Test
    fun `pollAndUpdate ignores system packages completely`() = runBlocking {
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 5_000

        // Start tracking a real app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.game", time1)
        engine.pollAndUpdate()

        val sessionSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.insertSession(capture(sessionSlot)) }
        val gameSession = sessionSlot.captured

        // System UI blip appears — should be completely ignored
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.android.systemui", time2)
        engine.pollAndUpdate()

        // No close, no insert, no update — the game session is undisturbed
        coVerify(exactly = 0) { sessionRepository.updateSession(any()) }
        coVerify(exactly = 1) { sessionRepository.insertSession(any()) } // Still only the game
    }

    @Test
    fun `ignored package does not break merge chain`() = runBlocking {
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 2_000
        val time3 = time1 + 60_000

        // Start tracking a real app
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.game", time1)
        engine.pollAndUpdate()

        val sessionSlot = slot<Session>()
        coVerify(exactly = 1) { sessionRepository.insertSession(capture(sessionSlot)) }

        // System UI blip — should be ignored, game session stays active
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.android.launcher3", time2)
        engine.pollAndUpdate()

        // Same game detected again — should remain on the same session, not try to merge
        coEvery { usageStatsTracker.pollRecentForegroundApp() } returns UsageStatsTracker.ForegroundEvent("com.test.game", time3)
        engine.pollAndUpdate()

        // Game was never closed, so detecting it again is a no-op (same package check)
        coVerify(exactly = 0) { sessionRepository.updateSession(any()) }
        coVerify(exactly = 1) { sessionRepository.insertSession(any()) }
    }
}
