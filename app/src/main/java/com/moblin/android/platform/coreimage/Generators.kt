package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coreimage.internal.EffectsLog

class CIQRCodeGenerator : CIFilter() {
    var message: ByteArray = ByteArray(0)
    var correctionLevel: String = "M"

    override val name: String
        get() = "CIQRCodeGenerator"

    override val outputImage: CIImage?
        get() {
            EffectsLog.notImplemented("CIQRCodeGenerator.outputImage")
            return null
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            "inputMessage" -> message = value as? ByteArray ?: message
            "inputCorrectionLevel" -> correctionLevel = value as? String ?: correctionLevel
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            "inputMessage" -> message
            "inputCorrectionLevel" -> correctionLevel
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        message = ByteArray(0)
        correctionLevel = "M"
    }
}
