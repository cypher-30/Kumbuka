package dev.kumbuka.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.kumbuka.app.data.local.dao.AssessmentMarkDao
import dev.kumbuka.app.data.local.dao.DeadlineDao
import dev.kumbuka.app.data.local.dao.SessionDao
import dev.kumbuka.app.data.local.dao.TopicDao
import dev.kumbuka.app.data.local.dao.UnitDao
import dev.kumbuka.app.data.local.entity.AssessmentMarkEntity
import dev.kumbuka.app.data.local.entity.AssessmentMarkTopicCrossRef
import dev.kumbuka.app.data.local.entity.DeadlineEntity
import dev.kumbuka.app.data.local.entity.DeadlineTopicCrossRef
import dev.kumbuka.app.data.local.entity.SessionEntity
import dev.kumbuka.app.data.local.entity.TopicEntity
import dev.kumbuka.app.data.local.entity.UnitEntity

@Database(
    entities = [
        UnitEntity::class,
        TopicEntity::class,
        SessionEntity::class,
        DeadlineEntity::class,
        DeadlineTopicCrossRef::class,
        AssessmentMarkEntity::class,
        AssessmentMarkTopicCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KumbukaDatabase : RoomDatabase() {
    abstract fun unitDao(): UnitDao
    abstract fun topicDao(): TopicDao
    abstract fun sessionDao(): SessionDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun assessmentMarkDao(): AssessmentMarkDao

    companion object {
        const val DATABASE_NAME = "kumbuka.db"
    }
}
