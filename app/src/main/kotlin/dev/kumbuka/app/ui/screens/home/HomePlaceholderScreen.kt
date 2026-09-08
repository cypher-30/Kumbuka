package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.theme.KbColors

/**
 * Stands in for the real Today screen (Figma "07 Home — Tonight") until the
 * baseline scheduler lands in a later phase. Exists so Phase 2's navigation
 * flow has somewhere real to land after onboarding, and so the bottom nav
 * this phase built can actually be exercised end to end.
 */
@Composable
fun HomePlaceholderScreen(onBrowseUnits: () -> Unit) {
    var activeTab by remember { mutableStateOf(KbNavTab.TONIGHT) }

    Scaffold(
        bottomBar = { KbBottomNavBar(active = activeTab, onSelect = { activeTab = it }) },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.home_placeholder_title), style = MaterialTheme.typography.bodyLarge, color = KbColors.inkMuted)
            Spacer(Modifier.height(16.dp))
            KbPrimaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}
