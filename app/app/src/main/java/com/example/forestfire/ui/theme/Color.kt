package com.example.forestfire.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Turnout-gear materials: Nomex gold shell, charcoal, retro-reflective lime and silver tape.
private val Gold = Color(0xFFC9A65A)
private val GoldDeep = Color(0xFFB8954B)
private val GoldDeeper = Color(0xFFA9873F)
private val GoldPale = Color(0xFFE9D6A6)
private val Charcoal = Color(0xFF1C1F22)
private val NightGround = Color(0xFF15181B)
private val Bone = Color(0xFFECE7DA)
private val Lime = Color(0xFFDDF23A)

internal val DayColors = lightColorScheme(
    primary = Charcoal,
    onPrimary = GoldPale,
    primaryContainer = GoldDeeper,
    onPrimaryContainer = Charcoal,
    secondary = Lime,
    onSecondary = Charcoal,
    background = Gold,
    onBackground = Charcoal,
    surface = Gold,
    onSurface = Charcoal,
    surfaceVariant = GoldDeep,
    onSurfaceVariant = Color(0xFF221D13),
    surfaceContainerLowest = Color(0xFFD4B46C),
    surfaceContainerLow = Color(0xFFCEAD62),
    surfaceContainer = GoldDeep,
    surfaceContainerHigh = GoldDeeper,
    // Too dark for body text; use only for fills, never behind text.
    surfaceContainerHighest = Color(0xFF9C7B36),
    error = Color(0xFF9A1219),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFD7262E),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF5A4E33),
    outlineVariant = Color(0xFFA88C4E),
)

internal val NightColors = darkColorScheme(
    primary = Gold,
    onPrimary = Charcoal,
    primaryContainer = Color(0xFF3A3220),
    onPrimaryContainer = GoldPale,
    secondary = Lime,
    onSecondary = Charcoal,
    background = NightGround,
    onBackground = Bone,
    surface = NightGround,
    onSurface = Bone,
    surfaceVariant = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFFB9B2A2),
    surfaceContainerLowest = Color(0xFF101215),
    surfaceContainerLow = Color(0xFF1A1D21),
    surfaceContainer = Color(0xFF1F2328),
    surfaceContainerHigh = Color(0xFF272C32),
    surfaceContainerHighest = Color(0xFF30363D),
    error = Color(0xFFE5484D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFB3202A),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF6B6558),
    outlineVariant = Color(0xFF33383F),
)

/** The reflective stripe's own tokens; Material's color roles have no slot for them. */
@Immutable
data class StripeColors(
    val lime: Color,
    val silver: Color,
    val dull: Color,
    val amber: Color,
)

internal val DayStripe = StripeColors(
    lime = Lime,
    silver = Color(0xFFC7CCD3),
    dull = Color(0xFF8C8577),
    amber = Color(0xFFF2B33D),
)

internal val NightStripe = StripeColors(
    lime = Lime,
    silver = Color(0xFFE3E7EC),
    dull = Color(0xFF4A4F57),
    amber = Color(0xFFF2B33D),
)

internal val LocalStripeColors = staticCompositionLocalOf { DayStripe }
