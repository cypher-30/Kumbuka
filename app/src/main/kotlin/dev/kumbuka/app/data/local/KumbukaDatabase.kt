package dev.kumbuka.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.kumbuka.app.data.local.dao.AssessmentMarkDao
import dev.kumbuka.app.data.local.dao.DeadlineDao
import dev.kumbuka.app.data.local.dao.SchedulerEvaluationDao
import dev.kumbuka.app.data.local.dao.SessionDao
import dev.kumbuka.app.data.local.dao.TopicDao
import dev.kumbuka.app.data.local.dao.UnitDao
import dev.kumbuka.app.data.local.entity.AssessmentMarkEntity
import dev.kumbuka.app.data.local.entity.AssessmentMarkTopicCrossRef
import dev.kumbuka.app.data.local.entity.DeadlineEntity
import dev.kumbuka.app.data.local.entity.DeadlineTopicCrossRef
import dev.kumbuka.app.data.local.entity.SchedulerEvaluationEntity
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
        SchedulerEvaluationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KumbukaDatabase : RoomDatabase() {
    abstract fun unitDao(): UnitDao
    abstract fun topicDao(): TopicDao
    abstract fun sessionDao(): SessionDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun assessmentMarkDao(): AssessmentMarkDao
    abstract fun schedulerEvaluationDao(): SchedulerEvaluationDao

    companion object {
        const val DATABASE_NAME = "kumbuka.db"

        /**
         * v1 -> v2: adds archive/sample/research-provenance columns and the new
         * scheduler_evaluations table. Purely additive - no destructive rebuild,
         * so every existing unit/topic/session/mark row (and its history) survives.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE units ADD COLUMN isSample INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN sourcePlanId TEXT")
                db.execSQL("ALTER TABLE sessions ADD COLUMN sourceArm TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scheduler_evaluations (
                        id TEXT NOT NULL PRIMARY KEY,
                        planId TEXT NOT NULL,
                        evaluatedAt INTEGER NOT NULL,
                        topicId TEXT NOT NULL,
                        unitId TEXT NOT NULL,
                        arm TEXT NOT NULL,
                        requestedArm TEXT NOT NULL,
                        actualSource TEXT NOT NULL,
                        fallbackReason TEXT,
                        rank INTEGER NOT NULL,
                        selected INTEGER NOT NULL,
                        minutes INTEGER NOT NULL,
                        score REAL NOT NULL,
                        predictedRecall REAL,
                        predictorVersion TEXT,
                        isSample INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduler_evaluations_planId ON scheduler_evaluations(planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduler_evaluations_topicId ON scheduler_evaluations(topicId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduler_evaluations_evaluatedAt ON scheduler_evaluations(evaluatedAt)")
            }
        }
    }
}
