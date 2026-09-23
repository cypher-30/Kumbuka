@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.KbSpacing
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(KbSpacing.x2),
            verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
        ) {
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.insights_placeholder_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
        }
    }
}
