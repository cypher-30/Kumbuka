package dev.kumbuka.app.ui.screens.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.theme.KbColors

/** Figma node 58:241 "23 Consent" - a local-storage privacy notice, not research consent. */
@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KbColors.paper)
            .padding(horizontal = 26.dp)
            .padding(top = 36.dp, bottom = 28.dp),
    ) {
        Text(stringResource(R.string.consent_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.consent_body),
            style = MaterialTheme.typography.bodyMedium,
            color = KbColors.inkMuted,
        )
        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, KbColors.border, RoundedCornerShape(10.dp))
                .background(KbColors.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { checked = !checked }
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ConsentCheckbox(checked)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.consent_checkbox_label),
                style = MaterialTheme.typography.bodyMedium,
                color = KbColors.ink,
            )
        }
        Spacer(Modifier.height(14.dp))
        KbPrimaryButton(
            text = stringResource(R.string.consent_continue),
            onClick = onAccept,
            enabled = checked,
        )
    }
}

@Composable
private fun ConsentCheckbox(checked: Boolean) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) KbColors.primary else KbColors.surface)
            .border(1.5.dp, if (checked) KbColors.primary else KbColors.border, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = KbColors.surface, modifier = Modifier.size(13.dp))
        }
    }
}
