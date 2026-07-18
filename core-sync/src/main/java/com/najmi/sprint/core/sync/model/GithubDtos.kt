package com.najmi.sprint.core.sync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubIssueDto(
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("pull_request") val pullRequest: GithubPullRequestDto? = null
)

@Serializable
data class GithubPullRequestDto(
    val url: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("merged_at") val mergedAt: String? = null
)

@Serializable
data class GithubCommitDto(
    val sha: String,
    val commit: GithubCommitDetailDto,
    @SerialName("html_url") val htmlUrl: String
)

@Serializable
data class GithubCommitDetailDto(
    val message: String
)
