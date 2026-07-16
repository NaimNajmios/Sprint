package com.najmi.sprint.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.najmi.sprint.core.data.local.SprintDatabase
import com.najmi.sprint.core.data.local.dao.ContextDao
import com.najmi.sprint.core.data.local.dao.ProjectDao
import com.najmi.sprint.core.data.local.dao.RetroDao
import com.najmi.sprint.core.data.local.dao.SessionDao
import com.najmi.sprint.core.data.local.dao.TaskDao
import com.najmi.sprint.core.data.local.entity.ContextEntity
import com.najmi.sprint.core.data.repository.RoomContextRepository
import com.najmi.sprint.core.data.repository.RoomProjectRepository
import com.najmi.sprint.core.data.repository.RoomRetroRepository
import com.najmi.sprint.core.data.repository.RoomSessionRepository
import com.najmi.sprint.core.data.repository.RoomTaskRepository
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.core.domain.repository.TaskRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import com.najmi.sprint.core.data.repository.GlobalContextManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideSprintDatabase(
        @ApplicationContext context: Context,
        provider: Provider<ContextDao>,
        applicationScope: CoroutineScope
    ): SprintDatabase {
        return Room.databaseBuilder(
            context,
            SprintDatabase::class.java,
            "sprint.db"
        )
        .addCallback(SprintDatabaseCallback(provider, applicationScope))
        .build()
    }

    @Provides
    fun provideContextDao(database: SprintDatabase): ContextDao = database.contextDao()

    @Provides
    fun provideProjectDao(database: SprintDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideSessionDao(database: SprintDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideTaskDao(database: SprintDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideRetroDao(database: SprintDatabase): RetroDao = database.retroDao()

    @Provides
    fun provideRuleDao(database: SprintDatabase): com.najmi.sprint.core.data.local.dao.RuleDao = database.ruleDao()
}

class SprintDatabaseCallback(
    private val contextDaoProvider: Provider<ContextDao>,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            val dao = contextDaoProvider.get()
            // Seed 4 default contexts
            dao.insertContext(ContextEntity(UUID.randomUUID().toString(), "Internship", "#FF5722", true))
            dao.insertContext(ContextEntity(UUID.randomUUID().toString(), "Coursework", "#2196F3", true))
            dao.insertContext(ContextEntity(UUID.randomUUID().toString(), "Side Projects", "#4CAF50", true))
            dao.insertContext(ContextEntity(UUID.randomUUID().toString(), "Life", "#9C27B0", true))
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContextRepository(
        impl: RoomContextRepository
    ): ContextRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: RoomProjectRepository
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: RoomSessionRepository
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: RoomTaskRepository
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindRetroRepository(
        impl: RoomRetroRepository
    ): RetroRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(
        impl: com.najmi.sprint.core.data.repository.RoomRuleRepository
    ): com.najmi.sprint.core.domain.repository.RuleRepository

    @Binds
    @Singleton
    abstract fun bindGlobalContextManager(
        impl: GlobalContextManagerImpl
    ): GlobalContextManager
}
