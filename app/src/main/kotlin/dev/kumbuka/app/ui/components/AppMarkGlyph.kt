package dev.kumbuka.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.ui.theme.KbBrand

/**
 * The Kumbuka app-mark, logo option C "Jua" (Kumbuka Brand.dc.html): a horizon
 * curve with a rising sun. Same geometry as the launcher icon and the
 * animated splash drawables (64-unit grid: curve `M8 46 C 20 30 44 30 56 46`,
 * stroke 5.5, sun at (32, 22) r8), so every surface shares one definition.
 *
 * [drawProgress] trims the curve to its first fraction (Compose has no
 * stroke-dashoffset, so PathMeasure.getSegment stands in); [sunScale] scales
 * the sun about its centre (0 = hidden, 1 = resting size, >1 = the pop
 * overshoot). Defaults draw the finished mark.
 */
@Composable
fun AppMarkGlyph(
    size: Dp = 38.dp,
    color: Color = KbBrand.accent,
    modifier: Modifier = Modifier,
    drawProgress: Float = 1f,
    sunColor: Color = color,
    sunScale: Float = 1f,
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.width / 64f

        val curve = Path().apply {
            moveTo(8f * u, 46f * u)
            cubicTo(20f * u, 30f * u, 44f * u, 30f * u, 56f * u, 46f * u)
        }
        val visibleCurve = if (drawProgress >= 1f) {
            curve
        } else {
            val measure = PathMeasure().apply { setPath(curve, forceClosed = false) }
            Path().also { measure.getSegment(0f, measure.length * drawProgress.coerceAtLeast(0f), it, startWithMoveTo = true) }
        }
        if (drawProgress > 0f) {
            drawPath(
                path = visibleCurve,
                color = color,
                style = Stroke(width = 5.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (sunScale > 0f) {
            drawCircle(
                color = sunColor,
                radius = 8f * u * sunScale,
                center = Offset(32f * u, 22f * u),
            )
        }
    }
}
