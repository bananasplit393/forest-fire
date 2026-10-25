package com.example.forestfire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.example.forestfire.R

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int) = Font(
    resId = R.font.archivo,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** One grotesque for everything; hierarchy comes from size and weight only. */
val Archivo = FontFamily(archivo(400), archivo(600), archivo(700), archivo(800), archivo(900))

private val Base = Typography()
private val Display = FontWeight(800)
private val Title = FontWeight(700)
private val Label = FontWeight(600)

internal val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        letterSpacing = (-0.03).em,
    ),
    displayMedium = Base.displayMedium.copy(fontFamily = Archivo, fontWeight = Display),
    displaySmall = Base.displaySmall.copy(fontFamily = Archivo, fontWeight = Display),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Archivo, fontWeight = Display),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Archivo, fontWeight = Display),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Archivo, fontWeight = Display),
    titleLarge = Base.titleLarge.copy(fontFamily = Archivo, fontWeight = Title),
    titleMedium = Base.titleMedium.copy(fontFamily = Archivo, fontWeight = Title),
    titleSmall = Base.titleSmall.copy(fontFamily = Archivo, fontWeight = Title),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Archivo),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Archivo),
    bodySmall = Base.bodySmall.copy(fontFamily = Archivo),
    labelLarge = Base.labelLarge.copy(fontFamily = Archivo, fontWeight = Label),
    labelMedium = Base.labelMedium.copy(fontFamily = Archivo, fontWeight = Label),
    labelSmall = Base.labelSmall.copy(fontFamily = Archivo, fontWeight = Label),
)

/** Figures that update in place (risk, distance) keep a fixed width so nothing shifts. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
