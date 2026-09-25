package com.moblin.android.platform.core

class CharacterSet private constructor(private val allowed: (Int) -> Boolean) {
    fun contains(byte: Int): Boolean = allowed(byte)

    companion object {
        private const val alphanumerics = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        private fun ascii(characters: String): CharacterSet {
            val set = (alphanumerics + characters).map { it.code }.toSet()
            return CharacterSet { set.contains(it) }
        }

        val urlQueryAllowed = ascii("!$&'()*+,-./:;=?@_~")
        val urlPathAllowed = ascii("!$&'()*+,-./:=@_~")
        val urlHostAllowed = ascii("!$&'()*+,-.:;=[]_~")
        val urlFragmentAllowed = ascii("!$&'()*+,-./:;=?@_~")
        val urlUserAllowed = ascii("!$&'()*+,-.;=_~")
        val urlPasswordAllowed = ascii("!$&'()*+,-.;=_~")
    }
}

fun String.addingPercentEncoding(withAllowedCharacters: CharacterSet): String? {
    val builder = StringBuilder()
    for (byte in encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        if (value < 0x80 && withAllowedCharacters.contains(value)) {
            builder.append(value.toChar())
        } else {
            builder.append('%')
            builder.append("0123456789ABCDEF"[value shr 4])
            builder.append("0123456789ABCDEF"[value and 0xF])
        }
    }
    return builder.toString()
}
