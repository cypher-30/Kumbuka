package dev.kumbuka.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kumbuka.app.ui.theme.LocalKbColors

/**
 * Pill-shaped segmented control (language, theme, session length, Dates/Results).
 * The selected segment gets the tonal fill, bold label and - when [showCheck] -
 * a leading check, so selection never relies on colour alone.
 */
@Composable
fun KbSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    showCheck: Boolean = true,
    labelSize: Int = 14,
) {
    val colors = LocalKbColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, colors.outlineStrong, shape),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) colors.primaryTint else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selected && showCheck) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimaryTint, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = labelSize.sp, fontWeight = FontWeight.Bold),
                        color = if (selected) colors.onPrimaryTint else colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Square checkbox drawn to the design: 22dp, 6dp corners, 2dp outline that fills with Clay when checked. */
@Composable
fun KbCheckBox(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(shape)
            .background(if (checked) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, if (checked) colors.primary else colors.outlineStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}
