package dev.kumbuka.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbColors

/** Figma's "click:submit-auth" / "click:accept-consent" / "click:next-onboard" pattern: full-width, 48dp, primary fill. */
@Composable
fun KbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KbColors.primary,
            contentColor = KbColors.surface,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Figma's "click:skip-onboard" / "click:go-onboarding" pattern: full-width, 48dp, outlined. */
@Composable
fun KbSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, KbColors.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = KbColors.ink),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
