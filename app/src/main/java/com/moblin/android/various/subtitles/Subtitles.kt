package com.moblin.android.various.subtitles

class Subtitles(languageIdentifier: String?) {
    private var lastLinePosition = 0
    private var previousFirstLinePosition = -1
    var lines: MutableList<String> = mutableListOf()
    private val length: Int

    init {
        length = if (languageIdentifier != null) {
            if (languageIdentifier.startsWith("zh")) {
                20
            } else if (languageIdentifier == "ja") {
                30
            } else {
                50
            }
        } else {
            50
        }
    }

    fun updateSubtitles(position: Int, text: String) {
        if (position < 0 || text.isEmpty()) {
            return
        }
        val endPosition = position + text.length
        while (lastLinePosition + length < endPosition) {
            lastLinePosition += length
        }
        while (lastLinePosition >= endPosition) {
            lastLinePosition -= length
            lastLinePosition = maxOf(lastLinePosition, 0)
        }
        val firstLinePosition = lastLinePosition - length
        val offset = lastLinePosition - position
        if (offset < 0 || offset >= text.length) {
            return
        }
        val lastLineIndex = offset
        val lastLine = text.substring(lastLineIndex)
        if (firstLinePosition >= position && firstLinePosition >= previousFirstLinePosition) {
            previousFirstLinePosition = firstLinePosition
            val offset = firstLinePosition - position
            if (offset >= text.length) {
                return
            }
            val firstLine = text.substring(offset, lastLineIndex)
            lines = mutableListOf(firstLine.trim(), lastLine.trim())
        } else {
            lines = mutableListOf(lastLine.trim())
        }
    }
}
