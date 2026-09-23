package dev.kumbuka.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.ui.theme.KbBrand
import dev.kumbuka.app.ui.theme.KbSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** cubic-bezier(.2,.9,.3,1.2) - the bounce-overshoot easing on the icon pop-in. */
private val PopEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

/** cubic-bezier(.4,0,.2,1) - the ease-out on the curve draw-in. */
private val CurveEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** Total choreography length; caller advances navigation once this elapses. */
const val SplashAnimationDurationMs = 1500L

/**
 * Figma node 1:5551 "01 Splash", restored on top of the native SplashScreen
 * icon by explicit request: the OS-owned native splash still shows the
 * instant the process starts (avoiding a blank/white first frame), but once
 * MainActivity resolves the real start route it hands off to this composable
 * for the fuller icon pop -> curve draw -> wordmark fade-up -> tagline
 * fade-up choreography before actually navigating to onboarding/Today.
 */
@Composable
fun SplashScreen() {
    val iconScale = remember { Animatable(0.7f) }
    val iconAlpha = remember { Animatable(0f) }
    val curveProgress = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(10f) }
    val tagAlpha = remember { Animatable(0f) }
    val tagOffset = remember { Animatable(10f) }

    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(700, easing = LinearEasing)) }
        launch { iconScale.animateTo(1f, tween(700, easing = PopEasing)) }
        launch {
            delay(300)
            curveProgress.animateTo(1f, tween(1100, easing = CurveEasing))
        }
        launch {
            delay(500)
            wordAlpha.animateTo(1f, tween(600, easing = LinearEasing))
        }
        launch {
            delay(500)
            wordOffset.animateTo(0f, tween(600, easing = LinearEasing))
        }
        launch {
            delay(700)
            tagAlpha.animateTo(1f, tween(600, easing = LinearEasing))
        }
        launch {
            delay(700)
            tagOffset.animateTo(0f, tween(600, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KbBrand.primary)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KbSpacing.x2)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                AppMarkGlyph(size = 52.dp, drawProgress = curveProgress.value)
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier
                    .alpha(wordAlpha.value)
                    .offset(y = wordOffset.value.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .alpha(tagAlpha.value)
                    .offset(y = tagOffset.value.dp),
            )
        }
    }
}
