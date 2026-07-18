package com.najmi.sprint.tracking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.najmi.sprint.core.data.local.dao.GithubCacheDao
import com.najmi.sprint.core.data.local.entity.GithubCommitCacheEntity
import com.najmi.sprint.core.data.local.entity.GithubIssueCacheEntity
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.security.SecretRepository
import com.najmi.sprint.core.sync.client.GithubClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class GithubSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val projectRepository: ProjectRepository,
    private val secretRepository: SecretRepository,
    private val githubClient: GithubClient,
    private val githubCacheDao: GithubCacheDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val projects = projectRepository.observeAllProjects().firstOrNull() ?: emptyList()
            
            for (project in projects) {
                val owner = project.githubOwner
                val repo = project.githubRepo
                if (owner != null && repo != null) {
                    val patSecret = secretRepository.observeSecretsByProject(project.id)
                        .firstOrNull()?.find { it.label == "GITHUB_PAT" }
                    
                    if (patSecret != null) {
                        val pat = secretRepository.revealSecret(patSecret.id)
                        if (pat != null) {
                            syncProjectGithubData(project.id, owner, repo, pat)
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Back off on 403/429 limits cleanly
            if (e.message?.contains("403") == true || e.message?.contains("429") == true) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncProjectGithubData(projectId: String, owner: String, repo: String, pat: String) {
        val issues = githubClient.getOpenIssues(owner, repo, pat)
        val commits = githubClient.getRecentCommits(owner, repo, pat)

        val issueEntities = issues.map {
            GithubIssueCacheEntity(
                id = "${projectId}_${it.number}",
                projectId = projectId,
                issueNumber = it.number,
                title = it.title,
                state = it.state,
                htmlUrl = it.htmlUrl
            )
        }

        val commitEntities = commits.map {
            GithubCommitCacheEntity(
                sha = it.sha,
                projectId = projectId,
                message = it.commit.message,
                htmlUrl = it.htmlUrl
            )
        }

        githubCacheDao.replaceIssuesForProject(projectId, issueEntities)
        githubCacheDao.replaceCommitsForProject(projectId, commitEntities)
    }
}
