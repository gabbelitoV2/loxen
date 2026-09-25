package com.moblin.android.various.utils

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.moblin.android.platform.coreimage.qrCodeModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrCodeRenderSuite {
    private val message = "moblin://?%7B%22remoteControl%22%3A%7B%7D%7D"

    @Test
    fun everyModuleIsFiveSharpPixelsTheRightWayUp() {
        val modules = qrCodeModules(message.toByteArray(Charsets.UTF_8), "M")!!
        val image = generateQrCode(from = message)
        assertNotNull(image)
        image!!
        assertEquals(modules.size * 5, image.width)
        assertEquals(modules.size * 5, image.height)
        for (y in 0 until modules.size) {
            for (x in 0 until modules.size) {
                for ((dx, dy) in listOf(0 to 0, 4 to 0, 2 to 2, 0 to 4, 4 to 4)) {
                    val red = Color.red(image.getPixel(x * 5 + dx, y * 5 + dy))
                    if (modules.isDark(x, y)) {
                        assertTrue("module ($x, $y) pixel ($dx, $dy) is $red", red < 32)
                    } else {
                        assertTrue("module ($x, $y) pixel ($dx, $dy) is $red", red > 223)
                    }
                }
            }
        }
    }

    @Test
    fun aReaderGetsTheMessageBack() {
        val image = generateQrCode(from = message)!!
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels)))
        val result = QRCodeReader().decode(bitmap, mapOf(DecodeHintType.PURE_BARCODE to true))
        assertEquals(message, result.text)
    }
}
