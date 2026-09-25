package com.moblin.android.platform.cryptokit

import java.util.Base64

internal class DerNode(val tag: Int, val content: ByteArray, val children: List<DerNode>)

private const val TAG_INTEGER = 0x02
private const val TAG_BIT_STRING = 0x03
private const val TAG_OCTET_STRING = 0x04
private const val TAG_OBJECT_IDENTIFIER = 0x06
private const val TAG_SEQUENCE = 0x30

internal val ecPublicKeyOid = unsignedBytes(0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02, 0x01)
internal val secp256r1Oid = unsignedBytes(0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07)
private val secp384r1Oid = unsignedBytes(0x2B, 0x81, 0x04, 0x00, 0x22)
private val secp521r1Oid = unsignedBytes(0x2B, 0x81, 0x04, 0x00, 0x23)

private val p256AlgorithmIdentifier = unsignedBytes(0x30, 0x13, 0x06, 0x07) + ecPublicKeyOid +
    unsignedBytes(0x06, 0x08) + secp256r1Oid

internal fun unsignedBytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

internal fun hexString(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

internal fun parseDer(bytes: ByteArray): DerNode {
    val nodes = parseDerNodes(bytes, 0, bytes.size, 1)
    if (nodes.size != 1) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    return nodes[0]
}

private fun parseDerNodes(bytes: ByteArray, start: Int, end: Int, depth: Int): List<DerNode> {
    if (depth > 10) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    val nodes = mutableListOf<DerNode>()
    var index = start
    while (index < end) {
        val tag = bytes[index].toInt() and 0xFF
        if (tag and 0x1F == 0x1F) {
            throw CryptoKitASN1Error.invalidFieldIdentifier
        }
        index += 1
        if (index >= end) {
            throw CryptoKitASN1Error.truncatedASN1Field
        }
        val first = bytes[index].toInt() and 0xFF
        index += 1
        val length: Int
        if (first < 0x80) {
            length = first
        } else {
            val count = first and 0x7F
            if (count == 0 || count > 4) {
                throw CryptoKitASN1Error.unsupportedFieldLength
            }
            if (end - index < count) {
                throw CryptoKitASN1Error.truncatedASN1Field
            }
            if (bytes[index].toInt() == 0) {
                throw CryptoKitASN1Error.unsupportedFieldLength
            }
            var value = 0L
            repeat(count) {
                value = (value shl 8) or (bytes[index].toLong() and 0xFF)
                index += 1
            }
            if (value < 0x80 || value > Int.MAX_VALUE) {
                throw CryptoKitASN1Error.unsupportedFieldLength
            }
            length = value.toInt()
        }
        if (end - index < length) {
            throw CryptoKitASN1Error.truncatedASN1Field
        }
        val children = if (tag and 0x20 != 0) parseDerNodes(bytes, index, index + length, depth + 1) else emptyList()
        nodes.add(DerNode(tag, bytes.copyOfRange(index, index + length), children))
        index += length
    }
    return nodes
}

private class DerIterator(node: DerNode) {
    private val nodes: List<DerNode>
    private var index = 0

    init {
        if (node.tag != TAG_SEQUENCE) {
            throw CryptoKitASN1Error.unexpectedFieldType
        }
        nodes = node.children
    }

    fun next(): DerNode {
        if (index >= nodes.size) {
            throw CryptoKitASN1Error.invalidASN1Object
        }
        index += 1
        return nodes[index - 1]
    }

    fun nextOrNull(): DerNode? {
        if (index >= nodes.size) {
            return null
        }
        index += 1
        return nodes[index - 1]
    }

    fun optionalExplicitlyTagged(tagNumber: Int): DerNode? {
        val node = nodes.getOrNull(index) ?: return null
        if (node.tag != 0xA0 + tagNumber) {
            return null
        }
        index += 1
        if (node.children.size != 1) {
            throw CryptoKitASN1Error.invalidASN1Object
        }
        return node.children[0]
    }

    fun finish() {
        if (index != nodes.size) {
            throw CryptoKitASN1Error.invalidASN1Object
        }
    }
}

private fun DerNode.integerValue(): Long {
    if (tag != TAG_INTEGER) {
        throw CryptoKitASN1Error.unexpectedFieldType
    }
    if (content.isEmpty()) {
        throw CryptoKitASN1Error.invalidASN1IntegerEncoding
    }
    if (content.size > 1) {
        val first = content[0].toInt() and 0xFF
        val second = content[1].toInt() and 0xFF
        if ((first == 0x00 && second < 0x80) || (first == 0xFF && second >= 0x80)) {
            throw CryptoKitASN1Error.invalidASN1IntegerEncoding
        }
    }
    if (content.size > 8) {
        throw CryptoKitASN1Error.invalidASN1IntegerEncoding
    }
    var value = content[0].toLong()
    for (i in 1 until content.size) {
        value = (value shl 8) or (content[i].toLong() and 0xFF)
    }
    return value
}

private fun DerNode.octetString(): ByteArray {
    if (tag != TAG_OCTET_STRING) {
        throw CryptoKitASN1Error.unexpectedFieldType
    }
    return content
}

private fun DerNode.bitString(): ByteArray {
    if (tag != TAG_BIT_STRING) {
        throw CryptoKitASN1Error.unexpectedFieldType
    }
    if (content.isEmpty()) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    val paddingBits = content[0].toInt() and 0xFF
    if (paddingBits > 7 || (content.size == 1 && paddingBits != 0)) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    if (paddingBits != 0 && (content.last().toInt() and ((1 shl paddingBits) - 1)) != 0) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    return content.copyOfRange(1, content.size)
}

private fun DerNode.objectIdentifier(): ByteArray {
    if (tag != TAG_OBJECT_IDENTIFIER) {
        throw CryptoKitASN1Error.unexpectedFieldType
    }
    if (content.isEmpty() || (content.last().toInt() and 0x80) != 0) {
        throw CryptoKitASN1Error.invalidObjectIdentifier
    }
    return content
}

private class AlgorithmIdentifier(val algorithm: ByteArray, val parameters: DerNode?) {
    fun isEcCurve(curve: ByteArray): Boolean {
        val parameters = parameters ?: return false
        return algorithm.contentEquals(ecPublicKeyOid) &&
            parameters.tag == TAG_OBJECT_IDENTIFIER &&
            parameters.content.contentEquals(curve)
    }
}

private fun parseAlgorithmIdentifier(node: DerNode): AlgorithmIdentifier {
    val nodes = DerIterator(node)
    val algorithm = nodes.next().objectIdentifier()
    val parameters = nodes.nextOrNull()
    nodes.finish()
    return AlgorithmIdentifier(algorithm, parameters)
}

private class Sec1PrivateKey(val privateKey: ByteArray, val curve: ByteArray?)

private fun parseSec1PrivateKey(node: DerNode): Sec1PrivateKey {
    val nodes = DerIterator(node)
    if (nodes.next().integerValue() != 1L) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    val privateKey = nodes.next().octetString()
    val curve = nodes.optionalExplicitlyTagged(0)?.objectIdentifier()
    nodes.optionalExplicitlyTagged(1)?.bitString()
    nodes.finish()
    if (curve != null && listOf(secp256r1Oid, secp384r1Oid, secp521r1Oid).none { it.contentEquals(curve) }) {
        throw CryptoKitASN1Error.invalidObjectIdentifier
    }
    return Sec1PrivateKey(privateKey, curve)
}

internal fun sec1PrivateKeyBytes(der: ByteArray): ByteArray = parseSec1PrivateKey(parseDer(der)).privateKey

internal fun pkcs8PrivateKeyBytes(der: ByteArray): ByteArray {
    val nodes = DerIterator(parseDer(der))
    if (nodes.next().integerValue() != 0L) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    val algorithm = parseAlgorithmIdentifier(nodes.next())
    val privateKey = nodes.next().octetString()
    nodes.optionalExplicitlyTagged(0)
    nodes.finish()
    val sec1 = parseSec1PrivateKey(parseDer(privateKey))
    if (sec1.curve != null && !algorithm.isEcCurve(sec1.curve)) {
        throw CryptoKitASN1Error.invalidASN1Object
    }
    return sec1.privateKey
}

internal fun subjectPublicKeyInfoKeyBytes(der: ByteArray): ByteArray {
    val nodes = DerIterator(parseDer(der))
    parseAlgorithmIdentifier(nodes.next())
    val key = nodes.next().bitString()
    nodes.finish()
    return key
}

internal fun p256PrivateKeyDer(rawRepresentation: ByteArray, x963PublicKey: ByteArray): ByteArray {
    return unsignedBytes(0x30, 0x81, 0x87, 0x02, 0x01, 0x00) + p256AlgorithmIdentifier +
        unsignedBytes(0x04, 0x6D, 0x30, 0x6B, 0x02, 0x01, 0x01, 0x04, 0x20) + rawRepresentation +
        unsignedBytes(0xA1, 0x44, 0x03, 0x42, 0x00) + x963PublicKey
}

internal fun p256PublicKeyDer(x963Representation: ByteArray): ByteArray {
    return unsignedBytes(0x30, 0x59) + p256AlgorithmIdentifier + unsignedBytes(0x03, 0x42, 0x00) +
        x963Representation
}

internal class PemDocument(val type: String, val derBytes: ByteArray)

private val pemNewline = Regex("[\\n\\r\\u000B\\u000C\\u0085\\u2028\\u2029]")

internal fun pemEncode(type: String, derBytes: ByteArray): String {
    val lines = mutableListOf("-----BEGIN $type-----")
    lines += Base64.getEncoder().encodeToString(derBytes).chunked(64)
    lines += "-----END $type-----"
    return lines.joinToString("\n")
}

internal fun pemDecode(pemString: String): PemDocument {
    val lines = pemString.split(pemNewline).filter { it.isNotEmpty() }
    if (lines.size < 3) {
        throw CryptoKitASN1Error.invalidPEMDocument
    }
    val type = pemDiscriminator(lines.first(), "-----BEGIN ")
    if (type == null || type != pemDiscriminator(lines.last(), "-----END ")) {
        throw CryptoKitASN1Error.invalidPEMDocument
    }
    val body = lines.subList(1, lines.size - 1)
    if (body.dropLast(1).any { it.length != 64 } || body.last().length > 64) {
        throw CryptoKitASN1Error.invalidPEMDocument
    }
    val base64 = body.joinToString("")
    if (base64.length % 4 != 0) {
        throw CryptoKitASN1Error.invalidPEMDocument
    }
    val derBytes = try {
        Base64.getDecoder().decode(base64)
    } catch (_: IllegalArgumentException) {
        throw CryptoKitASN1Error.invalidPEMDocument
    }
    return PemDocument(type, derBytes)
}

private fun pemDiscriminator(line: String, prefix: String): String? {
    val suffix = "-----"
    if (line.length < prefix.length + suffix.length || !line.startsWith(prefix) || !line.endsWith(suffix)) {
        return null
    }
    return line.substring(prefix.length, line.length - suffix.length)
}
