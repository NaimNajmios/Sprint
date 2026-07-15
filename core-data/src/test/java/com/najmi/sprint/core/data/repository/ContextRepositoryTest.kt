package com.najmi.sprint.core.data.repository

import android.content.Context
import androidx.room.Room
import app.cash.turbine.test
import com.najmi.sprint.core.data.local.SprintDatabase
import com.najmi.sprint.core.domain.model.Context as DomainContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContextRepositoryTest {

    private lateinit var database: SprintDatabase
    private lateinit var repository: RoomContextRepository

    @Before
    fun setup() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, SprintDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomContextRepository(database.contextDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetContext() = runTest {
        val id = UUID.randomUUID().toString()
        val context = DomainContext(id, "Test Context", "#FFFFFF", true)

        repository.insertContext(context)
        
        val retrieved = repository.getContextById(id)
        assertEquals(context, retrieved)
    }

    @Test
    fun observeActiveContexts_filtersInactive() = runTest {
        val active1 = DomainContext("1", "Active 1", "#FFFFFF", true)
        val inactive = DomainContext("2", "Inactive", "#000000", false)
        val active2 = DomainContext("3", "Active 2", "#CCCCCC", true)

        repository.insertContext(active1)
        repository.insertContext(inactive)
        repository.insertContext(active2)

        repository.observeActiveContexts().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals("Active 1", list[0].name)
            assertEquals("Active 2", list[1].name)
        }
    }

    @Test
    fun softDeleteContext_hidesFromActive() = runTest {
        val ctx = DomainContext("1", "To Delete", "#FFFFFF", true)
        repository.insertContext(ctx)

        repository.observeActiveContexts().test {
            assertEquals(1, awaitItem().size)
            repository.softDeleteContext("1")
            assertEquals(0, awaitItem().size)
        }
    }
}
