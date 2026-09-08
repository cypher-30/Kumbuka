package dev.kumbuka.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.theme.KbColors

/**
 * Figma nodes 1:5559 "02 Login — Sign in" and 1:5597 "03 Login — Create account",
 * combined into one screen with a tab switch, matching the shared "tabs" component
 * both frames use. Accounts are genuinely not wired up (no server, no auth) - the
 * placeholder note is real, not a mistake, per the user's explicit decision to keep
 * Figma's own honesty about this rather than hide or fake a login.
 */
@Composable
fun LoginScreen(
    currentLanguage: String,
    onToggleLanguage: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = sign in, 1 = create account
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KbColors.paper)
            .padding(horizontal = 26.dp)
            .padding(top = 36.dp, bottom = 28.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KbColors.primary),
            contentAlignment = Alignment.Center,
        ) {
            AppMarkGlyph(size = 28.dp, color = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.login_welcome_back), style = MaterialTheme.typography.headlineSmall, color = KbColors.ink)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(KbColors.warningTint)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                stringResource(R.string.login_not_wired_note),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = KbColors.inkMuted,
            )
        }
        Spacer(Modifier.height(14.dp))

        LoginTabs(selected = tab, onSelect = { tab = it })
        Spacer(Modifier.height(12.dp))

        LabeledField(
            label = stringResource(R.string.login_email_label),
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.login_email_placeholder),
        )
        Spacer(Modifier.height(12.dp))
        LabeledField(
            label = stringResource(R.string.login_password_label),
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true,
        )
        if (tab == 1) {
            Spacer(Modifier.height(12.dp))
            LabeledField(
                label = stringResource(R.string.login_confirm_password_label),
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "••••••••",
                isPassword = true,
            )
        }
        Spacer(Modifier.height(18.dp))

        KbPrimaryButton(
            text = stringResource(if (tab == 0) R.string.login_submit_signin else R.string.login_submit_signup),
            onClick = { /* no backend: DESIGN.md keeps accounts out of v1 - see the note above */ },
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalRule()
            Text(stringResource(R.string.login_or), style = MaterialTheme.typography.bodySmall, color = KbColors.inkFaint)
            HorizontalRule()
        }
        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KbColors.border),
        ) {
            Text(stringResource(R.string.login_continue_guest), style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Language, contentDescription = null, tint = KbColors.ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (currentLanguage == "sw") "Badili kuwa Kiingereza" else stringResource(R.string.login_switch_language),
                style = MaterialTheme.typography.labelMedium,
                color = KbColors.primary,
                modifier = Modifier.clickableNoRipple(onToggleLanguage),
            )
        }
    }
}

@Composable
private fun LoginTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KbColors.paper2)
            .padding(3.dp),
    ) {
        TabChip(stringResource(R.string.login_tab_signin), selected == 0, Modifier.weight(1f)) { onSelect(0) }
        TabChip(stringResource(R.string.login_tab_signup), selected == 1, Modifier.weight(1f)) { onSelect(1) }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) KbColors.surface else KbColors.paper2)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) KbColors.ink else KbColors.inkMuted,
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = KbColors.inkMuted)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = KbColors.inkFaint) },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = KbColors.surface,
                focusedContainerColor = KbColors.surface,
                unfocusedBorderColor = KbColors.border,
                focusedBorderColor = KbColors.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HorizontalRule() {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(KbColors.border),
    )
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    ),
)
