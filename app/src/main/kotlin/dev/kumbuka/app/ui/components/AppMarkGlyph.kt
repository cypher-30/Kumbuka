package dev.kumbuka.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbColors

/**
 * The Kumbuka app-mark (Figma node 1:5554): a forgetting-curve peak with a
 * dot, drawn to match the launcher icon's vector drawable exactly so both
 * come from the same shape definition rather than two hand-maintained copies.
 * Native viewBox is 52x52.
 */
@Composable
fun AppMarkGlyph(size: Dp = 38.dp, color: androidx.compose.ui.graphics.Color = KbColors.accent, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 52f
        val strokeWidth = 4.875f * scale

        val curve = Path().apply {
            moveTo(4.875f * scale, 37.375f * scale)
            cubicTo(
                14.625f * scale, 37.375f * scale,
                16.25f * scale, 16.25f * scale,
                26f * scale, 16.25f * scale,
            )
            cubicTo(
                35.75f * scale, 16.25f * scale,
                37.375f * scale, 37.375f * scale,
                47.125f * scale, 37.375f * scale,
            )
        }
        drawPath(
            path = curve,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        drawCircle(
            color = color,
            radius = 4.469f * scale,
            center = Offset(47.125f * scale, 37.375f * scale),
        )
    }
}
