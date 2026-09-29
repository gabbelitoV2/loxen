package com.moblin.android.platform.log

import com.moblin.android.various.logger

object Log {
    const val VERBOSE = android.util.Log.VERBOSE
    const val DEBUG = android.util.Log.DEBUG
    const val INFO = android.util.Log.INFO
    const val WARN = android.util.Log.WARN
    const val ERROR = android.util.Log.ERROR
    const val ASSERT = android.util.Log.ASSERT

    private val inLogger = ThreadLocal.withInitial { false }

    private inline fun toLogger(logcat: () -> Int, log: () -> Unit): Int {
        if (inLogger.get()) {
            return logcat()
        }
        inLogger.set(true)
        try {
            log()
        } finally {
            inLogger.set(false)
        }
        return 0
    }

    private fun withStackTrace(msg: String?, tr: Throwable?): String = "$msg\n${getStackTraceString(tr)}"

    fun v(tag: String?, msg: String?): Int = d(tag, msg)

    fun v(tag: String?, msg: String?, tr: Throwable?): Int = d(tag, msg, tr)

    fun d(tag: String?, msg: String?): Int = toLogger({ android.util.Log.d(tag, "$msg") }) { logger.debug { "$msg" } }

    fun d(tag: String?, msg: String?, tr: Throwable?): Int = d(tag, withStackTrace(msg, tr))

    fun i(tag: String?, msg: String?): Int = toLogger({ android.util.Log.i(tag, "$msg") }) { logger.info("$msg") }

    fun i(tag: String?, msg: String?, tr: Throwable?): Int = i(tag, withStackTrace(msg, tr))

    fun w(tag: String?, msg: String?): Int = android.util.Log.w(tag, "$msg")

    fun w(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.w(tag, "$msg", tr)

    fun w(tag: String?, tr: Throwable?): Int = android.util.Log.w(tag, tr)

    fun e(tag: String?, msg: String?): Int = android.util.Log.e(tag, "$msg")

    fun e(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.e(tag, "$msg", tr)

    fun wtf(tag: String?, msg: String?): Int = android.util.Log.wtf(tag, msg)

    fun wtf(tag: String?, tr: Throwable): Int = android.util.Log.wtf(tag, tr)

    fun wtf(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.wtf(tag, msg, tr)

    fun println(priority: Int, tag: String?, msg: String?): Int = when (priority) {
        VERBOSE, DEBUG -> d(tag, msg)
        INFO -> i(tag, msg)
        else -> android.util.Log.println(priority, tag, "$msg")
    }

    fun isLoggable(tag: String?, level: Int): Boolean = level > DEBUG || logger.debugEnabled

    fun getStackTraceString(tr: Throwable?): String = android.util.Log.getStackTraceString(tr)
}
