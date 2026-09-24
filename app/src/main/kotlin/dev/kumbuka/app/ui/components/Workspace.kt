@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.LocalKbColors
import dev.kumbuka.app.ui.theme.unitMarker

/** Max content width on tablets/expanded windows so rows never stretch edge to edge. */
val KbContentMaxWidth = 720.dp

/** Bounded, centered content column for expanded windows. */
fun Modifier.kbContentWidth(): Modifier = this.fillMaxWidth().widthIn(max = KbContentMaxWidth)

/** Standard top bar for main and secondary screens (paper background, no divider). */
@Composable
fun KbTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalKbColors.current
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.paper,
            scrolledContainerColor = colors.paper,
            titleContentColor = colors.ink,
            navigationIconContentColor = colors.ink,
            actionIconContentColor = colors.ink,
        ),
    )
}

/** Toolbar entry point to Settings, present on every main destination. */
@Composable
fun KbSettingsAction(onOpenSettings: () -> Unit, tint: Color = LocalKbColors.current.ink) {
    IconButton(onClick = onOpenSettings) {
        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings), tint = tint)
    }
}

/** Section heading with an optional labelled trailing action ("View all"). */
@Composable
fun KbSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = LocalKbColors.current.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Opaque content surface with a hairline border - for distinct blocks, not every record. */
@Composable
fun KbSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = LocalKbColors.current.surface,
    borderColor: Color? = LocalKbColors.current.border,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
    val border = borderColor?.let { BorderStroke(1.dp, it) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = inner)
    }
}

/** Unit identity cue: stable colour dot + unit code. Always shows the code, never colour alone. */
@Composable
fun KbUnitLabel(unitKey: String, unitCode: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val colors = LocalKbColors.current
    val marker = colors.unitMarker(unitKey)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(marker, CircleShape))
        Text(
            unitCode,
            style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = if (emphasized) colors.ink else colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Units in one programme usually share a prefix ("ICS 3102", "ICS 3202"), so the badge shows
 * the course number when there is one and falls back to the letters otherwise.
 */
fun unitBadgeText(unitCode: String): String {
    val number = Regex("\\d+[A-Za-z]?").findAll(unitCode).lastOrNull()?.value
    return (number ?: unitCode.filter { it.isLetter() }.ifBlank { unitCode }).take(4).uppercase()
}

/** Square unit badge used on library rows and headers. */
@Composable
fun KbUnitBadge(unitKey: String, unitCode: String, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val colors = LocalKbColors.current
    val marker = colors.unitMarker(unitKey)
    val initials = unitBadgeText(unitCode)
    Box(
        modifier = Modifier
            .size(size)
            .background(marker.copy(alpha = if (colors.isDark) 0.22f else 0.12f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = MaterialTheme.typography.labelLarge, color = if (colors.isDark) marker else marker, maxLines = 1)
    }
}

enum class KbStatus { NEUTRAL, INFO, SUCCESS, WARNING, ERROR }

@Composable
private fun statusColors(status: KbStatus): Pair<Color, Color> {
    val c = LocalKbColors.current
    return when (status) {
        KbStatus.NEUTRAL -> c.paper2 to c.inkMuted
        KbStatus.INFO -> c.primaryTint to c.onPrimaryTint
        KbStatus.SUCCESS -> c.successContainer to c.success
        KbStatus.WARNING -> c.warningTint to c.warning
        KbStatus.ERROR -> c.errorContainer to c.error
    }
}

/** Small labelled status pill - text always carries the meaning, colour reinforces it. */
@Composable
fun KbStatusPill(text: String, status: KbStatus = KbStatus.NEUTRAL, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(status)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Inline feedback / notice banner with icon + text. */
@Composable
fun KbBanner(text: String, status: KbStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(status)
    val icon = when (status) {
        KbStatus.SUCCESS -> Icons.Outlined.CheckCircle
        KbStatus.WARNING -> Icons.Outlined.WarningAmber
        KbStatus.ERROR -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Info
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (status == KbStatus.NEUTRAL || status == KbStatus.INFO) LocalKbColors.current.ink else fg)
    }
}

/** Honest empty state: what is missing and the concrete next step. */
@Composable
fun KbEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val colors = LocalKbColors.current
    KbSurface(modifier = modifier, contentPadding = 20.dp) {
        Box(
            modifier = Modifier.size(52.dp).background(colors.primaryTint, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimaryTint)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        if (primaryLabel != null && onPrimary != null) {
            KbPrimaryButton(text = primaryLabel, onClick = onPrimary, modifier = Modifier.padding(top = 4.dp))
        }
        if (secondaryLabel != null && onSecondary != null) {
            KbSecondaryButton(text = secondaryLabel, onClick = onSecondary)
        }
    }
}

/** Compact, tappable list row used for topics, dates, results and settings. */
@Composable
fun KbListRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalKbColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surface, MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        trailing?.invoke()
    }
}

/** Calendar-style date tile for deadline rows: short month over day number. */
@Composable
fun KbDateTile(month: String, day: String, status: KbStatus = KbStatus.INFO) {
    val (bg, fg) = statusColors(status)
    Column(
        modifier = Modifier
            .size(width = 52.dp, height = 56.dp)
            .background(bg, RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(month.uppercase(), style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
        Text(day, style = MaterialTheme.typography.titleLarge, color = fg, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Big number + label, used by Progress summary tiles. */
@Composable
fun KbStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    Column(
        modifier = modifier
            .background(colors.surface, MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.ink, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}
