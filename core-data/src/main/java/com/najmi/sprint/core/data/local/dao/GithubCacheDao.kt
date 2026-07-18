package com.najmi.sprint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.najmi.sprint.core.data.local.entity.GithubCommitCacheEntity
import com.najmi.sprint.core.data.local.entity.GithubIssueCacheEntity

@Dao
interface GithubCacheDao {
    @Query("SELECT * FROM github_issues_cache WHERE projectId = :projectId")
    suspend fun getIssuesForProject(projectId: String): List<GithubIssueCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<GithubIssueCacheEntity>)

    @Query("DELETE FROM github_issues_cache WHERE projectId = :projectId")
    suspend fun deleteIssuesForProject(projectId: String)

    @Transaction
    suspend fun replaceIssuesForProject(projectId: String, issues: List<GithubIssueCacheEntity>) {
        deleteIssuesForProject(projectId)
        insertIssues(issues)
    }

    @Query("SELECT * FROM github_commits_cache WHERE projectId = :projectId")
    suspend fun getCommitsForProject(projectId: String): List<GithubCommitCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommits(commits: List<GithubCommitCacheEntity>)

    @Query("DELETE FROM github_commits_cache WHERE projectId = :projectId")
    suspend fun deleteCommitsForProject(projectId: String)

    @Transaction
    suspend fun replaceCommitsForProject(projectId: String, commits: List<GithubCommitCacheEntity>) {
        deleteCommitsForProject(projectId)
        insertCommits(commits)
    }
}
