package com.najmi.sprint.feature.retro

import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class RetroViewModelTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var contextRepository: ContextRepository
    private lateinit var retroRepository: RetroRepository
    private lateinit var globalContextManager: GlobalContextManager
    private lateinit var viewModel: RetroViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val contextsFlow = MutableStateFlow<List<Context>>(emptyList())
    private val retrosFlow = MutableStateFlow<List<RetroEntry>>(emptyList())

    private val workContext = Context("ctx-work", "Work", "#FF5722", true)
    private val lifeContext = Context("ctx-life", "Life", "#9C27B0", true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        sessionRepository = mockk(relaxed = true)
        contextRepository = mockk(relaxed = true)
        retroRepository = mockk(relaxed = true)
        globalContextManager = mockk(relaxed = true)

        every { contextRepository.observeActiveContexts() } returns contextsFlow
        every { retroRepository.observeRetros() } returns retrosFlow
        every { globalContextManager.selectedContextId } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty sessions produce zero totals`() = runTest(testDispatcher) {
        coEvery { sessionRepository.getSessionsBetween(any(), any()) } returns emptyList()
        contextsFlow.value = listOf(workContext, lifeContext)

        viewModel = RetroViewModel(sessionRepository, contextRepository, retroRepository, globalContextManager)

        // Give the combine + collect time to run
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(0L, state.weeklyTotalMinutes)
        assertEquals(emptyMap<String, Long>(), state.weeklyPerContext)
    }

    @Test
    fun `sessions are aggregated per context correctly`() = runTest(testDispatcher) {
        val now = Clock.System.now()
        val sessions = listOf(
            makeSession("s1", "com.work.app", "ctx-work", now.minus(2.hours), now.minus(1.hours)),
            makeSession("s2", "com.life.app", "ctx-life", now.minus(3.hours), now.minus(2.hours.plus(30.minutes)))
        )

        coEvery { sessionRepository.getSessionsBetween(any(), any()) } returns sessions
        contextsFlow.value = listOf(workContext, lifeContext)

        viewModel = RetroViewModel(sessionRepository, contextRepository, retroRepository, globalContextManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(90L, state.weeklyTotalMinutes) // 60 + 30
        assertEquals(60L, state.weeklyPerContext["ctx-work"])
        assertEquals(30L, state.weeklyPerContext["ctx-life"])
    }

    @Test
    fun `top app is identified correctly`() = runTest(testDispatcher) {
        val now = Clock.System.now()
        val sessions = listOf(
            makeSession("s1", "com.android.chrome", "ctx-work", now.minus(3.hours), now.minus(1.hours)),
            makeSession("s2", "com.whatsapp", "ctx-life", now.minus(4.hours), now.minus(3.hours.plus(30.minutes)))
        )

        coEvery { sessionRepository.getSessionsBetween(any(), any()) } returns sessions
        contextsFlow.value = listOf(workContext, lifeContext)

        viewModel = RetroViewModel(sessionRepository, contextRepository, retroRepository, globalContextManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("com.android.chrome", state.topApp) // 120 min > 30 min
        assertEquals(120L, state.topAppMinutes)
    }

    @Test
    fun `daily breakdown has 7 entries`() = runTest(testDispatcher) {
        coEvery { sessionRepository.getSessionsBetween(any(), any()) } returns emptyList()
        contextsFlow.value = listOf(workContext)

        viewModel = RetroViewModel(sessionRepository, contextRepository, retroRepository, globalContextManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(7, state.dailyBreakdown.size)
        assertEquals("Mon", state.dailyBreakdown[0].dayLabel)
        assertEquals("Sun", state.dailyBreakdown[6].dayLabel)
    }

    @Test
    fun `retro entries are passed through to state`() = runTest(testDispatcher) {
        val retro = RetroEntry(
            id = "r1",
            weekOf = kotlinx.datetime.LocalDate(2026, 7, 7),
            summaryText = "Great week!",
            generatedByModel = "llama3-70b-8192",
            promptVersion = "v1.0",
            criticApproved = true
        )

        coEvery { sessionRepository.getSessionsBetween(any(), any()) } returns emptyList()
        contextsFlow.value = listOf(workContext)
        retrosFlow.value = listOf(retro)

        viewModel = RetroViewModel(sessionRepository, contextRepository, retroRepository, globalContextManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.retros.size)
        assertEquals("Great week!", state.retros.first().summaryText)
    }

    private fun makeSession(
        id: String,
        rawLabel: String,
        contextId: String,
        start: Instant,
        end: Instant
    ): Session = Session(
        id = id,
        deviceId = "test",
        source = SessionSource.APP_USAGE,
        rawLabel = rawLabel,
        startTime = start,
        endTime = end,
        contextId = contextId
    )
}
