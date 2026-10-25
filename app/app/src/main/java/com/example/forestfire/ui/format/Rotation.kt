package com.example.forestfire.ui.format

/**
 * The angle to animate to so an arrow at [current] turns the short way to [target]
 * (e.g. 350° → 10° turns +20°, not -340°). Keeps [current]'s accumulated turns.
 */
fun shortestRotationTarget(current: Float, target: Float): Float {
    val delta = ((target - current) % 360f + 540f) % 360f - 180f
    return current + delta
}
