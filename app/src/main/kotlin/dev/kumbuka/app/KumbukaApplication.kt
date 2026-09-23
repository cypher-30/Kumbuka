package dev.kumbuka.app

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dev.kumbuka.app.data.bootstrap.DefaultContentSeeder
import dev.kumbuka.app.data.local.KumbukaDatabase
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.reminders.ReminderWorker
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.PackRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val android.content.Context.dataStore by preferencesDataStore(name = "kumbuka_prefs")

class KumbukaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
    val packRepository: PackRepository by lazy {
        PackRepository(database, unitRepository, topicRepository, deadlineRepository)
    }

    val preferences: AppPreferences by lazy { AppPreferences(dataStore) }

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.ensureChannel(this)
        applicationScope.launch {
            runCatching {
                DefaultContentSeeder.seedIfEmpty(unitRepository, packRepository)
            }.onFailure { error ->
                Log.e(TAG, "Failed to seed default content", error)
            }
        }
    }

    companion object {
        private const val TAG = "KumbukaApplication"
    }
}
