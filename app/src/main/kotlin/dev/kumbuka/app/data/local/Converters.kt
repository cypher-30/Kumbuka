package dev.kumbuka.app.data.local

import androidx.room.TypeConverter
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.DeadlineKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromConfidence(value: Confidence?): Int? = value?.score

    @TypeConverter
    fun toConfidence(value: Int?): Confidence? = value?.let { Confidence.fromScore(it) }

    @TypeConverter
    fun fromDeadlineKind(value: DeadlineKind): String = value.name

    @TypeConverter
    fun toDeadlineKind(value: String): DeadlineKind = DeadlineKind.valueOf(value)

    @TypeConverter
    fun fromAssessmentKind(value: AssessmentKind): String = value.name

    @TypeConverter
    fun toAssessmentKind(value: String): AssessmentKind = AssessmentKind.valueOf(value)
}
