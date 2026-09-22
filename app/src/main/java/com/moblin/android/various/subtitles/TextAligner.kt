package com.moblin.android.various.subtitles

import kotlin.math.abs

class TextAligner(text: String) {
    private var text: String = text
    var position: Int = 0

    fun update(newText: String) {
        val oldText = this.text
        if (newText == oldText) {
            return
        }
        val newLength = newText.length
        val oldLength = oldText.length
        if (oldLength == 0 || newLength == 0) {
            this.text = newText
            return
        }
        val shiftLimit = 25
        val minShift = maxOf(-(newLength - 1), -shiftLimit)
        val maxShift = minOf(oldLength - 1, shiftLimit)
        var bestShift = 0
        var bestMatches = -1
        for (shift in minShift..maxShift) {
            val startOffset = maxOf(0, -shift)
            val endOffset = minOf(newLength - 1, (oldLength - 1) - shift)
            if (startOffset > endOffset) {
                continue
            }
            var matches = 0
            var offset = startOffset
            while (offset <= endOffset) {
                if (newText[offset] == oldText[offset + shift]) {
                    matches += 1
                }
                offset += 1
            }
            if (matches > bestMatches ||
                (matches == bestMatches && abs(shift) < abs(bestShift)) ||
                (matches == bestMatches && abs(shift) == abs(bestShift) && shift == 0)
            ) {
                bestMatches = matches
                bestShift = shift
            }
        }
        position += bestShift
        this.text = newText
    }
}
