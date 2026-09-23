package dev.kumbuka.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbBrand

/**
 * The Kumbuka app-mark (Figma node 1:5554): a forgetting-curve peak with a
 * dot, drawn to match the launcher icon's vector drawable exactly so both
 * come from the same shape definition rather than two hand-maintained copies.
 * Native viewBox is 52x52.
 *
 * [drawProgress] trims the curve to its first [0, drawProgress] fraction
 * (Compose has no stroke-dashoffset equivalent, so PathMeasure.getSegment
 * stands in for it) - used by the splash screen to draw the curve in over
 * time. The dot only appears once the curve has fully drawn.
 */
@Composable
fun AppMarkGlyph(
    size: Dp = 38.dp,
    color: androidx.compose.ui.graphics.Color = KbBrand.accent,
    modifier: Modifier = Modifier,
    drawProgress: Float = 1f,
) {
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
        val visibleCurve = if (drawProgress >= 1f) {
            curve
        } else {
            val measure = PathMeasure().apply { setPath(curve, forceClosed = false) }
            Path().also { measure.getSegment(0f, measure.length * drawProgress, it, startWithMoveTo = true) }
        }
        drawPath(
            path = visibleCurve,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        if (drawProgress >= 1f) {
            drawCircle(
                color = color,
                radius = 4.469f * scale,
                center = Offset(47.125f * scale, 37.375f * scale),
            )
        }
    }
}
