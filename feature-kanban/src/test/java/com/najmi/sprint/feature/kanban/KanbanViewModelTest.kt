package com.najmi.sprint.feature.kanban

import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import com.najmi.sprint.core.domain.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KanbanViewModelTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var contextRepository: ContextRepository
    private lateinit var globalContextManager: GlobalContextManager
    private lateinit var viewModel: KanbanViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val contextsFlow = MutableStateFlow<List<Context>>(emptyList())
    private val selectedContextIdFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        taskRepository = mockk(relaxed = true)
        contextRepository = mockk(relaxed = true)
        globalContextManager = mockk(relaxed = true)

        every { taskRepository.observeAllTasks() } returns tasksFlow
        every { contextRepository.observeActiveContexts() } returns contextsFlow
        every { globalContextManager.selectedContextId } returns selectedContextIdFlow

        val testContext = Context("ctx-1", "Work", "#000000", true)
        contextsFlow.value = listOf(testContext)

        viewModel = KanbanViewModel(taskRepository, contextRepository, globalContextManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads correctly`() = runTest(testDispatcher) {
        assertTrue(viewModel.state.value.isLoading)

        backgroundScope.launch { viewModel.state.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("ctx-1", state.contexts.first().id)
    }

    @Test
    fun `tasks are grouped by status`() = runTest(testDispatcher) {
        val now = Clock.System.now()
        val task1 = Task("1", "ctx-1", null, "Task 1", TaskStatus.BACKLOG, null, now, now, "dev")
        val task2 = Task("2", "ctx-1", null, "Task 2", TaskStatus.IN_PROGRESS, null, now, now, "dev")

        tasksFlow.value = listOf(task1, task2)

        backgroundScope.launch { viewModel.state.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.tasksByStatus[TaskStatus.BACKLOG]?.size)
        assertEquals(1, state.tasksByStatus[TaskStatus.IN_PROGRESS]?.size)
        assertEquals(0, state.tasksByStatus[TaskStatus.DONE]?.size)
    }

    @Test
    fun `tasks are filtered by global context`() = runTest(testDispatcher) {
        val now = Clock.System.now()
        val workTask = Task("1", "ctx-work", null, "Work Task", TaskStatus.BACKLOG, null, now, now, "dev")
        val lifeTask = Task("2", "ctx-life", null, "Life Task", TaskStatus.BACKLOG, null, now, now, "dev")

        tasksFlow.value = listOf(workTask, lifeTask)
        selectedContextIdFlow.value = "ctx-work"

        backgroundScope.launch { viewModel.state.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("ctx-work", state.selectedContextId)
        assertEquals(1, state.tasksByStatus[TaskStatus.BACKLOG]?.size)
        assertEquals("Work Task", state.tasksByStatus[TaskStatus.BACKLOG]?.first()?.title)
    }

    @Test
    fun `addTask calls repository`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addTask("New Task")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(timeout = 1000) { taskRepository.insertTask(any()) }
    }

    @Test
    fun `moveTask calls repository`() = runTest(testDispatcher) {
        viewModel.moveTask("task-123", TaskStatus.DONE)

        coVerify(timeout = 1000) { taskRepository.updateTaskStatus("task-123", TaskStatus.DONE) }
    }
}
