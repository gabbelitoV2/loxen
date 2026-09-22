package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc

sealed class MpegTsVideoConfig {
    data class Avc(val config: MpegTsVideoConfigAvc) : MpegTsVideoConfig()

    data class Hevc(val config: MpegTsVideoConfigHevc) : MpegTsVideoConfig()
}
