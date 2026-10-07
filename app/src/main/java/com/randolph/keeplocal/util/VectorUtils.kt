package com.randolph.keeplocal.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

object VectorUtils {

    const val DEFAULT_EMBEDDING_DIM = 128

    /**
     * Slices an array down to targetDimensions (MRL - Matryoshka Representation Learning)
     * and applies Euclidean L2 normalization.
     */
    fun sliceAndNormalize(rawOutput: FloatArray, targetDimensions: Int = DEFAULT_EMBEDDING_DIM): FloatArray {
        val size = minOf(rawOutput.size, targetDimensions)
        val sliced = rawOutput.copyOfRange(0, size)

        // Compute Euclidean norm (L2 norm)
        var sumSquares = 0.0f
        for (value in sliced) {
            sumSquares += value * value
        }
        val norm = sqrt(sumSquares)

        if (norm > 0.000001f) {
            for (i in sliced.indices) {
                sliced[i] /= norm
            }
        }
        return sliced
    }

    /**
     * Computes dot product between two normalized vector float arrays.
     */
    fun dotProduct(vectorA: FloatArray, vectorB: FloatArray): Float {
        val size = minOf(vectorA.size, vectorB.size)
        var dot = 0.0f
        for (i in 0 until size) {
            dot += vectorA[i] * vectorB[i]
        }
        return dot
    }

    /**
     * Converts a FloatArray into a ByteArray blob (512 bytes for 128 FP32 floats).
     */
    fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer: FloatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(floats)
        return byteBuffer.array()
    }

    /**
     * Converts a ByteArray blob back into a FloatArray.
     */
    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = byteBuffer.asFloatBuffer()
        val floats = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floats)
        return floats
    }
}
