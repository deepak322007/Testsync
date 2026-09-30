package com.example.drugdetector

import com.example.drugdetector.model.ResultCategory
import com.example.drugdetector.model.TestRecord
import com.example.drugdetector.util.CryptoUtils
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun testByteArraySha256() {
        val sampleBytes = "DrugDetectorProofTest".toByteArray()
        val hash = CryptoUtils.calculateByteArraySha256(sampleBytes)
        assertNotNull(hash)
        assertEquals(64, hash.length) // 256-bit hex is 64 chars
    }

    @Test
    fun testSignaturePayloadConsistency() {
        val record1 = TestRecord(
            id = "TEST-100",
            timestampMs = 1700000000000L,
            operatorId = "OFFICER-01",
            agency = "PD",
            kitId = "marquis_opiates",
            kitName = "Marquis",
            targetSubstance = "Heroin",
            resultCategory = ResultCategory.POSITIVE,
            confidencePercent = 95,
            latitude = 37.7749,
            longitude = -122.4194,
            imagePath = "/tmp/img.jpg",
            imageHashSha256 = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
            calibratedRgbHex = "#5A0A6E"
        )

        val payload1 = record1.buildSignaturePayload()

        val recordAltered = record1.copy(resultCategory = ResultCategory.NEGATIVE)
        val payloadAltered = recordAltered.buildSignaturePayload()

        assertNotEquals(payload1, payloadAltered)
    }

    private fun assertNotNull(obj: Any?) {
        Assert.assertNotNull(obj)
    }
}
