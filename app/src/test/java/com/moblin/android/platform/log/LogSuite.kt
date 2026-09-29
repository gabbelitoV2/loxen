package com.moblin.android.platform.log

import com.moblin.android.various.logger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class LogSuite {
    private val lines = mutableListOf<String>()
    private val timestamped = Regex("""^\d\d:\d\d:\d\d\.\d{3} (.*)$""")

    @Before
    fun setUp() {
        logger.handler = { lines.add(it) }
        logger.debugEnabled = false
    }

    @After
    fun tearDown() {
        logger.handler = null
        logger.debugEnabled = false
    }

    private fun messages(): List<String> = lines.map { line ->
        assertNotNull(timestamped.matchEntire(line), line).groupValues[1]
    }

    @Test
    fun infoGoesToMoblinsLogWithATimestamp() {
        Log.i("RtmpStream", "rtmp: Connected")
        Log.i("RtmpStream", null)
        assertEquals(listOf("rtmp: Connected", "null"), messages())
    }

    @Test
    fun debugGoesToMoblinsLogOnlyWhenDebugLoggingIsOn() {
        Log.d("T", "srt: Stats")
        Log.v("T", "srt: More stats")
        assertFalse(Log.isLoggable("T", Log.DEBUG))
        assertTrue(Log.isLoggable("T", Log.INFO))
        logger.debugEnabled = true
        Log.d("T", "srt: Stats")
        assertTrue(Log.isLoggable("T", Log.DEBUG))
        assertEquals(listOf("srt: Stats"), messages())
    }

    @Test
    fun errorsAndWarningsStayInLogcatLikeIosHasNoSuchLevel() {
        Log.e("T", "failed", RuntimeException())
        Log.e("T", "failed")
        Log.w("T", "careful")
        assertTrue(lines.isEmpty())
    }

    @Test
    fun theShimTakesEveryCallAndroidLogTakesAndSendsInfoAndDebugWithAThrowableToMoblinsLog() {
        logger.debugEnabled = true
        val error = RuntimeException("boom")
        val stackTrace = Log.getStackTraceString(error)
        assertTrue(stackTrace.startsWith("java.lang.RuntimeException: boom"), stackTrace)
        Log.i("T", "failed", error)
        Log.d("T", "retry", error)
        Log.v("T", "again", error)
        Log.println(Log.INFO, "T", "println info")
        Log.println(Log.DEBUG, "T", "println debug")
        Log.println(Log.VERBOSE, "T", "println verbose")
        Log.println(Log.WARN, "T", "println warn")
        Log.w("T", error)
        Log.wtf("T", "what")
        Log.wtf("T", error)
        Log.wtf("T", "what", error)
        assertEquals(
            listOf(
                "failed\n$stackTrace",
                "retry\n$stackTrace",
                "again\n$stackTrace",
                "println info",
                "println debug",
                "println verbose",
            ),
            lines.map { it.substringAfter(' ') },
        )
        val logcat = ShadowLog.getLogsForTag("T").map { it.type }
        assertEquals(listOf(Log.WARN, Log.WARN, Log.ASSERT), logcat.take(3))
        assertEquals(Log.ASSERT, logcat.last())
        assertTrue(logcat.all { it >= Log.WARN }, "$logcat")
        logger.debugEnabled = false
        Log.d("T", "hidden", error)
        Log.println(Log.DEBUG, "T", "hidden")
        assertEquals(6, lines.size)
    }

    @Test
    fun aLineLoggedWhileLoggingGoesToLogcatInsteadOfCallingItselfForever() {
        logger.debugEnabled = true
        logger.handler = { line ->
            lines.add(line)
            Log.i("RemoteControlWeb", "remote-control-web: send failed")
            Log.d("RemoteControlWeb", "remote-control-web: send failed")
        }
        Log.i("T", "first")
        Log.d("T", "second")
        assertEquals(listOf("first", "second"), messages())
        Log.i("T", "third")
        assertEquals(listOf("first", "second", "third"), messages())
        logger.handler = { throw IllegalStateException() }
        assertFailsWith<IllegalStateException> { Log.i("T", "fails") }
        logger.handler = { lines.add(it) }
        Log.i("T", "fourth")
        assertEquals(listOf("first", "second", "third", "fourth"), messages())
    }

    @Test
    fun theLoggerDoesNotLogItselfWithDebugLoggingOn() {
        logger.debugEnabled = true
        logger.info("a")
        logger.debug { "b" }
        assertEquals(listOf("a", "b"), messages())
    }
}
