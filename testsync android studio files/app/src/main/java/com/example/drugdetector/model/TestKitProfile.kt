package com.example.drugdetector.model

import androidx.compose.ui.graphics.Color

/**
 * Defines a colorimetric field test kit reagent profile with expected color reaction shifts.
 */
data class TestKitProfile(
    val id: String,
    val name: String,
    val description: String,
    val targetSubstance: String,
    val positiveColorName: String,
    val positiveRgb: IntArray, // Expected RGB [R, G, B] after positive reaction
    val negativeColorName: String,
    val negativeRgb: IntArray, // Expected RGB [R, G, B] for negative/baseline
    val hueTolerance: Float = 35f, // Tolerance in HSV Hue degrees
    val saturationMin: Float = 0.2f // Minimum saturation required for positive
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TestKitProfile
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        val STANDARD_KITS = listOf(
            TestKitProfile(
                id = "marquis_opiates",
                name = "Marquis Reagent",
                description = "Primary field test for Opiates, Heroin, Morphine, & Amphetamines",
                targetSubstance = "Heroin / Morphine",
                positiveColorName = "Deep Purple / Violet",
                positiveRgb = intArrayOf(90, 10, 110), // Purple/Violet
                negativeColorName = "Clear / Pale Yellow",
                negativeRgb = intArrayOf(230, 220, 170)
            ),
            TestKitProfile(
                id = "marquis_amphetamines",
                name = "Marquis Reagent (Speed/Meth)",
                description = "Primary field test for Amphetamine & Methamphetamine",
                targetSubstance = "Amphetamine / Methamphetamine",
                positiveColorName = "Reddish-Brown / Orange",
                positiveRgb = intArrayOf(180, 70, 20),
                negativeColorName = "Clear / Pale Yellow",
                negativeRgb = intArrayOf(230, 220, 170)
            ),
            TestKitProfile(
                id = "scott_cocaine",
                name = "Scott Reagent (Cobalt Thiocyanate)",
                description = "Presumptive color test for Cocaine HCl and Cocaine Base",
                targetSubstance = "Cocaine",
                positiveColorName = "Turquoise / Intense Blue",
                positiveRgb = intArrayOf(0, 150, 220), // Bright Blue
                negativeColorName = "Pink / Light Red",
                negativeRgb = intArrayOf(230, 150, 160)
            ),
            TestKitProfile(
                id = "duquenois_levine_cannabis",
                name = "Duquenois-Levine Reagent",
                description = "Presumptive color test for THC / Cannabis / Hashish",
                targetSubstance = "THC / Cannabis",
                positiveColorName = "Dark Blue-Violet Layer",
                positiveRgb = intArrayOf(70, 30, 140),
                negativeColorName = "Yellowish-Green",
                negativeRgb = intArrayOf(200, 210, 100)
            ),
            TestKitProfile(
                id = "mecke_ecstasy",
                name = "Mecke Reagent",
                description = "Field test for MDMA / Ecstasy & Opiates",
                targetSubstance = "MDMA / Ecstasy",
                positiveColorName = "Blue-Green to Dark Green",
                positiveRgb = intArrayOf(10, 120, 90),
                negativeColorName = "Clear / Light Brown",
                negativeRgb = intArrayOf(220, 200, 170)
            ),
            TestKitProfile(
                id = "ehrlich_psychedelics",
                name = "Ehrlich Reagent",
                description = "Field test for Indoles (LSD, Psilocybin)",
                targetSubstance = "LSD / Indoles",
                positiveColorName = "Purple / Magenta",
                positiveRgb = intArrayOf(170, 20, 150),
                negativeColorName = "Clear",
                negativeRgb = intArrayOf(240, 240, 230)
            )
        )
    }
}
