# Tier: media

This file is part of the media pipeline. Translate all pure logic completely: byte parsing, packetizers, muxers, state machines, adaptive bitrate algorithms, timestamp arithmetic. Only the hardware-bound calls become TODO().

- AVCaptureSession, AVCaptureDevice and AVCaptureVideoDataOutput -> androidx.camera.core (CameraX) with ImageAnalysis for frames, or android.hardware.camera2 when fine control is needed. Keep the same class and method names around the Android calls.
- AVAudioEngine and AVAudioSession -> android.media.AudioRecord for capture and android.media.AudioTrack for playback, 48 kHz 16-bit PCM.
- VideoToolbox VTCompressionSession -> android.media.MediaCodec configured with MediaFormat for video/avc or video/hevc, using the async callback API. VTDecompressionSession -> a MediaCodec decoder. AudioConverter for AAC -> MediaCodec audio/mp4a-latm.
- CMSampleBuffer -> MediaSample (package com.moblin.android.media). CMTime -> Long microseconds. CMFormatDescription -> android.media.MediaFormat. CVPixelBuffer -> android.media.Image. AVAudioPCMBuffer -> ShortArray plus sampleRate and channels.
- CIImage, CIFilter, Metal, MTLDevice, MTLTexture, MetalKit and Core Image based video effects -> keep the effect's parameters and public API, make the render function TODO("OpenGL ES port") and list it in unsupported. Do not attempt to write shaders.
- Vision framework requests -> TODO() and list them in unsupported.
- libsrt calls stay as calls on a SrtNative object with external fun declarations.
- Never fabricate MediaCodec or CameraX behaviour. When a detail is uncertain, TODO() with a precise reason.
