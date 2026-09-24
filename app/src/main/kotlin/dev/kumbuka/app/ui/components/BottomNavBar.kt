package dev.kumbuka.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.LocalKbColors

/** The four main destinations of the Campus Workspace shell. Settings lives in the toolbar. */
enum class KbNavTab {
    HOME,
    UNITS,
    ASSESSMENTS,
    PROGRESS,
}

@Composable
fun kbNavTabLabel(tab: KbNavTab): String = when (tab) {
    KbNavTab.HOME -> stringResource(R.string.nav_home)
    KbNavTab.UNITS -> stringResource(R.string.nav_units)
    KbNavTab.ASSESSMENTS -> stringResource(R.string.nav_assessments)
    KbNavTab.PROGRESS -> stringResource(R.string.nav_progress)
}

/**
 * Functional navigation layer, visually separated from content: a gently
 * elevated surface with a clear selection pill (filled icon + tinted
 * indicator + bold label), never colour alone.
 */
@Composable
fun KbBottomNavBar(active: KbNavTab, onSelect: (KbNavTab) -> Unit) {
    val colors = LocalKbColors.current
    Surface(
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 4.dp),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) {
            KbNavItem(KbNavTab.HOME, Icons.Outlined.Home, Icons.Filled.Home, active, onSelect)
            KbNavItem(KbNavTab.UNITS, Icons.Outlined.AutoStories, Icons.Filled.AutoStories, active, onSelect)
            KbNavItem(KbNavTab.ASSESSMENTS, Icons.AutoMirrored.Outlined.EventNote, Icons.AutoMirrored.Filled.EventNote, active, onSelect)
            KbNavItem(KbNavTab.PROGRESS, Icons.Outlined.Insights, Icons.Filled.Insights, active, onSelect)
        }
    }
}

@Composable
private fun RowScope.KbNavItem(
    tab: KbNavTab,
    icon: ImageVector,
    selectedIcon: ImageVector,
    active: KbNavTab,
    onSelect: (KbNavTab) -> Unit,
) {
    val colors = LocalKbColors.current
    val selected = active == tab
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(tab) },
        icon = { Icon(if (selected) selectedIcon else icon, contentDescription = null) },
        label = {
            Text(
                kbNavTabLabel(tab),
                style = if (selected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryTint,
            selectedTextColor = colors.primary,
            unselectedIconColor = colors.inkMuted,
            unselectedTextColor = colors.inkMuted,
            indicatorColor = colors.primaryTint,
        ),
    )
}
