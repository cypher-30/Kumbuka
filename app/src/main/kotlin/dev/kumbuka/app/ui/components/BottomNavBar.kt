package dev.kumbuka.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbColors

enum class KbNavTab(val label: String) {
    TONIGHT("Tonight"),
    PROGRESS("Progress"),
    EXAMS("Exams"),
    SETTINGS("Settings"),
}

/** Figma "Bottom Nav" component (node 1:5546): 4 tabs, active tab tinted primary. */
@Composable
fun KbBottomNavBar(active: KbNavTab, onSelect: (KbNavTab) -> Unit) {
    Column {
        HorizontalDivider(color = KbColors.border)
        NavigationBar(containerColor = KbColors.surface, modifier = Modifier.height(74.dp)) {
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
    NavigationBarItem(
        selected = active == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, contentDescription = tab.label) },
        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = KbColors.primary,
            selectedTextColor = KbColors.primary,
            unselectedIconColor = KbColors.inkFaint,
            unselectedTextColor = KbColors.inkFaint,
            indicatorColor = KbColors.surface,
        ),
    )
}
