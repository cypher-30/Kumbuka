package dev.kumbuka.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbColors

/** Figma "dots" component from the onboarding screens: active dot widens to 20dp. */
@Composable
fun PagerDots(count: Int, activeIndex: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val isActive = index == activeIndex
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 20.dp else 8.dp)
                    .background(
                        color = if (isActive) KbColors.primary else KbColors.border,
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}
