package com.example.drugdetector.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a tamper-evident digital record of a field colorimetric drug test.
 */
data class TestRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val operatorId: String,
    val operatorName: String = "",
    val agency: String = "",
    val kitId: String,
    val kitName: String,
    val targetSubstance: String,
    val resultCategory: ResultCategory, // POSITIVE, NEGATIVE, INCONCLUSIVE
    val confidencePercent: Int,
    val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAccuracy: Float = 0.0f,
    val locationName: String = "Unknown Location",
    val imagePath: String, // Relative or absolute file path to captured image
    val imageHashSha256: String, // SHA-256 hash of the captured image
    val rawRgbHex: String = "#FFFFFF",
    val calibratedRgbHex: String = "#FFFFFF",
    val calibrationLightingQuality: String = "GOOD", // GOOD, FAIR, POOR
    val digitalSignature: String = "", // Cryptographic HMAC-SHA256 signature
    val keyFingerprint: String = "", // Fingerprint of key used to sign
    val isSyncedToCloud: Boolean = false // Cloud sync status flag
) {
    val formattedTimestamp: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            return sdf.format(Date(timestampMs))
        }

    val isoTimestamp: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            return sdf.format(Date(timestampMs))
        }

    /**
     * Builds the standardized payload string used for signing and integrity verification.
     */
    fun buildSignaturePayload(): String {
        return listOf(
            id,
            timestampMs.toString(),
            operatorId,
            agency,
            kitId,
            resultCategory.name,
            confidencePercent.toString(),
            "%.5f".format(Locale.US, latitude),
            "%.5f".format(Locale.US, longitude),
            imageHashSha256,
            calibratedRgbHex
        ).joinToString(separator = "|")
    }
}

enum class ResultCategory {
    POSITIVE,
    NEGATIVE,
    INCONCLUSIVE
}
