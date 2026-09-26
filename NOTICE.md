# Third-party notices

Moblin for Android is a port of [Moblin](https://github.com/eerimoq/moblin), MIT License, Copyright (c) 2023
Erik Moqvist.

It also contains code ported from, and files taken from, the following projects.

| Project | License | Used for |
|---|---|---|
| [ayagami](https://github.com/AyagamiDev/ayagami) | MIT or Apache-2.0 | Live2D compatible model loading and deformation in `platform/live2d/` |
| [AyagamiSwift](https://github.com/eerimoq/AyagamiSwift) | MIT, Copyright (c) 2026 Erik Moqvist | The Swift API that `platform/live2d/` mirrors |
| [VRMKit](https://github.com/tattn/VRMKit) ([fork](https://github.com/eerimoq/VRMKit)) | MIT, Copyright (c) 2018 Tatsuya Tanaka | VRM loading in `platform/scenekit/` |
| [MediaPipe](https://github.com/google-ai-edge/mediapipe) | Apache-2.0 | `app/src/main/assets/face_landmarker.task` and face landmark detection |
| [SRT](https://github.com/eerimoq/srt) | MPL-2.0 | `app/src/main/cpp/srt` |
| [Mbed TLS](https://github.com/Mbed-TLS/mbedtls) | Apache-2.0 or GPL-2.0-or-later | `app/src/main/cpp/mbedtls` |
| [libdatachannel](https://github.com/eerimoq/libdatachannel) | MPL-2.0 | `app/src/main/cpp/libdatachannel` |
| [librist](https://github.com/eerimoq/librist) | BSD-2-Clause, Copyright (c) 2019-2020 VideoLAN and librist authors | `app/src/main/cpp/librist` |
| [Material Design icons](https://github.com/google/material-design-icons) | Apache-2.0 | Glyphs of the launcher shortcut icons in `app/src/main/res/drawable/shortcut_*.xml` |

The MPL-2.0 sources are the git submodules under `app/src/main/cpp`, including any patches in
`app/src/main/cpp/patches`.
