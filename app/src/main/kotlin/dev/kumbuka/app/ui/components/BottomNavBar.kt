package dev.kumbuka.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.LocalKbColors

/** The four main destinations of the shell. Settings lives in the top bar (phone) or the rail (wide). */
enum class KbNavTab {
    TODAY,
    UNITS,
    ASSESSMENTS,
    PROGRESS,
    ;

    companion object {
        /** Restores a saved tab name; "HOME" is the pre-redesign name of [TODAY]. */
        fun fromSaved(name: String): KbNavTab =
            if (name == "HOME") TODAY else runCatching { valueOf(name) }.getOrDefault(TODAY)
    }
}

@Composable
fun kbNavTabLabel(tab: KbNavTab): String = when (tab) {
    KbNavTab.TODAY -> stringResource(R.string.nav_today)
    KbNavTab.UNITS -> stringResource(R.string.nav_units)
    KbNavTab.ASSESSMENTS -> stringResource(R.string.nav_assessments)
    KbNavTab.PROGRESS -> stringResource(R.string.nav_progress)
}

private fun KbNavTab.icon(selected: Boolean): ImageVector = when (this) {
    KbNavTab.TODAY -> if (selected) Icons.Filled.Today else Icons.Outlined.Today
    KbNavTab.UNITS -> if (selected) Icons.Filled.AutoStories else Icons.Outlined.AutoStories
    KbNavTab.ASSESSMENTS -> if (selected) Icons.AutoMirrored.Filled.EventNote else Icons.AutoMirrored.Outlined.EventNote
    KbNavTab.PROGRESS -> if (selected) Icons.Filled.Insights else Icons.Outlined.Insights
}

/**
 * Phone navigation: a flat 80dp bar on the paper-2 well, with a 64x32 tinted
 * pill behind the selected (filled) icon and a bold label - never colour alone.
 */
@Composable
fun KbBottomNavBar(active: KbNavTab, onSelect: (KbNavTab) -> Unit) {
    val colors = LocalKbColors.current
    NavigationBar(
        containerColor = colors.paper2,
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding(),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        KbNavTab.entries.forEach { tab -> KbNavItem(tab, active, onSelect) }
    }
}

@Composable
private fun RowScope.KbNavItem(tab: KbNavTab, active: KbNavTab, onSelect: (KbNavTab) -> Unit) {
    val colors = LocalKbColors.current
    val selected = active == tab
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(tab) },
        icon = { Icon(tab.icon(selected), contentDescription = null) },
        label = {
            Text(
                kbNavTabLabel(tab),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryTint,
            selectedTextColor = colors.ink,
            unselectedIconColor = colors.inkMuted,
            unselectedTextColor = colors.inkMuted,
            indicatorColor = colors.primaryTint,
        ),
    )
}

/**
 * Wide-window navigation: an 88dp rail on the paper-2 well with the mark at the
 * top, the four destinations (56x32 pill indicator) and Settings pinned to the
 * bottom. Replaces the bottom bar from 600dp, and carries Settings so the top
 * bar does not have to.
 */
@Composable
fun KbNavRail(
    active: KbNavTab,
    onSelect: (KbNavTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    Column(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(colors.paper2)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.height(64.dp).fillMaxWidth().padding(bottom = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppMarkGlyph(size = 34.dp, color = colors.ink, sunColor = colors.accent)
        }
        KbNavTab.entries.forEach { tab -> KbRailItem(tab, active == tab) { onSelect(tab) } }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.nav_settings),
                tint = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun KbRailItem(tab: KbNavTab, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalKbColors.current
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.primaryTint else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon(selected),
                contentDescription = null,
                tint = if (selected) colors.onPrimaryTint else colors.inkMuted,
            )
        }
        Text(
            kbNavTabLabel(tab),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (selected) colors.ink else colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
