package dev.kumbuka.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.ui.theme.KbColors

/**
 * Figma node 1:5551 "01 Splash". Purely visual - the caller (KumbukaNavGraph)
 * decides when to navigate away, since that decision depends on the
 * onboardingComplete preference finishing its first DataStore read, not just
 * on a fixed timer.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(KbColors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                AppMarkGlyph(size = 52.dp)
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}
