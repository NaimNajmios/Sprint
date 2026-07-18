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
        RetroEntryEntity::class,
        com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SprintDatabase : RoomDatabase() {
    
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE classification_rules ADD COLUMN isIgnored INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    abstract fun contextDao(): ContextDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun retroDao(): RetroDao
    abstract fun ruleDao(): com.najmi.sprint.core.data.local.dao.RuleDao
}
