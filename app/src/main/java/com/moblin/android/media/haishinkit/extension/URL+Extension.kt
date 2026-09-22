package com.moblin.android.media.haishinkit.extension

import java.net.URI

val URI.absoluteWithoutAuthenticationString: String
    get() {
        if (userInfo == null) {
            return toString()
        }
        return runCatching {
            URI(scheme, null, host, port, path, query, fragment).toString()
        }.getOrDefault(toString())
    }

fun URI.dictionaryFromQuery(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val query = rawQuery ?: return result
    if (query.isEmpty()) {
        return result
    }
    for (item in query.split("&")) {
        if (item.isEmpty()) {
            continue
        }
        val index = item.indexOf('=')
        if (index < 0) {
            continue
        }
        val name = percentDecode(item.substring(0, index))
        val value = percentDecode(item.substring(index + 1))
        result[name] = value
    }
    return result
}

private fun percentDecode(value: String): String {
    if (value.indexOf('%') < 0) {
        return value
    }
    val bytes = ByteArray(value.length * 4)
    var count = 0
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '%' && index + 3 <= value.length) {
            val code = value.substring(index + 1, index + 3).toIntOrNull(16)
            if (code != null) {
                bytes[count++] = code.toByte()
                index += 3
                continue
            }
        }
        val encoded = character.toString().toByteArray(Charsets.UTF_8)
        for (byte in encoded) {
            bytes[count++] = byte
        }
        index += 1
    }
    return String(bytes, 0, count, Charsets.UTF_8)
}
