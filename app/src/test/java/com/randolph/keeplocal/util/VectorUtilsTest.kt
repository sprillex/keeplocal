package com.randolph.keeplocal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorUtilsTest {

    @Test
    fun testMrlSlicingAndL2Normalization() {
        val raw768 = FloatArray(768) { (it + 1).toFloat() }
        val sliced128 = VectorUtils.sliceAndNormalize(raw768, 128)

        assertEquals(128, sliced128.size)

        // Verify L2 norm equals 1.0 (unit length)
        var sumSquares = 0.0f
        for (v in sliced128) {
            sumSquares += v * v
        }
        val norm = kotlin.math.sqrt(sumSquares)
        assertEquals(1.0f, norm, 0.0001f)
    }

    @Test
    fun testDotProductOrthogonalAndIdentical() {
        val vecA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecB = floatArrayOf(0.0f, 1.0f, 0.0f)
        val vecIdentical = floatArrayOf(1.0f, 0.0f, 0.0f)

        assertEquals(0.0f, VectorUtils.dotProduct(vecA, vecB), 0.0001f)
        assertEquals(1.0f, VectorUtils.dotProduct(vecA, vecIdentical), 0.0001f)
    }

    @Test
    fun testByteArrayConversionRoundtrip() {
        val original = floatArrayOf(0.123f, -0.456f, 0.789f, 1.0f, -1.0f)
        val bytes = VectorUtils.floatArrayToByteArray(original)
        assertEquals(original.size * 4, bytes.size)

        val restored = VectorUtils.byteArrayToFloatArray(bytes)
        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i], restored[i], 0.0001f)
        }
    }
}
