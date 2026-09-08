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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.PagerDots
import dev.kumbuka.app.ui.theme.KbColors
import kotlinx.coroutines.launch

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KbColors.paper)
            .padding(horizontal = 26.dp)
            .padding(top = 36.dp, bottom = 28.dp),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            OnboardingPageContent(pages[page])
        }
        PagerDots(
            count = pages.size,
            activeIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(KbColors.primaryTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = KbColors.primary, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = KbColors.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = KbColors.inkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
