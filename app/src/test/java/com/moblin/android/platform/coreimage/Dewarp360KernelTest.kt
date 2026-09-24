package com.moblin.android.platform.coreimage

import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class Dewarp360KernelTest {
    @Test
    fun dewarpKernelIsRegisteredAsAWarp() {
        val glsl = assertNotNull(CIKernelLibrary.glsl("dewarp360", isWarp = true))
        assertTrue(glsl.contains("vec2 dewarp360("))
        assertTrue(CIKernelLibrary.glsl("dewarp360", isWarp = false) == null)
    }
}
