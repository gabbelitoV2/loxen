package com.moblin.android.common.various

import com.moblin.android.localized
import java.net.URI

fun isValidIngestLatency(value: String): String? {
    val latency = value.toIntOrNull() ?: return localized("Not a number")
    if (latency < 5) {
        return localized("Too small")
    }
    if (latency > 10000) {
        return localized("Too big")
    }
    return null
}

fun isValidRistVirtualPort(value: String): String? {
    isValidPort(value = value)?.let { error ->
        return error
    }
    val port = value.toUIntOrNull() ?: return localized("Not a number")
    if (port % 2u != 0u) {
        return localized("Must be even")
    }
    return null
}

fun isValidPort(value: String): String? {
    val port = value.toUIntOrNull() ?: return localized("Not a number")
    if (port <= 0u) {
        return localized("Too small")
    }
    if (port > UShort.MAX_VALUE.toUInt()) {
        return localized("Too big")
    }
    return null
}

fun isValidAudioBitrate(bitrate: Int): Boolean {
    if (bitrate < 32000 || bitrate > 320_000) {
        return false
    }
    if (bitrate % 32000 != 0) {
        return false
    }
    return true
}

fun isValidRtmpUrl(url: String, rtmpStreamKeyRequired: Boolean): String? {
    if (!rtmpStreamKeyRequired) {
        return null
    }
    if (makeRtmpUri(url = url) == "") {
        return localized("Malformed RTMP URL")
    }
    if (makeRtmpStreamKey(url = url) == "") {
        return localized("RTMP stream key missing")
    }
    return null
}

fun isValidSrtUrl(url: String): String? {
    val parsedUrl = runCatching { URI(url) }.getOrNull() ?: return localized("Malformed SRT(LA) URL")
    if (parsedUrl.port == -1) {
        return localized("SRT(LA) port number missing")
    }
    return null
}

fun isValidRistUrl(url: String): String? {
    val parsedUrl = runCatching { URI(url) }.getOrNull() ?: return localized("Malformed RIST URL")
    if (parsedUrl.port == -1) {
        return localized("RIST port number missing")
    }
    return null
}

fun isValidWhipUrl(url: String): String? {
    if (runCatching { URI(url) }.getOrNull() == null) {
        return localized("Malformed WHIP URL")
    }
    return null
}

fun isValidMobcamUrl(url: String): String? {
    val parsedUrl = runCatching { URI(url) }.getOrNull() ?: return localized("Malformed Mobcam URL")
    val host = parsedUrl.host
    if (host == null || !listOf("localhost", "127.0.0.1").contains(host.lowercase())) {
        return localized("Mobcam host must be localhost")
    }
    if (parsedUrl.port == -1) {
        return localized("Mobcam port number missing")
    }
    return null
}

private fun isValidRtspUrl(url: String): String? {
    if (runCatching { URI(url) }.getOrNull() == null) {
        return localized("Malformed RTSP URL")
    }
    return null
}

fun isValidUrl(
    value: String,
    allowedSchemes: List<String>? = null,
    rtmpStreamKeyRequired: Boolean = true,
): String? {
    val url = runCatching { URI(value) }.getOrNull() ?: return localized("Malformed URL")
    if (url.host == null) {
        return localized("Host missing")
    }
    if (url.port != -1) {
        val port = url.port
        if (port <= 0 || port > UShort.MAX_VALUE.toInt()) {
            return localized("Bad port")
        }
    }
    if (runCatching { URI(value) }.getOrNull() == null) {
        return localized("Malformed URL")
    }
    val scheme = url.scheme
    if (allowedSchemes != null && scheme != null) {
        if (!allowedSchemes.contains(scheme)) {
            return "Only ${allowedSchemes.joinToString(separator = " and ")} allowed, not $scheme"
        }
    }
    when (scheme) {
        "rtmp" -> {
            isValidRtmpUrl(url = value, rtmpStreamKeyRequired = rtmpStreamKeyRequired)?.let { message ->
                return message
            }
        }
        "rtmps" -> {
            isValidRtmpUrl(url = value, rtmpStreamKeyRequired = rtmpStreamKeyRequired)?.let { message ->
                return message
            }
        }
        "srt" -> {
            isValidSrtUrl(url = value)?.let { message ->
                return message
            }
        }
        "srtla" -> {
            isValidSrtUrl(url = value)?.let { message ->
                return message
            }
        }
        "rist" -> {
            isValidRistUrl(url = value)?.let { message ->
                return message
            }
        }
        "whip" -> {
            isValidWhipUrl(url = value)?.let { message ->
                return message
            }
        }
        "whips" -> {
            isValidWhipUrl(url = value)?.let { message ->
                return message
            }
        }
        "mobcam" -> {
            isValidMobcamUrl(url = value)?.let { message ->
                return message
            }
        }
        "http" -> {
            isValidHttpUrl(value = value)?.let { message ->
                return message
            }
        }
        "https" -> {
            isValidHttpUrl(value = value)?.let { message ->
                return message
            }
        }
        "rtsp" -> {
            isValidRtspUrl(url = value)?.let { message ->
                return message
            }
        }
        null -> return localized("Scheme missing")
        else -> return localized("Unsupported scheme $scheme")
    }
    return null
}

fun isValidWebSocketUrl(value: String): String? {
    if (value.isEmpty()) {
        return null
    }
    val url = runCatching { URI(value) }.getOrNull() ?: return localized("Malformed URL")
    if (url.host == null) {
        return localized("Host missing")
    }
    if (runCatching { URI(value) }.getOrNull() == null) {
        return localized("Malformed URL")
    }
    when (val scheme = url.scheme) {
        "ws" -> {}
        "wss" -> {}
        null -> return localized("Scheme missing")
        else -> return localized("Unsupported scheme $scheme")
    }
    return null
}

fun isValidHttpUrl(value: String): String? {
    if (value.isEmpty()) {
        return null
    }
    val url = runCatching { URI(value) }.getOrNull() ?: return localized("Malformed URL")
    if (url.host == null) {
        return localized("Host missing")
    }
    if (runCatching { URI(value) }.getOrNull() == null) {
        return localized("Malformed URL")
    }
    when (val scheme = url.scheme) {
        "http" -> {}
        "https" -> {}
        null -> return localized("Scheme missing")
        else -> return localized("Unsupported scheme $scheme")
    }
    return null
}
