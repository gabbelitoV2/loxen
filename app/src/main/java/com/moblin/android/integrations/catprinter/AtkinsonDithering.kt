package com.moblin.android.integrations.catprinter

class AtkinsonDithering {
    private var image: MutableList<UByteArray> = mutableListOf()
    private var width: Int = 0
    private var height: Int = 0

    fun apply(image: MutableList<UByteArray>): MutableList<UByteArray> {
        if (image.isEmpty()) {
            return image
        }
        this.image = image
        height = image.size
        width = image[0].size
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newColor = if (this.image[y][x].toInt() > 127) 255 else 0
                val diff = this.image[y][x].toInt() - newColor
                this.image[y][x] = newColor.toUByte()
                adjustPixel(y = y, x = x + 1, delta = diff / 8)
                adjustPixel(y = y, x = x + 2, delta = diff / 8)
                adjustPixel(y = y + 1, x = x - 1, delta = diff / 8)
                adjustPixel(y = y + 1, x = x, delta = diff / 8)
                adjustPixel(y = y + 1, x = x + 1, delta = diff / 8)
                adjustPixel(y = y + 2, x = x, delta = diff / 8)
            }
        }
        return this.image
    }

    private fun adjustPixel(y: Int, x: Int, delta: Int) {
        if (!(y >= 0 && y < height && x >= 0 && x < width)) {
            return
        }
        image[y][x] = (image[y][x].toInt() + delta).coerceIn(0, 255).toUByte()
    }
}
