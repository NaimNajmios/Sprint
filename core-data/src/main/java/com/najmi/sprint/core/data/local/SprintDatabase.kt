package com.najmi.sprint.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.najmi.sprint.core.data.local.dao.ContextDao
import com.najmi.sprint.core.data.local.dao.ProjectDao
import com.najmi.sprint.core.data.local.dao.RetroDao
import com.najmi.sprint.core.data.local.dao.SessionDao
import com.najmi.sprint.core.data.local.dao.TaskDao
import com.najmi.sprint.core.data.local.entity.ContextEntity
import com.najmi.sprint.core.data.local.entity.ProjectEntity
import com.najmi.sprint.core.data.local.entity.RetroEntryEntity
import com.najmi.sprint.core.data.local.entity.SessionEntity
import com.najmi.sprint.core.data.local.entity.TaskEntity

@Database(
    entities = [
        ContextEntity::class,
        ProjectEntity::class,
        SessionEntity::class,
        TaskEntity::class,
        RetroEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SprintDatabase : RoomDatabase() {
    abstract fun contextDao(): ContextDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun retroDao(): RetroDao
}
