package dev.kumbuka.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbCheckBox
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSegmentedControl
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch

private val OnboardingMaxWidth = 480.dp
private const val StepCount = 3

/** What the student picked on the last onboarding step. */
enum class FirstUnitChoice { IMPORT, SAMPLE, CREATE, SKIP }

/**
 * Three-step first run: welcome (language + how it works), "before you start"
 * (privacy acknowledgement is required, study participation is optional and
 * starts unticked) and add-your-first-unit. [onSaveConsent] persists consent +
 * onboarding completion atomically when leaving step 2; on failure the screen
 * stays put with a retry affordance rather than silently continuing. There is
 * no account, login or guest mode.
 */
@Composable
fun OnboardingScreen(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onSaveConsent: suspend (participateInStudy: Boolean) -> Result<Unit>,
    onChoose: (FirstUnitChoice) -> Unit,
) {
    val colors = LocalKbColors.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var participate by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = step > 0) { step -= 1 }

    fun continueFromConsent() {
        scope.launch {
            saving = true
            saveError = false
            val result = onSaveConsent(participate)
            saving = false
            if (result.isSuccess) step = 2 else saveError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = OnboardingMaxWidth)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        ) {
            StepHeader(step = step, onBack = { step -= 1 })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (step) {
                    0 -> WelcomeStep(currentLanguage, onLanguageSelected)
                    1 -> ConsentStep(
                        acknowledged = acknowledged,
                        onAcknowledged = { acknowledged = it },
                        participate = participate,
                        onParticipate = { participate = it },
                        saveError = saveError,
                    )
                    else -> FirstUnitStep(onChoose)
                }
            }
            when (step) {
                0 -> KbPrimaryButton(
                    text = stringResource(R.string.common_continue),
                    onClick = { step = 1 },
                    modifier = Modifier.height(52.dp),
                )
                1 -> KbPrimaryButton(
                    text = stringResource(if (saveError) R.string.onboarding_retry else R.string.common_continue),
                    onClick = ::continueFromConsent,
                    enabled = acknowledged && !saving,
                    modifier = Modifier.height(52.dp),
                )
                else -> TextButton(onClick = { onChoose(FirstUnitChoice.SKIP) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(
                        stringResource(R.string.common_skip_for_now),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int, onBack: () -> Unit) {
    val colors = LocalKbColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (step > 0) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back), tint = colors.ink)
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(StepCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (index <= step) colors.primary else colors.border, RoundedCornerShape(2.dp)),
                )
            }
        }
        Text(
            stringResource(R.string.onboarding_step, step + 1, StepCount),
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
        )
    }
}

@Composable
private fun WelcomeStep(currentLanguage: String, onLanguageSelected: (String) -> Unit) {
    val colors = LocalKbColors.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            modifier = Modifier.padding(top = 12.dp).size(76.dp).clip(RoundedCornerShape(24.dp)).background(colors.brand),
            contentAlignment = Alignment.Center,
        ) {
            AppMarkGlyph(size = 46.dp, color = colors.accent)
        }
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp, lineHeight = 37.sp),
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
            color = colors.inkMuted,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.onboarding_language_label),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = colors.inkMuted,
            )
            KbSegmentedControl(
                options = listOf("English", "Kiswahili"),
                selectedIndex = if (currentLanguage == "sw") 1 else 0,
                onSelect = { onLanguageSelected(if (it == 1) "sw" else "en") },
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            listOf(R.string.onboarding_how_1, R.string.onboarding_how_2, R.string.onboarding_how_3).forEachIndexed { index, text ->
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(colors.primaryTint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.onPrimaryTint,
                        )
                    }
                    Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                }
            }
        }
    }
}

@Composable
private fun ConsentStep(
    acknowledged: Boolean,
    onAcknowledged: (Boolean) -> Unit,
    participate: Boolean,
    onParticipate: (Boolean) -> Unit,
    saveError: Boolean,
) {
    val colors = LocalKbColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.onboarding_consent_title),
            style = MaterialTheme.typography.displayMedium,
            color = colors.ink,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Text(
            stringResource(R.string.onboarding_consent_body),
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
            color = colors.inkMuted,
        )
        InfoCard(Icons.Outlined.Smartphone, colors.primary, R.string.onboarding_recorded_title, R.string.onboarding_recorded_list)
        InfoCard(Icons.Outlined.Block, colors.inkMuted, R.string.onboarding_never_title, R.string.onboarding_never_list)
        CheckCard(acknowledged, onAcknowledged, R.string.onboarding_privacy_checkbox, R.string.common_required)
        CheckCard(participate, onParticipate, R.string.onboarding_check_study, R.string.common_optional)
        if (saveError) {
            KbBanner(stringResource(R.string.onboarding_save_failed), KbStatus.ERROR)
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, tint: Color, title: Int, body: Int) {
    val colors = LocalKbColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.ink)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = colors.inkMuted)
        }
    }
}

@Composable
private fun CheckCard(checked: Boolean, onChange: (Boolean) -> Unit, text: Int, tag: Int) {
    val colors = LocalKbColors.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, if (checked) colors.primary else colors.border, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        KbCheckBox(checked, modifier = Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp), color = colors.ink)
            Text(
                stringResource(tag),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun FirstUnitStep(onChoose: (FirstUnitChoice) -> Unit) {
    val colors = LocalKbColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.onboarding_add_first_title),
            style = MaterialTheme.typography.displayMedium,
            color = colors.ink,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Text(
            stringResource(R.string.onboarding_add_first_body),
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
            color = colors.inkMuted,
        )
        ChoiceCard(Icons.Outlined.UploadFile, R.string.import_pack_title, R.string.onboarding_import_desc, primary = true) {
            onChoose(FirstUnitChoice.IMPORT)
        }
        ChoiceCard(Icons.Outlined.Science, R.string.import_try_sample_cta, R.string.onboarding_sample_desc, primary = false) {
            onChoose(FirstUnitChoice.SAMPLE)
        }
        ChoiceCard(Icons.Outlined.EditNote, R.string.author_start_cta, R.string.onboarding_create_desc, primary = false) {
            onChoose(FirstUnitChoice.CREATE)
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: Int, desc: Int, primary: Boolean, onClick: () -> Unit) {
    val colors = LocalKbColors.current
    val shape = RoundedCornerShape(20.dp)
    val background = if (primary) colors.primary else colors.surface
    val foreground = if (primary) colors.onPrimary else colors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(if (primary) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (primary) Color.White.copy(alpha = 0.18f) else colors.primaryTint, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (primary) colors.onPrimary else colors.onPrimaryTint)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = foreground)
            Text(
                stringResource(desc),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = foreground.copy(alpha = 0.85f),
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = foreground.copy(alpha = 0.7f))
    }
}
