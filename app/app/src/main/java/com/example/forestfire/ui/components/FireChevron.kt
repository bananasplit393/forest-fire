package com.example.forestfire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.forestfire.ui.format.shortestRotationTarget
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.LocalReducedMotion

/**
 * A reflective chevron pointing at the fire. [rotation] is degrees clockwise from "up".
 * It turns the short way round on a spring; interrupted turns continue from where it is.
 */
@Composable
fun FireChevron(rotation: Float, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val angle = remember { Animatable(rotation) }
    LaunchedEffect(rotation) {
        val target = shortestRotationTarget(angle.value, rotation)
        if (reduced) {
            angle.snapTo(target)
        } else {
            angle.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
    }
    val body = MaterialTheme.colorScheme.onSurface
    val stripe = ForestFireTheme.stripe

    Canvas(modifier.size(240.dp).graphicsLayer { rotationZ = angle.value }) {
        val w = size.width
        val h = size.height
        val chevron = Path().apply {
            moveTo(w / 2, 0f)
            lineTo(w * 0.86f, h * 0.86f)
            lineTo(w / 2, h * 0.64f)
            lineTo(w * 0.14f, h * 0.86f)
            close()
        }
        drawPath(chevron, body)
        clipPath(chevron) {
            drawRect(stripe.lime, Offset(0f, h * 0.44f), Size(w, h * 0.035f))
            drawRect(stripe.silver, Offset(0f, h * 0.475f), Size(w, h * 0.05f))
            drawRect(stripe.lime, Offset(0f, h * 0.525f), Size(w, h * 0.035f))
        }
    }
}
