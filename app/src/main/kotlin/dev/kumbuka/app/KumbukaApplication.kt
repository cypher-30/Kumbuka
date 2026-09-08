package dev.kumbuka.app

import android.app.Application
import androidx.room.Room
import dev.kumbuka.app.data.local.KumbukaDatabase
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository

class KumbukaApplication : Application() {
    val database: KumbukaDatabase by lazy {
        Room.databaseBuilder(this, KumbukaDatabase::class.java, KumbukaDatabase.DATABASE_NAME).build()
    }

    val unitRepository: UnitRepository by lazy { UnitRepository(database.unitDao()) }
    val topicRepository: TopicRepository by lazy { TopicRepository(database.topicDao()) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val deadlineRepository: DeadlineRepository by lazy { DeadlineRepository(database.deadlineDao()) }
    val assessmentMarkRepository: AssessmentMarkRepository by lazy {
        AssessmentMarkRepository(database.assessmentMarkDao())
    }
}
