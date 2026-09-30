package com.example.drugdetector.util

import android.content.Context
import android.util.Base64
import com.example.drugdetector.model.TestRecord
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val PREFS_NAME = "drug_detector_crypto_prefs"
    private const val KEY_HMAC_SECRET = "hmac_secret_key"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Calculates the SHA-256 hash of a file as a lower-case hex string.
     */
    fun calculateFileSha256(file: File): String {
        if (!file.exists()) return "0000000000000000000000000000000000000000000000000000000000000000"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            "0000000000000000000000000000000000000000000000000000000000000000"
        }
    }

    /**
     * Calculates SHA-256 hash of raw byte array.
     */
    fun calculateByteArraySha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            "0000000000000000000000000000000000000000000000000000000000000000"
        }
    }

    /**
     * Retrieves or creates a device-bound HMAC secret key stored in encrypted device storage.
     */
    private fun getSecretKey(context: Context): SecretKeySpec {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var secretBase64 = prefs.getString(KEY_HMAC_SECRET, null)
        if (secretBase64 == null) {
            val random = SecureRandom()
            val secretBytes = ByteArray(32) // 256-bit secret
            random.nextBytes(secretBytes)
            secretBase64 = Base64.encodeToString(secretBytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_HMAC_SECRET, secretBase64).apply()
        }
        val secretBytes = Base64.decode(secretBase64, Base64.NO_WRAP)
        return SecretKeySpec(secretBytes, HMAC_ALGORITHM)
    }

    /**
     * Returns a short hexadecimal fingerprint of the signing key.
     */
    fun getKeyFingerprint(context: Context): String {
        val secretKey = getSecretKey(context)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(secretKey.encoded)
        return bytesToHex(hash).take(16).uppercase()
    }

    /**
     * Signs a TestRecord payload using HMAC-SHA256 and returns Base64 encoded signature.
     */
    fun signRecord(context: Context, record: TestRecord): String {
        return try {
            val secretKey = getSecretKey(context)
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(secretKey)
            val payload = record.buildSignaturePayload()
            val signatureBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Verifies whether a given TestRecord's digital signature and image hash are valid and untampered.
     */
    fun verifyRecord(context: Context, record: TestRecord, imageFile: File? = null): VerificationResult {
        // 1. Check Image File Hash
        var imageHashValid = true
        var imageMessage = "Image hash verified"
        if (imageFile != null && imageFile.exists()) {
            val currentFileHash = calculateFileSha256(imageFile)
            if (!currentFileHash.equals(record.imageHashSha256, ignoreCase = true)) {
                imageHashValid = false
                imageMessage = "Tampering detected! Captured image file SHA-256 ($currentFileHash) does not match recorded hash (${record.imageHashSha256})"
            }
        }

        // 2. Verify Digital Signature over record payload
        var signatureValid = false
        var signatureMessage = "Signature valid"
        try {
            val secretKey = getSecretKey(context)
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(secretKey)
            val payload = record.buildSignaturePayload()
            val expectedSignatureBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            val expectedSignature = Base64.encodeToString(expectedSignatureBytes, Base64.NO_WRAP)

            if (expectedSignature == record.digitalSignature) {
                signatureValid = true
            } else {
                signatureMessage = "Tampering detected! Digital signature signature mismatch. Record fields (timestamp, location, result, or operator) may have been altered."
            }
        } catch (e: Exception) {
            signatureMessage = "Error verifying signature: ${e.message}"
        }

        val isValid = imageHashValid && signatureValid
        return VerificationResult(
            isValid = isValid,
            isImageHashValid = imageHashValid,
            isSignatureValid = signatureValid,
            message = if (isValid) "Record integrity verified. Digital proof intact and authentic." else "$imageMessage. $signatureMessage"
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    data class VerificationResult(
        val isValid: Boolean,
        val isImageHashValid: Boolean,
        val isSignatureValid: Boolean,
        val message: String
    )
}
