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
        com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity::class,
        com.najmi.sprint.core.data.local.entity.GithubIssueCacheEntity::class,
        com.najmi.sprint.core.data.local.entity.GithubCommitCacheEntity::class,
        com.najmi.sprint.core.data.local.entity.ProjectDocumentEntity::class
    ],
    version = 5,
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

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN githubOwner TEXT")
                db.execSQL("ALTER TABLE projects ADD COLUMN githubRepo TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN githubIssueNumber INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN githubIssueUrl TEXT")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `github_issues_cache` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `issueNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `state` TEXT NOT NULL, `htmlUrl` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `github_commits_cache` (`sha` TEXT NOT NULL, `projectId` TEXT NOT NULL, `message` TEXT NOT NULL, `htmlUrl` TEXT NOT NULL, PRIMARY KEY(`sha`))")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_documents` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `uri` TEXT NOT NULL, `title` TEXT NOT NULL, `lastOpenedAt` INTEGER, PRIMARY KEY(`id`))")
            }
        }
    }

    abstract fun contextDao(): ContextDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun retroDao(): RetroDao
    abstract fun ruleDao(): com.najmi.sprint.core.data.local.dao.RuleDao
    abstract fun githubCacheDao(): com.najmi.sprint.core.data.local.dao.GithubCacheDao
    abstract fun projectDocumentDao(): com.najmi.sprint.core.data.local.dao.ProjectDocumentDao
}
