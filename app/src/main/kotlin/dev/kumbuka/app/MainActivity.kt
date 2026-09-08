package dev.kumbuka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.theme.KumbukaTheme
import kotlinx.coroutines.flow.first
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KumbukaApplication
        setContent {
            KumbukaTheme {
                DataLayerProofScreen(app)
            }
        }
    }
}

/**
 * Phase 1 proof: writes a Unit -> Topic -> Session through the real
 * repositories (exercising the Converters for List<String> and the
 * Confidence enum), then reads the same rows back via Flow. Replaced by
 * the real Today/Units screens starting Phase 2-4.
 */
@Composable
private fun DataLayerProofScreen(app: KumbukaApplication) {
    var status by remember { mutableStateOf("Writing test rows...") }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val unitId = "probe-unit"
        val topicId = "probe-topic"

        app.unitRepository.upsert(
            UnitModel(
                id = unitId,
                code = "ICS 3102",
                title = "Data layer probe",
                packId = null,
                packVersion = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        app.topicRepository.upsert(
            Topic(
                id = topicId,
                unitId = unitId,
                title = "Graph Colouring",
                objective = "Explain the greedy algorithm and one application",
                retrievalPrompt = "Explain graph colouring, the greedy algorithm, and one application",
                examWeight = 0.15f,
                orderIndex = 0,
                resourcePointers = listOf("Lecture 6 slides", "Textbook ch. 9"),
                titleEditedLocally = false,
                objectiveEditedLocally = false,
                weightEditedLocally = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        app.sessionRepository.upsert(
            Session(
                id = UUID.randomUUID().toString(),
                topicId = topicId,
                startedAt = now,
                endedAt = now + 8 * 60_000,
                plannedMinutes = 8,
                actualSeconds = 480,
                confidenceBefore = Confidence.SHAKY,
                confidenceAfter = Confidence.OK,
                wasDeferred = false,
                updatedAt = now,
            ),
        )

        val readUnit = app.unitRepository.getById(unitId)
        val readTopics = app.topicRepository.observeByUnit(unitId).first()
        val readSession = app.sessionRepository.getLatestForTopic(topicId)

        status = buildString {
            appendLine("unit: ${readUnit?.title} (${readUnit?.code})")
            appendLine("topic: ${readTopics.firstOrNull()?.title}")
            appendLine("resourcePointers: ${readTopics.firstOrNull()?.resourcePointers}")
            appendLine("session confidenceBefore: ${readSession?.confidenceBefore}")
            appendLine("gap from confidenceBefore: ${readSession?.confidenceBefore?.asGap()}")
        }
    }

    Scaffold { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Kumbuka — data layer proof",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
