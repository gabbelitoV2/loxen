package com.moblin.android.integrations.catprinter

class FloydSteinbergDithering {
    private var image: MutableList<MutableList<UByte>> = mutableListOf()
    private var width: Int = 0
    private var height: Int = 0

    fun apply(image: List<List<UByte>>): List<List<UByte>> {
        if (image.isEmpty()) {
            return image
        }
        this.image = image.map { it.toMutableList() }.toMutableList()
        height = image.size
        width = image[0].size
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newColor = if (this.image[y][x].toInt() > 127) 255 else 0
                val diff = this.image[y][x].toInt() - newColor
                this.image[y][x] = newColor.toUByte()
                adjustPixel(y = y, x = x + 1, delta = diff * 7 / 16)
                adjustPixel(y = y + 1, x = x - 1, delta = diff * 3 / 16)
                adjustPixel(y = y + 1, x = x, delta = diff * 5 / 16)
                adjustPixel(y = y + 1, x = x + 1, delta = diff * 1 / 16)
            }
        }
        return this.image
    }

    private fun adjustPixel(y: Int, x: Int, delta: Int) {
        if (y < 0 || y >= height || x < 0 || x >= width) {
            return
        }
        image[y][x] = (image[y][x].toInt() + delta).coerceIn(0, 255).toUByte()
    }
}
