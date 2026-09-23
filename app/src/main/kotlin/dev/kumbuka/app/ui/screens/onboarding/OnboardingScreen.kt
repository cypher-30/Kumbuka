package dev.kumbuka.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.theme.KbSpacing
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch

private val OnboardingMaxWidth = 480.dp

/**
 * The entire first-run experience is this one page: language, honest
 * local/offline copy, an explicit privacy acknowledgement and a single
 * "Get started" action - Consent and the old three-page pager have been
 * merged into it (DESIGN.md: shortest first run, no login). [onSave] must
 * persist consent + onboarding completion atomically; on failure this
 * screen stays put with a retry affordance rather than silently continuing
 * or getting stuck.
 */
@Composable
fun OnboardingScreen(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onSave: suspend () -> Result<Unit>,
    onFinished: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        scope.launch {
            saving = true
            saveError = false
            val result = onSave()
            saving = false
            if (result.isSuccess) {
                onFinished()
            } else {
                saveError = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalKbColors.current.paper)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = OnboardingMaxWidth)
                .align(Alignment.Center)
                .padding(horizontal = KbSpacing.x3)
                .padding(top = KbSpacing.x4, bottom = KbSpacing.x3),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(LocalKbColors.current.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    AppMarkGlyph(size = 40.dp)
                }
                Spacer(Modifier.height(KbSpacing.x2))
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = LocalKbColors.current.ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(KbSpacing.x1))
                Text(
                    text = stringResource(R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalKbColors.current.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(KbSpacing.x3))
            Text(stringResource(R.string.onboarding_language_label), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            Spacer(Modifier.height(KbSpacing.x1 / 2))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = currentLanguage == "en", onClick = { onLanguageSelected("en") }, label = { Text("English") })
                FilterChip(selected = currentLanguage == "sw", onClick = { onLanguageSelected("sw") }, label = { Text("Kiswahili") })
            }

            Spacer(Modifier.height(KbSpacing.x3))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LocalKbColors.current.border, RoundedCornerShape(10.dp))
                    .background(LocalKbColors.current.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { checked = !checked }
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                PrivacyCheckbox(checked)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.onboarding_privacy_checkbox),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalKbColors.current.ink,
                )
            }

            if (saveError) {
                Spacer(Modifier.height(KbSpacing.x1))
                Text(
                    stringResource(R.string.onboarding_save_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(KbSpacing.x2))
            KbPrimaryButton(
                text = stringResource(if (saveError) R.string.onboarding_retry else R.string.onboarding_get_started),
                onClick = ::save,
                enabled = checked && !saving,
            )
        }
    }
}

@Composable
private fun PrivacyCheckbox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) LocalKbColors.current.primary else LocalKbColors.current.surface)
            .border(1.5.dp, if (checked) LocalKbColors.current.primary else LocalKbColors.current.border, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = LocalKbColors.current.surface, modifier = Modifier.size(13.dp))
        }
    }
}
