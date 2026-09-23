package com.moblin.android.platform.simd

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SimdTest {
    @Test
    fun unlabeledInitializerTakesColumns() {
        val c0 = SIMD4(1f, 2f, 3f, 4f)
        val c1 = SIMD4(5f, 6f, 7f, 8f)
        val c2 = SIMD4(9f, 10f, 11f, 12f)
        val c3 = SIMD4(13f, 14f, 15f, 16f)
        val matrix = simd_float4x4(c0, c1, c2, c3)
        assertEquals(c0, matrix[0])
        assertEquals(c1, matrix[1])
        assertEquals(c2, matrix[2])
        assertEquals(c3, matrix[3])
        assertEquals(matrix, simd_float4x4(columns = listOf(c0, c1, c2, c3)))
    }

    @Test
    fun rowsInitializerSubscriptReturnsColumns() {
        val matrix = float3x3(rows = listOf(SIMD3(1f, 2f, 3f), SIMD3(4f, 5f, 6f), SIMD3(7f, 8f, 9f)))
        assertEquals(SIMD3(1f, 4f, 7f), matrix[0])
        assertEquals(SIMD3(2f, 5f, 8f), matrix[1])
        assertEquals(SIMD3(3f, 6f, 9f), matrix[2])
        assertEquals(SIMD3(14f, 32f, 50f), matrix * SIMD3(1f, 2f, 3f))
    }

    @Test
    fun matrixProductMatchesRowsTimesColumns() {
        val a = float3x3(rows = listOf(SIMD3(1f, 2f, 0f), SIMD3(0f, 1f, 0f), SIMD3(0f, 0f, 2f)))
        val b = float3x3(rows = listOf(SIMD3(0f, 1f, 0f), SIMD3(1f, 0f, 0f), SIMD3(0f, 0f, 1f)))
        val product = a * b
        assertEquals(
            float3x3(rows = listOf(SIMD3(2f, 1f, 0f), SIMD3(1f, 0f, 0f), SIMD3(0f, 0f, 2f))),
            product
        )
    }

    @Test
    fun distanceAndLength() {
        assertEquals(5f, length(SIMD2(3f, 4f)))
        assertEquals(5f, distance(SIMD2(1f, 1f), SIMD2(4f, 5f)))
        assertEquals(3f, length(SIMD3(1f, 2f, 2f)))
        assertEquals(SIMD2(-3f, -4f), SIMD2(1f, 1f) - SIMD2(4f, 5f))
        assertEquals(SIMD3(0.5f, 1f, 1.5f), SIMD3(1f, 2f, 3f) / 2f)
        assertEquals(SIMD3(0f, 0f, 1f), normalize(SIMD3(0f, 0f, 5f)))
        assertEquals(32f, dot(SIMD3(1f, 2f, 3f), SIMD3(4f, 5f, 6f)))
    }

    @Test
    fun negativeZeroEquality() {
        assertEquals(SIMD2(0f, 0f), SIMD2(-0f, 0f))
        assertEquals(SIMD2(0f, 0f).hashCode(), SIMD2(-0f, 0f).hashCode())
        assertTrue(SIMD4.zero == SIMD4(-0f, -0f, -0f, -0f))
    }
}
