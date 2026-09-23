package dev.kumbuka.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.LocalKbColors

enum class KbNavTab {
    TONIGHT,
    PROGRESS,
    EXAMS,
    SETTINGS,
}

/** Figma "Bottom Nav" component (node 1:5546): 4 tabs, active tab tinted primary. */
@Composable
fun KbBottomNavBar(active: KbNavTab, onSelect: (KbNavTab) -> Unit) {
    Column {
        HorizontalDivider(color = LocalKbColors.current.border)
        NavigationBar(containerColor = LocalKbColors.current.surface, modifier = Modifier.navigationBarsPadding()) {
            KbNavItem(KbNavTab.TONIGHT, Icons.Outlined.Home, active, onSelect)
            KbNavItem(KbNavTab.PROGRESS, Icons.Outlined.BarChart, active, onSelect)
            KbNavItem(KbNavTab.EXAMS, Icons.Outlined.CalendarMonth, active, onSelect)
            KbNavItem(KbNavTab.SETTINGS, Icons.Outlined.Tune, active, onSelect)
        }
    }
}

@Composable
private fun RowScope.KbNavItem(
    tab: KbNavTab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: KbNavTab,
    onSelect: (KbNavTab) -> Unit,
) {
    val label = when (tab) {
        KbNavTab.TONIGHT -> stringResource(R.string.nav_tonight)
        KbNavTab.PROGRESS -> stringResource(R.string.nav_progress)
        KbNavTab.EXAMS -> stringResource(R.string.nav_exams)
        KbNavTab.SETTINGS -> stringResource(R.string.nav_settings)
    }

    NavigationBarItem(
        selected = active == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LocalKbColors.current.primary,
            selectedTextColor = LocalKbColors.current.primary,
            unselectedIconColor = LocalKbColors.current.inkFaint,
            unselectedTextColor = LocalKbColors.current.inkFaint,
            indicatorColor = LocalKbColors.current.surface,
        ),
    )
}
