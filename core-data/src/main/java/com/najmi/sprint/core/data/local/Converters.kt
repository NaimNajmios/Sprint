package com.najmi.sprint.core.data.local

import androidx.room.TypeConverter
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class Converters {

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(millis: Long?): Instant? {
        return millis?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(status: String): TaskStatus {
        return TaskStatus.valueOf(status)
    }

    @TypeConverter
    fun fromSessionSource(source: SessionSource): String {
        return source.name
    }

    @TypeConverter
    fun toSessionSource(source: String): SessionSource {
        return SessionSource.valueOf(source)
    }
}
