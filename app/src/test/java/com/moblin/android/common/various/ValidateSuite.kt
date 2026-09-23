package com.moblin.android.common.various

import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ValidateSuite {
    @Test
    fun whipUrlValidation() {
        assertNull(isValidUrl("whips://whip.example.com/live/123"))
        assertNull(isValidUrl("whip://whip.example.com/live/123"))
    }

    @Test
    fun mobcamUrlValidation() {
        assertNull(isValidUrl("mobcam://localhost:7790"))
        assertNull(isValidUrl("mobcam://127.0.0.1:7790"))
        assertNull(isValidUrl("mobcam://LocalHost:7790"))
        assertNotNull(isValidUrl("mobcam://localhost"))
        assertNotNull(isValidUrl("mobcam://localhost:70000"))
        assertNotNull(isValidUrl("mobcam://192.168.1.5:7790"))
        assertNotNull(isValidUrl("mobcam://example.com:7790"))
        assertNotNull(isValidUrl("mobcam://[::1]:7790"))
        assertNotNull(isValidUrl("mobcam://localhost:7790", listOf("srt")))
    }
}
