package com.moblin.android.media.haishinkit.codec.video

data class VTSessionProperty(
    val key: VTSessionPropertyKey,
    val value: Any
)

data class VTSessionPropertyKey(val value: String) {
    companion object {
        val profileLevel = VTSessionPropertyKey("ProfileLevel")
        val h264EntropyMode = VTSessionPropertyKey("H264EntropyMode")
        val colorPrimaries = VTSessionPropertyKey("ColorPrimaries")
        val transferFunction = VTSessionPropertyKey("TransferFunction")
        val YCbCrMatrix = VTSessionPropertyKey("YCbCrMatrix")
        val expectedFrameRate = VTSessionPropertyKey("ExpectedFrameRate")
        val allowFrameReordering = VTSessionPropertyKey("AllowFrameReordering")
        val maxKeyFrameIntervalDuration = VTSessionPropertyKey("MaxKeyFrameIntervalDuration")
        val pixelTransferProperties = VTSessionPropertyKey("PixelTransferProperties")
        val averageBitRate = VTSessionPropertyKey("AverageBitRate")
        val constantBitRate = VTSessionPropertyKey("ConstantBitRate")
        val variableBitRate = VTSessionPropertyKey("VariableBitRate")
        val dataRateLimits = VTSessionPropertyKey("DataRateLimits")
        val realTime = VTSessionPropertyKey("RealTime")
        val hdrMetadataInsertionMode = VTSessionPropertyKey("HDRMetadataInsertionMode")
    }
}
