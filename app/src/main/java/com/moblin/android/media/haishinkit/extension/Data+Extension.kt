package com.moblin.android.media.haishinkit.extension

fun ByteArray.chunks(size: Int): List<ByteArray> {
    if (this.size < size) {
        return listOf(this)
    }
    val chunks: MutableList<ByteArray> = mutableListOf()
    val length = this.size
    var offset = 0
    do {
        val thisChunkSize = if ((length - offset) > size) size else (length - offset)
        chunks.add(this.copyOfRange(offset, offset + thisChunkSize))
        offset += thisChunkSize
    } while (offset < length)
    return chunks
}
