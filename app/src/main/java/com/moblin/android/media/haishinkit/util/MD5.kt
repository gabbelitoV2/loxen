package com.moblin.android.media.haishinkit.util

import java.security.MessageDigest
import java.util.Base64

fun calculateMd5Base64(message: String): String =
    Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte())).encodeToString(calculateMd5(message))

fun calculateMd5(message: String): ByteArray = calculateMd5(message.encodeToByteArray())

fun calculateMd5(data: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(data)
