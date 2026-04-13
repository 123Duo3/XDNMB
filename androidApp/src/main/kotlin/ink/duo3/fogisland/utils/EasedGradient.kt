package ink.duo3.fogisland.utils

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Creates a vertical gradient brush using a smoothstep curve:
 * alpha(t) = (1 - t)^2 * (1 + 2t)
 *
 * Both ends have zero slope, so the onset from transparent is gradual and the
 * opaque area at the top is relatively flat.
 *
 * @param color      The scrim color (alpha channel is overridden by the curve).
 * @param flatFraction Fraction of the total height that stays at full opacity before the
 *                   smoothstep transition begins. Must be in [0, 1]. Default is 0 (pure
 *                   gradient with no flat section). Example: 0.6 means the top 60% is
 *                   solid and the remaining 40% fades to transparent.
 */
fun smoothScrimBrush(color: Color, flatFraction: Float = 0f): Brush {
    require(flatFraction in 0f..1f) { "flatFraction must be in [0, 1]" }
    val steps = 16
    val maxAlpha = color.alpha
    val colorStops = buildList {
        // Flat region: two stops are enough for linear interpolation to stay at maxAlpha.
        if (flatFraction > 0f) {
            add(0f to color)
        }
        // Smoothstep transition: 17 stops (i = 0..steps) evenly spaced over the
        // remaining [flatFraction, 1] range. The curve is scaled by maxAlpha so that
        // color.alpha acts as the peak opacity. At i=0, position == flatFraction and
        // alpha == maxAlpha, cleanly joining the flat region (or the very top when
        // flatFraction == 0).
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val position = flatFraction + t * (1f - flatFraction)
            val alpha = maxAlpha * (1f - t) * (1f - t) * (1f + 2f * t)
            add(position to color.copy(alpha = alpha))
        }
    }.toTypedArray()
    return Brush.verticalGradient(colorStops = colorStops)
}
