package com.example.forestfire.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.staticCompositionLocalOf

/** Material 3 duration and easing tokens: one source of truth for every animation. */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

/** True when the system "Remove animations" setting is on; every animation then snaps. */
val LocalReducedMotion = staticCompositionLocalOf { false }
