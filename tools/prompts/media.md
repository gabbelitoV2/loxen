# Tier: media

This file is part of the media pipeline. Translate all pure logic completely: byte parsing, packetizers, muxers, state machines, adaptive bitrate algorithms, timestamp arithmetic. Only hardware-bound calls without a shim become TODO().

- Camera, microphone, audio session, encoder, recorder and pixel-buffer APIs have same-named shims in com.moblin.android.platform (see Platform API and the shim sections). Call them; do not call CameraX, Camera2, AudioRecord or MediaCodec directly. libsrt: import com.moblin.android.platform.srt.SrtNative; never declare SrtNative.
- Core Image, MetalPetal, CoreGraphics, simd, Vision, WebKit, MapKit, CoreMotion, SceneKit and the SwiftUI ImageRenderer have shims (see the Apple API shims and Platform API sections). Translate every effect completely against them, render paths included; never TODO("OpenGL ES port") and never declare placeholder classes for their types.
- Never fabricate MediaCodec or CameraX behaviour. When a detail is uncertain, TODO() with a precise reason.
