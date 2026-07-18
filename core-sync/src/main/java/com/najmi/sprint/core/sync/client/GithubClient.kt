package com.najmi.sprint.core.sync.client

import com.najmi.sprint.core.sync.model.GithubCommitDto
import com.najmi.sprint.core.sync.model.GithubIssueDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubClient @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun getOpenIssues(owner: String, repo: String, pat: String): List<GithubIssueDto> {
        return httpClient.get("https://api.github.com/repos/$owner/$repo/issues") {
            header("Authorization", "Bearer $pat")
            header("Accept", "application/vnd.github.v3+json")
            parameter("state", "open")
            parameter("per_page", 50)
        }.body()
    }

    suspend fun getRecentCommits(owner: String, repo: String, pat: String, sinceIso8601: String? = null): List<GithubCommitDto> {
        return httpClient.get("https://api.github.com/repos/$owner/$repo/commits") {
            header("Authorization", "Bearer $pat")
            header("Accept", "application/vnd.github.v3+json")
            if (sinceIso8601 != null) {
                parameter("since", sinceIso8601)
            }
            parameter("per_page", 50)
        }.body()
    }

    suspend fun closeIssue(owner: String, repo: String, pat: String, issueNumber: Int) {
        httpClient.patch("https://api.github.com/repos/$owner/$repo/issues/$issueNumber") {
            header("Authorization", "Bearer $pat")
            header("Accept", "application/vnd.github.v3+json")
            contentType(ContentType.Application.Json)
            setBody(mapOf("state" to "closed"))
        }
    }
}
