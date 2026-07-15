package com.najmi.sprint.core.data.repository

import androidx.room.Room
import app.cash.turbine.test
import com.najmi.sprint.core.data.local.SprintDatabase
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SessionRepositoryTest {

    private lateinit var database: SprintDatabase
    private lateinit var repository: RoomSessionRepository

    @Before
    fun setup() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, SprintDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomSessionRepository(database.sessionDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createAndReadSession() = runTest {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()
        val session = Session(
            id = id,
            deviceId = "device-1",
            source = SessionSource.APP_USAGE,
            rawLabel = "com.google.android.youtube",
            startTime = now
        )

        repository.insertSession(session)
        val retrieved = repository.getSessionById(id)

        assertNotNull(retrieved)
        assertEquals("com.google.android.youtube", retrieved?.rawLabel)
        assertEquals(now.toEpochMilliseconds(), retrieved?.startTime?.toEpochMilliseconds())
    }

    @Test
    fun observeSessionsForDate() = runTest {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        val session = Session(
            id = "1",
            deviceId = "device-1",
            rawLabel = "com.github.android",
            startTime = now
        )
        repository.insertSession(session)

        repository.observeSessionsForDate(today).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("com.github.android", list[0].rawLabel)
        }
    }
}
