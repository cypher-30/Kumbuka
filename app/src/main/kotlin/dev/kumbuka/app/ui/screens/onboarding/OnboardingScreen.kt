package dev.kumbuka.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.PagerDots
import dev.kumbuka.app.ui.theme.LocalKbColors
import dev.kumbuka.app.ui.theme.KbSpacing
import kotlinx.coroutines.launch

private val OnboardingMaxWidth = 640.dp

private data class OnboardingPage(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

private val pages = listOf(
    OnboardingPage(Icons.Outlined.Bedtime, R.string.onboarding_1_title, R.string.onboarding_1_body),
    OnboardingPage(Icons.Outlined.Insights, R.string.onboarding_2_title, R.string.onboarding_2_body),
    OnboardingPage(Icons.Outlined.MenuBook, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/** Figma nodes 1:5635 / 1:5654 / 1:5674, "04-06 Onboarding {1,2,3}". */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(LocalKbColors.current.paper),
    ) {
        val availableWidth = screenWidth
        val compact = availableWidth < 360.dp
        val horizontalInset = if (compact) KbSpacing.x2 else KbSpacing.x3
        val iconBoxSize = if (compact) 72.dp else if (availableWidth >= 600.dp) 96.dp else 88.dp
        val iconSize = if (compact) 30.dp else if (availableWidth >= 600.dp) 42.dp else 38.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = OnboardingMaxWidth)
                .align(Alignment.Center)
                .padding(horizontal = horizontalInset)
                .padding(top = KbSpacing.x4, bottom = KbSpacing.x3),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                    },
                    label = "onboardingPageTransition",
                ) { activePage ->
                    OnboardingPageContent(
                        page = pages[activePage],
                        iconBoxSize = iconBoxSize,
                        iconSize = iconSize,
                    )
                }
            }
            PagerDots(
                count = pages.size,
                activeIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = KbSpacing.x1 / 2),
            )
            Spacer(Modifier.height(KbSpacing.x2))
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                    KbPrimaryButton(
                        text = stringResource(
                            if (pagerState.currentPage == pages.lastIndex) R.string.onboarding_get_started else R.string.onboarding_continue,
                        ),
                        onClick = {
                            if (pagerState.currentPage == pages.lastIndex) {
                                onFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = tween(durationMillis = 280),
                                    )
                                }
                            }
                        },
                    )
                    KbSecondaryButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = onFinished,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(KbSpacing.x1), modifier = Modifier.fillMaxWidth()) {
                    KbSecondaryButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = onFinished,
                        modifier = Modifier.weight(1f),
                    )
                    KbPrimaryButton(
                        text = stringResource(
                            if (pagerState.currentPage == pages.lastIndex) R.string.onboarding_get_started else R.string.onboarding_continue,
                        ),
                        onClick = {
                            if (pagerState.currentPage == pages.lastIndex) {
                                onFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = tween(durationMillis = 280),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, iconBoxSize: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(LocalKbColors.current.primaryTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = LocalKbColors.current.primary, modifier = Modifier.size(iconSize))
        }
        Spacer(Modifier.height(KbSpacing.x3))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = LocalKbColors.current.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(KbSpacing.x1))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalKbColors.current.inkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = KbSpacing.x2),
        )
    }
}
