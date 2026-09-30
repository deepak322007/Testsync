package com.example.drugdetector.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.drugdetector.model.ResultCategory
import com.example.drugdetector.model.TestKitProfile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object ColorAnalysisEngine {

    data class AnalysisResult(
        val category: ResultCategory,
        val confidencePercent: Int,
        val rawRgb: IntArray,
        val calibratedRgb: IntArray,
        val lightingQuality: String, // GOOD, FAIR, POOR
        val matchedSubstance: String,
        val colorDescription: String,
        val detailedMetrics: String
    )

    data class ReferencePatch(
        val r: Int,
        val g: Int,
        val b: Int
    )

    /**
     * Performs color calibration and classification on a captured Bitmap.
     * Extracts reference white patch and reaction zone ROI.
     */
    fun analyzeTestImage(
        bitmap: Bitmap,
        kitProfile: TestKitProfile,
        whiteRefRect: RectF = RectF(0.1f, 0.1f, 0.25f, 0.25f), // Relative coordinates [0.0..1.0]
        grayRefRect: RectF = RectF(0.1f, 0.3f, 0.25f, 0.45f),
        reactionRect: RectF = RectF(0.5f, 0.3f, 0.8f, 0.6f)
    ): AnalysisResult {
        val width = bitmap.width
        val height = bitmap.height

        // 1. Extract average RGB from Reference White, Gray, and Reaction zones
        val whitePatch = sampleAverageRgb(bitmap, whiteRefRect)
        val grayPatch = sampleAverageRgb(bitmap, grayRefRect)
        val rawReaction = sampleAverageRgb(bitmap, reactionRect)

        // 2. Compute lighting gain calibration based on Reference White Patch
        // Ideal reference white patch RGB is ~ (245, 245, 245)
        val gainR = 245.0f / max(1.0f, whitePatch.r.toFloat())
        val gainG = 245.0f / max(1.0f, whitePatch.g.toFloat())
        val gainB = 245.0f / max(1.0f, whitePatch.b.toFloat())

        // Apply gain to reaction zone
        val calR = (rawReaction.r * gainR).toInt().coerceIn(0, 255)
        val calG = (rawReaction.g * gainG).toInt().coerceIn(0, 255)
        val calB = (rawReaction.b * gainB).toInt().coerceIn(0, 255)
        val calibratedRgb = intArrayOf(calR, calG, calB)
        val rawRgb = intArrayOf(rawReaction.r, rawReaction.g, rawReaction.b)

        // 3. Assess Lighting Quality
        val whiteLuminance = (whitePatch.r * 0.299 + whitePatch.g * 0.587 + whitePatch.b * 0.114)
        val maxGain = max(gainR, max(gainG, gainB))
        val minGain = min(gainR, min(gainG, gainB))
        val gainSpread = maxGain / max(0.1f, minGain)

        val lightingQuality = when {
            whiteLuminance < 60 || whiteLuminance > 250 || gainSpread > 2.5f -> "POOR"
            gainSpread > 1.6f || whiteLuminance < 110 -> "FAIR"
            else -> "GOOD"
        }

        // 4. Convert Calibrated RGB to HSV
        val hsvCalibrated = FloatArray(3)
        Color.RGBToHSV(calR, calG, calB, hsvCalibrated)
        val hueCal = hsvCalibrated[0]
        val satCal = hsvCalibrated[1]
        val valCal = hsvCalibrated[2]

        // 5. Compare against Test Kit Profile
        val posRgb = kitProfile.positiveRgb
        val negRgb = kitProfile.negativeRgb

        val hsvPositive = FloatArray(3)
        Color.RGBToHSV(posRgb[0], posRgb[1], posRgb[2], hsvPositive)

        val hsvNegative = FloatArray(3)
        Color.RGBToHSV(negRgb[0], negRgb[1], negRgb[2], hsvNegative)

        val distPosRgb = rgbDistance(calibratedRgb, posRgb)
        val distNegRgb = rgbDistance(calibratedRgb, negRgb)

        val hueDiffPos = hueDifference(hueCal, hsvPositive[0])
        val hueDiffNeg = hueDifference(hueCal, hsvNegative[0])

        val category: ResultCategory
        val confidence: Int
        val colorDesc: String

        if (lightingQuality == "POOR" && satCal < 0.10f) {
            category = ResultCategory.INCONCLUSIVE
            confidence = 45
            colorDesc = "Inconclusive due to inadequate lighting calibration"
        } else if (hueDiffPos <= kitProfile.hueTolerance && distPosRgb < 140.0) {
            category = ResultCategory.POSITIVE
            val distFactor = (1.0 - (distPosRgb / 180.0)).coerceIn(0.6, 0.99)
            val hueFactor = (1.0 - (hueDiffPos / kitProfile.hueTolerance)).coerceIn(0.5, 1.0)
            val lightingBonus = if (lightingQuality == "GOOD") 0.05 else 0.0
            confidence = ((distFactor * 0.5 + hueFactor * 0.4 + lightingBonus) * 100).toInt().coerceIn(60, 99)
            colorDesc = "${kitProfile.positiveColorName} reaction detected"
        } else if (distNegRgb < 120.0 || (hueDiffNeg <= kitProfile.hueTolerance && satCal < 0.35f)) {
            category = ResultCategory.NEGATIVE
            val distFactor = (1.0 - (distNegRgb / 160.0)).coerceIn(0.6, 0.98)
            confidence = (distFactor * 100).toInt().coerceIn(65, 98)
            colorDesc = "${kitProfile.negativeColorName} baseline (No color change)"
        } else if (distPosRgb < distNegRgb && distPosRgb < 170.0) {
            category = ResultCategory.POSITIVE
            confidence = 65
            colorDesc = "Moderate match to ${kitProfile.positiveColorName}"
        } else {
            category = ResultCategory.INCONCLUSIVE
            confidence = 50
            colorDesc = "Ambiguous color reaction outside calibrated thresholds"
        }

        val metrics = "Raw RGB: (${rawReaction.r},${rawReaction.g},${rawReaction.b}) | " +
                "Calibrated RGB: ($calR,$calG,$calB) | " +
                "HSV: (${hueCal.toInt()}°, ${(satCal * 100).toInt()}%, ${(valCal * 100).toInt()}%) | " +
                "Lighting: $lightingQuality (Gains: R=%.2f, G=%.2f, B=%.2f)".format(gainR, gainG, gainB)

        return AnalysisResult(
            category = category,
            confidencePercent = confidence,
            rawRgb = rawRgb,
            calibratedRgb = calibratedRgb,
            lightingQuality = lightingQuality,
            matchedSubstance = if (category == ResultCategory.POSITIVE) kitProfile.targetSubstance else "None detected",
            colorDescription = colorDesc,
            detailedMetrics = metrics
        )
    }

    private fun sampleAverageRgb(bitmap: Bitmap, rect: RectF): ReferencePatch {
        val startX = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val endX = (rect.right * bitmap.width).toInt().coerceIn(startX + 1, bitmap.width)
        val startY = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val endY = (rect.bottom * bitmap.height).toInt().coerceIn(startY + 1, bitmap.height)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        // Step by 2 pixels for performance
        for (x in startX until endX step 2) {
            for (y in startY until endY step 2) {
                val pixel = bitmap.getPixel(x, y)
                totalR += Color.red(pixel)
                totalG += Color.green(pixel)
                totalB += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) return ReferencePatch(128, 128, 128)
        return ReferencePatch((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
    }

    private fun rgbDistance(rgb1: IntArray, rgb2: IntArray): Double {
        val dr = (rgb1[0] - rgb2[0]).toDouble()
        val dg = (rgb1[1] - rgb2[1]).toDouble()
        val db = (rgb1[2] - rgb2[2]).toDouble()
        return sqrt(dr.pow(2) + dg.pow(2) + db.pow(2))
    }

    private fun hueDifference(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    fun rgbToHex(rgb: IntArray): String {
        return "#%02X%02X%02X".format(
            rgb[0].coerceIn(0, 255),
            rgb[1].coerceIn(0, 255),
            rgb[2].coerceIn(0, 255)
        )
    }

    /**
     * Creates a synthetic test result bitmap for testing and simulation.
     */
    fun createSyntheticTestBitmap(
        reactionRgb: IntArray,
        whiteRefRgb: IntArray = intArrayOf(245, 245, 245),
        grayRefRgb: IntArray = intArrayOf(128, 128, 128)
    ): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background (Test Kit Card Tray)
        val bgPaint = Paint().apply { color = Color.parseColor("#E0E0E0") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title / Card Frame
        val framePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRoundRect(40f, 40f, width - 40f, height - 40f, 20f, 20f, framePaint)

        // Reference Color Card Block (Left Side)
        val refCardPaint = Paint().apply { color = Color.parseColor("#333333") }
        canvas.drawRect(80f, 80f, 240f, 520f, refCardPaint)

        // Reference White Patch
        val whitePaint = Paint().apply {
            color = Color.rgb(whiteRefRgb[0], whiteRefRgb[1], whiteRefRgb[2])
        }
        canvas.drawRect(100f, 100f, 220f, 220f, whitePaint)

        // Reference Gray Patch
        val grayPaint = Paint().apply {
            color = Color.rgb(grayRefRgb[0], grayRefRgb[1], grayRefRgb[2])
        }
        canvas.drawRect(100f, 240f, 220f, 360f, grayPaint)

        // Reference RGB Swatches
        val redPaint = Paint().apply { color = Color.RED }
        val greenPaint = Paint().apply { color = Color.GREEN }
        val bluePaint = Paint().apply { color = Color.BLUE }
        canvas.drawRect(100f, 380f, 135f, 500f, redPaint)
        canvas.drawRect(142f, 380f, 177f, 500f, greenPaint)
        canvas.drawRect(185f, 380f, 220f, 500f, bluePaint)

        // Test Tube / Ampoule Reaction Area (Right Side)
        val tubePaint = Paint().apply { color = Color.parseColor("#B0BEC5") }
        canvas.drawRoundRect(360f, 120f, 680f, 480f, 30f, 30f, tubePaint)

        // Chemical Reaction Fluid
        val fluidPaint = Paint().apply {
            color = Color.rgb(reactionRgb[0], reactionRgb[1], reactionRgb[2])
        }
        canvas.drawRoundRect(380f, 180f, 660f, 460f, 20f, 20f, fluidPaint)

        return bitmap
    }
}
