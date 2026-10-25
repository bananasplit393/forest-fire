package com.example.forestfire.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.forestfire.ui.format.StripeState
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.LocalReducedMotion
import com.example.forestfire.ui.theme.Motion

/**
 * The lime-silver-lime retro-reflective tape from a turnout jacket. It always shows duty state.
 * When [glintKey] changes to a non-null value a headlight glint sweeps across it once.
 * [stateDescription] is exposed to TalkBack as the stripe's state. When it is null the stripe
 * stays decorative: the screen's text carries the same state.
 */
@Composable
fun ReflectiveStripe(
    state: StripeState,
    modifier: Modifier = Modifier,
    thickness: Dp = 34.dp,
    glintKey: Any? = null,
    stateDescription: String? = null,
) {
    val stripe = ForestFireTheme.stripe
    val reduced = LocalReducedMotion.current
    val (outerTarget, innerTarget) = when (state) {
        StripeState.Lit -> stripe.lime to stripe.silver
        StripeState.Pending -> stripe.amber to stripe.silver
        StripeState.Fault -> MaterialTheme.colorScheme.error to stripe.silver
        StripeState.Off -> stripe.dull to stripe.dull.copy(alpha = 0.6f)
    }
    val fade = tween<Color>(if (reduced) 0 else Motion.MEDIUM)
    val outer by animateColorAsState(outerTarget, fade, label = "stripeOuter")
    val inner by animateColorAsState(innerTarget, fade, label = "stripeInner")

    val glint = remember { Animatable(-1f) }
    LaunchedEffect(glintKey) {
        glint.snapTo(-1f)
        if (glintKey != null && !reduced) {
            glint.snapTo(-0.2f)
            glint.animateTo(1.2f, tween(Motion.LONG * 2, easing = Motion.Emphasized))
            glint.snapTo(-1f)
        }
    }

    val described = if (stateDescription != null) {
        modifier.semantics { this.stateDescription = stateDescription }
    } else {
        modifier
    }
    Canvas(described.fillMaxWidth().height(thickness)) {
        val unit = size.height / 34f
        drawRect(outer, size = Size(size.width, 10 * unit))
        drawRect(inner, topLeft = Offset(0f, 10 * unit), size = Size(size.width, 14 * unit))
        drawRect(outer, topLeft = Offset(0f, 24 * unit), size = Size(size.width, 10 * unit))
        val position = glint.value // read in the draw phase only
        if (position > -0.5f) {
            val x = position * size.width
            val half = 24.dp.toPx()
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent),
                    start = Offset(x - half, 0f),
                    end = Offset(x + half, size.height),
                ),
            )
        }
    }
}
