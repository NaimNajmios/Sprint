package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.ClassificationRule

interface RuleRepository {
    suspend fun getRuleForPackage(packageName: String): ClassificationRule?
    suspend fun insertOrUpdateRule(rule: ClassificationRule)
    suspend fun getAllRules(): List<ClassificationRule>
    suspend fun deleteRule(packageName: String)
    fun observeIgnoredRules(): kotlinx.coroutines.flow.Flow<List<ClassificationRule>>
    suspend fun setPackageIgnored(packageName: String, isIgnored: Boolean)
}
