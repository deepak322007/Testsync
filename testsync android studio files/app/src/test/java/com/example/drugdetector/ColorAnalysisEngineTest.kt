package com.example.drugdetector

import com.example.drugdetector.model.TestKitProfile
import com.example.drugdetector.util.ColorAnalysisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ColorAnalysisEngineTest {

    @Test
    fun testRgbToHex() {
        val hex = ColorAnalysisEngine.rgbToHex(intArrayOf(255, 0, 128))
        assertEquals("#FF0080", hex)
    }

    @Test
    fun testTestKitProfileDefinitions() {
        val marquis = TestKitProfile.STANDARD_KITS.first { it.id == "marquis_opiates" }
        assertEquals("Marquis Reagent", marquis.name)
        assertEquals("Heroin / Morphine", marquis.targetSubstance)

        val scott = TestKitProfile.STANDARD_KITS.first { it.id == "scott_cocaine" }
        assertEquals("Scott Reagent (Cobalt Thiocyanate)", scott.name)
        assertEquals("Cocaine", scott.targetSubstance)
    }
}
