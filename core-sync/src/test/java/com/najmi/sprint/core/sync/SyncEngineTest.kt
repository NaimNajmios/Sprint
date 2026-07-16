package com.najmi.sprint.core.sync

import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.core.domain.repository.TaskRepository
import com.najmi.sprint.core.sync.client.SupabaseApiService
import com.najmi.sprint.core.sync.model.ContextDto
import com.najmi.sprint.core.sync.model.TaskDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test

class SyncEngineTest {

    private lateinit var api: SupabaseApiService
    private lateinit var contextRepository: ContextRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var retroRepository: RetroRepository
    private lateinit var syncEngine: SyncEngine

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        contextRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        retroRepository = mockk(relaxed = true)

        syncEngine = SyncEngine(
            api = api,
            contextRepository = contextRepository,
            taskRepository = taskRepository,
            sessionRepository = sessionRepository,
            retroRepository = retroRepository
        )
    }

    @Test
    fun `syncAll pushes local contexts and pulls remote contexts`() = runTest {
        // Arrange
        val localContext = Context("ctx1", "Work", "#FFFFFF", true)
        val remoteContextDto = ContextDto("ctx2", "Life", "#000000", true)

        every { contextRepository.observeAllContexts() } returns flowOf(listOf(localContext))
        coEvery { api.fetchContexts() } returns listOf(remoteContextDto)

        every { taskRepository.observeAllTasks() } returns flowOf(emptyList())
        every { sessionRepository.observeRecentSessions(any()) } returns flowOf(emptyList())
        every { retroRepository.observeRetros() } returns flowOf(emptyList())

        // Act
        syncEngine.syncAll()

        // Assert
        // Pushes local contexts to API
        coVerify { 
            api.upsertContexts(match { it.size == 1 && it[0].id == "ctx1" }) 
        }

        // Pulls remote contexts and saves them locally
        coVerify { 
            contextRepository.insertContext(match { it.id == "ctx2" && it.name == "Life" }) 
        }
    }

    @Test
    fun `syncAll pushes local tasks and pulls remote tasks`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val localTask = Task("task1", "ctx1", null, "Buy milk", TaskStatus.BACKLOG, null, now, now, "device1")
        val remoteTaskDto = TaskDto("task2", "ctx1", null, "Read book", "DONE", null, now, now, "device2")

        every { contextRepository.observeAllContexts() } returns flowOf(emptyList())
        every { taskRepository.observeAllTasks() } returns flowOf(listOf(localTask))
        coEvery { api.fetchTasks() } returns listOf(remoteTaskDto)

        every { sessionRepository.observeRecentSessions(any()) } returns flowOf(emptyList())
        every { retroRepository.observeRetros() } returns flowOf(emptyList())

        // Act
        syncEngine.syncAll()

        // Assert
        // Pushes local tasks to API
        coVerify { 
            api.upsertTasks(match { it.size == 1 && it[0].id == "task1" && it[0].title == "Buy milk" }) 
        }

        // Pulls remote tasks and saves them locally
        coVerify { 
            taskRepository.insertTask(match { it.id == "task2" && it.title == "Read book" && it.status == TaskStatus.DONE }) 
        }
    }
}
