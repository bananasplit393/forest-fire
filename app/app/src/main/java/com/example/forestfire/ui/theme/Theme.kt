package com.example.forestfire.ui.theme

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Day: Nomex gold. Night: charcoal with the reflective tape at full brightness. */
@Composable
fun ForestFireTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(
        LocalStripeColors provides if (darkTheme) NightStripe else DayStripe,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) NightColors else DayColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object ForestFireTheme {
    val stripe: StripeColors
        @Composable @ReadOnlyComposable get() = LocalStripeColors.current
}

/** Draws behind the system bars; bar icons follow the day/night theme. */
fun ComponentActivity.setupEdgeToEdge() {
    enableEdgeToEdge()
}
