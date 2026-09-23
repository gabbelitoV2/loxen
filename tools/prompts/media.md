# Tier: media

This file is part of the media pipeline. Translate all pure logic completely: byte parsing, packetizers, muxers, state machines, adaptive bitrate algorithms, timestamp arithmetic. Only hardware-bound calls without a shim become TODO().

- Camera, microphone, audio session, encoder, recorder and pixel-buffer APIs have same-named shims in com.moblin.android.platform (see Platform API and the shim sections). Call them; do not call CameraX, Camera2, AudioRecord or MediaCodec directly. libsrt: import com.moblin.android.platform.srt.SrtNative; never declare SrtNative.
- CIImage, CIFilter, Metal, MTLDevice, MTLTexture, MetalKit and Core Image based video effects -> keep the effect's parameters and public API, make the render function TODO("OpenGL ES port") and list it in unsupported. Do not attempt to write shaders.
- Vision framework requests -> TODO() and list them in unsupported.
- Never fabricate MediaCodec or CameraX behaviour. When a detail is uncertain, TODO() with a precise reason.
