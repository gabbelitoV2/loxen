package com.moblin.android.media.haishinkit.extension

data class AudioStreamBasicDescription(
    var mSampleRate: Double = 0.0,
    var mFormatID: UInt = 0u,
    var mFormatFlags: UInt = 0u,
    var mBytesPerPacket: UInt = 0u,
    var mFramesPerPacket: UInt = 0u,
    var mBytesPerFrame: UInt = 0u,
    var mChannelsPerFrame: UInt = 0u,
    var mBitsPerChannel: UInt = 0u,
    var mReserved: UInt = 0u,
)
