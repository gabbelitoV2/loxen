# Loxen

Loxen is an unofficial Android IRL streaming app, automatically ported from
[Moblin](https://github.com/eerimoq/moblin), the iOS IRL streaming app by Erik Moqvist. A pipeline clones the Swift
sources of eerimoq/moblin into `.upstream/`, sorts every file by how portable it is, and translates it file by file
into the Android Studio project under `app/`. The Kotlin keeps the same structure, type names and function names as
the Swift code, and hand-written Apple-named shims under `app/src/main/java/com/moblin/android/platform/` provide the
Apple APIs on Android (camera, encoding, video effects, SRT, RIST, WebRTC, Bluetooth, ...). Every night the port
follows the latest Moblin automatically. What has no Android counterpart is listed in `PORT-REPORT.md`.

Loxen is based on Moblin by Erik Moqvist (MIT). Not affiliated with or endorsed by Moblin. Both are MIT licensed, see
`LICENSE` and `NOTICE.md`.

[Privacy policy](https://gabbelitov2.github.io/loxen/privacy-policy/en.html)

## Test the app

Loxen is in closed testing on Google Play. To join:

1. Join the [Loxen Testers](https://groups.google.com/g/loxen-testers) Google Group (*Join group*).
2. Open the [opt-in page](https://play.google.com/apps/testing/com.loxen.app) with the same Google account and tap
   *Become a tester*.
3. Install Loxen from Google Play.

Please stay in the group and keep Loxen installed for at least 14 days: Google requires 12 testers for 14 days before
the app can be released publicly. Report bugs and ideas in [issues](https://github.com/gabbelitoV2/loxen/issues).

## Import settings using loxen:// or moblin:// (custom URL)

The custom URL format comes from Moblin, see
[Import settings using moblin:// (custom URL)](https://github.com/eerimoq/moblin#import-settings-using-moblin-custom-url)
in Moblin's README. Loxen opens both `loxen://` and `moblin://` URLs, so links and QR codes made for iOS Moblin also
work in Loxen. *Settings > Deep link creator* in the app (shown with *Show all settings* on) builds such links and
QR codes. It writes `moblin://` links, which work in both apps.

Loxen asks before it imports anything, and does not import while live or recording. A stream with the same name as an
existing one can replace it or be added under a new name.

### Examples

#### New stream

An example creating a new stream is

```
loxen://?{"streams":[{"name":"BELABOX%20UK","url":"srtla://uk.srt.belabox.net:5000?streamid=9812098rh9hf8942hid","video":{"codec":"H.265/HEVC"},"obs":{"webSocketUrl":"ws://123.22.32.112:5465","webSocketPassword":"foobar"}}]}
```

where the URL decoded pretty printed JSON blob is

```json
{
  "streams": [
    {
      "name": "BELABOX UK",
      "url": "srtla://uk.srt.belabox.net:5000?streamid=9812098rh9hf8942hid",
      "video": {
        "codec": "H.265/HEVC"
      },
      "obs": {
        "webSocketUrl": "ws://123.22.32.112:5465",
        "webSocketPassword": "foobar"
      }
    }
  ]
}
```

#### Quick button settings

An example with only two quick buttons enabled is

```
loxen://?{"quickButtons":{"twoColumns":false,"showName":true,"enableScroll":true,"disableAllButtons":true,"buttons":[{"type":"Mute","enabled":true},{"type":"Draw","enabled":true}]}}
```

where the URL decoded pretty printed JSON blob is

```json
{
  "quickButtons": {
    "twoColumns": false,
    "showName": true,
    "enableScroll": true,
    "disableAllButtons": true,
    "buttons": [
      {
        "type": "Mute",
        "enabled": true
      },
      {
        "type": "Draw",
        "enabled": true
      }
    ]
  }
}
```

#### Web browser and remote control

An example setting the web browser's home page and connecting to a remote control assistant is

```
loxen://?{"webBrowser":{"home":"https://example.com"},"remoteControl":{"streamer":{"enabled":true,"url":"ws://192.168.1.10:2345"},"password":"secret"}}
```

where the URL decoded pretty printed JSON blob is

```json
{
  "webBrowser": {
    "home": "https://example.com"
  },
  "remoteControl": {
    "streamer": {
      "enabled": true,
      "url": "ws://192.168.1.10:2345"
    },
    "password": "secret"
  }
}
```

The same URLs with `moblin://` instead of `loxen://` import the same settings.

### Specification

Format: `loxen://?<URL encoded JSON blob>` or `moblin://?<URL encoded JSON blob>`

The JSON blob is Moblin's `MoblinSettingsUrl`, ported to
`app/src/main/java/com/moblin/android/various/MoblinSettingsUrl.kt`. Class members are JSON object keys. Every key is
optional unless it is marked as required, and unknown keys are ignored. These are the keys Loxen imports:

| key | type | notes |
|---|---|---|
| `streams` | array of streams | Added in order. A stream whose name already exists can replace that stream. |
| `quickButtons` | object | See below. |
| `webBrowser.home` | string | The web browser's home page. |
| `remoteControl` | object | See below. |

A stream:

| key | type | notes |
|---|---|---|
| `name` | string, required | |
| `url` | string, required | Must be a valid stream URL, for example `rtmp://`, `rtmps://`, `srt://`, `srtla://` or `rist://`. |
| `selected` | bool | `true` makes it the current stream. |
| `backgroundStreaming` | bool | |
| `backgroundStreamingPiP` | bool | |
| `video.resolution` | string | `"4032x3024"`, `"3840x2160"`, `"2560x1440"`, `"1920x1440"`, `"1920x1080"`, `"1664x936"`, `"1280x720"`, `"1024x768"`, `"960x540"`, `"854x480"`, `"640x360"` or `"426x240"`. |
| `video.fps` | int | 15, 25, 30, 50, 60, 100 or 120. Other values are ignored. |
| `video.bitrate` | int | Bits per second, 50000 to 50000000. Other values are ignored. |
| `video.codec` | string | `"H.264/AVC"` or `"H.265/HEVC"`. |
| `video.bFrames` | bool | |
| `video.maxKeyFrameInterval` | int | Seconds, 0 to 10. Other values are ignored. |
| `audio.bitrate` | int | Bits per second, a multiple of 32000 from 32000 to 320000. Other values are ignored. |
| `srt.latency` | int | Milliseconds, 0 to 65535. |
| `srt.adaptiveBitrateEnabled` | bool | |
| `srt.dnsLookupStrategy` | string | `"System"`, `"IPv4"`, `"IPv6"` or `"IPv4 and IPv6"`. |
| `obs.webSocketUrl`, `obs.webSocketPassword` | strings, both required | Enables OBS remote control. The URL must be a valid WebSocket URL. |
| `twitch.channelName`, `twitch.channelId` | strings, both required | |
| `kick.channelName` | string, required | |

`quickButtons`:

| key | type | notes |
|---|---|---|
| `twoColumns`, `showName`, `enableScroll` | bool | |
| `disableAllButtons` | bool | `true` disables every quick button first, so that `buttons` enables only the listed ones. |
| `buttons` | array | Each has `type` (required, the quick button's name, for example `"Mute"` or `"Draw"`, as in `SettingsQuickButtonType`), `enabled` (bool) and `page` (int). |

`remoteControl`:

| key | type | notes |
|---|---|---|
| `password` | string, required | |
| `assistant.enabled`, `assistant.port` | bool and int, both required | Port 0 to 65535. |
| `assistant.relay.enabled`, `assistant.relay.baseUrl`, `assistant.relay.bridgeId` | bool and strings, all required | |
| `streamer.enabled`, `streamer.url` | bool and string, both required | The URL of the assistant to connect to. |

## Requirements

- Python 3.10 or newer.
- One of:
  - Claude Code logged in (`claude` in a terminal, then `/login`). Used when no API key is set.
  - `pip install anthropic` and `ANTHROPIC_API_KEY`.
  - `pip install anthropic` and `DEEPSEEK_API_KEY`, then `--provider deepseek`. Uses `deepseek-flash`,
    the current DeepSeek V4.1 Flash. About forty times cheaper than Claude Opus. Prices double during
    peak hours, 01:00-04:00 and 06:00-10:00 UTC on weekdays, so the nightly sync runs at 19:47 UTC (GitHub often starts scheduled runs hours late) and the repair
    schedule skips the peak hours.

  API keys can also be put in a `.env` file in this directory, one `NAME=value` per line. The file is
  ignored by git.
- Android Studio to open the project. Pick a local Gradle installation the first time, or run
  `gradle wrapper` in this directory.

## Keeping up with Moblin

Erik only changes the Swift app. One command brings the Android port up to date:

```sh
python tools/sync.py --commit --push
```

It fetches eerimoq/moblin into `.upstream/`, finds the Swift files that changed since each Kotlin file was
last ported, and sends the model the Swift diff together with the current Kotlin file. The model applies
only that change, so hand-written Android fixes inside generated files survive. New Swift files are
translated in full, deleted ones are removed. Then `tools/postprocess.py` applies the deterministic
Android rules, `tools/fix.py` feeds compile errors back to the model until the app builds, and the result
is committed.

The sync never stops halfway. When the app or its unit tests still do not compile after the fix rounds, or a
re-translated file lost one of its hooks, it puts back the previous version of each Kotlin file this sync changed that
has errors, together with its entry in `tools/port-state.json` so that the next sync retries it, and builds again until
the app builds. Everything that needs attention (files held back with their errors, missing hooks, a failing
`tools/pbswift.py`, findings of the hand-port and effects checks) goes into `tools/sync-report.json`, which is
committed with the rest. A sync never pushes an app that does not build: when it still fails with every change held
back, only the report is committed. Before pushing on top of a `main` that moved in the meantime, it builds again.

`python tools/sync.py --dry-run` shows the new upstream commits and the files that would be ported.

The Tesla protobufs (`Moblin/Integrations/Tesla/Protobuf/*.pb.swift`, generated by `protoc`) never go to the
model. Every sync runs `tools/pbswift.py`, which translates them deterministically into
`app/src/main/java/com/moblin/android/integrations/tesla/protobuf/` on top of the SwiftProtobuf runtime in
`platform/swiftprotobuf/`. It stops with the Swift file and line when a new `protoc` emits something it does not
understand; the sync then keeps the previously generated Kotlin and reports it. `python tools/pbswift.py --check`
verifies that the committed Kotlin matches the Swift.

The GitHub Actions workflow in `.github/workflows/android.yml` runs the same sync every night and on
demand, then builds `app-debug.apk` and attaches it to the run. It needs the repository secret
`DEEPSEEK_API_KEY`.

## System tests

`tests/` is a copy of Moblin's system tests (Python, MIT, by Erik Moqvist). They drive the app from a computer over
Moblin's remote control protocol: the computer runs `moblin_assistant`, the app connects to it, and the tests import
settings, switch scenes and mics, go live, record, send chat messages and so on, then check the result with mediamtx
and ffmpeg (does the stream arrive and decode, does the timecode in the audio run without gaps, what did the app log).
Every sync copies Moblin's `tests/` again (`tools/resources.py`), so nothing in it is changed for Loxen; Loxen's
part lives in `tools/`.

`tools/system_tests.py` runs them against Loxen on an Android device on the same network as the computer. Like
Moblin's, the tests need Python 3.14 or newer; `setup` looks for it (also in pyenv-win) and builds `.venv` with it.

```sh
python tools/system_tests.py setup                    # .venv with the test packages, on Windows also the tools
python tools/system_tests.py firewall                 # Windows, once per checkout: asks for administrator rights
python tools/system_tests.py test                     # every suite
python tools/system_tests.py test StreamSrtToMediaMtx # one test; any argument of python -m tests.test works
python tools/system_tests.py stability --duration 0.5
```

`test` finds the device with adb (`--serial` picks one), writes `tests/config.toml` with the addresses of the device
and of this computer (edit the capabilities there; later runs only update the addresses), keeps the screen on while
the device is plugged in and turns the media volume up (the talkback tests listen to the speaker with the microphone);
both are put back afterwards. The first run copies Loxen's settings to `.system-tests/device-settings/` (debug builds
only), because the tests replace them; `python tools/system_tests.py restore-settings` puts them back. Then it waits
for Loxen to connect to the test assistant, and if it does not, points Loxen's remote control at this computer with a
`moblin://` link, tapping *Import settings* itself. Loxen must be installed. A watchdog starts Loxen again when
Android kills it and prints why. The tablet streams for minutes at a time, so keep it on a charger (wireless
debugging works). The map tests need location services on.

On Windows, `setup` downloads the full ffmpeg build (the tests need its `qrencode` filter), mediamtx and qrtool into
`.system-tests/`, and installs `tools/system_tests_shims/` into `.venv`: `ltcgen` and `ltcdump` (SMPTE linear
timecode, which the tests put in the audio of their streams and read back from recordings), `lsof`, and a `tcpdump`
that only says that packet capture is not available. `firewall` allows inbound connections to that ffmpeg, mediamtx
and Python on private networks, so run it again after moving the checkout or changing Python. The stability test's
`--network-capture` and its traffic shaper (a Linux machine controlled over SSH) are not supported on Windows. As in
Moblin, tests that need a capability the device does not list, a DJI camera, a gimbal or the Arduino rig are skipped.

## Automatic repair

What the nightly sync cannot bring in is repaired by Claude, without anyone touching the Android port:

1. When `tools/sync-report.json` is not empty, or the sync stopped or timed out, the sync opens an issue labelled
   `needs-repair`, or updates the one that is open, with the report. When a later sync has nothing to report, it
   closes the issue. Only issues opened by the workflow itself (`github-actions[bot]`) are used.
2. `.github/workflows/repair.yml` runs after every sync, at 00:17, 04:17, 12:17, 16:17 and 20:17 UTC and on demand, but only while such an issue is
   open. It gives Claude `tools/prompts/repair.md` and the report, and Claude fixes the port the same way a person
   would: shims and translation guidance first, then translating the Swift again through the pipeline, hooks only as
   one-line fixes.
3. The workflow then checks the result itself: `tools/check_hooks.py` with nothing missing, `tools/pbswift.py --check`,
   `assembleDebug` and all unit tests with no failures other than those in `tools/known_test_failures.json`, those
   that also fail on `main` and those that pass when run again. Only then does it commit and push. It closes the issue
   when nothing is left, and otherwise updates it.

Each attempt leaves a comment on the issue with the result and Claude's summary. The repair prefers 20:00-06:00 UTC,
so that it does not compete with interactive use of the same Claude subscription, and makes at most two attempts per
issue in any 24 hours, runs that hit the usage limit included; in daytime it only runs when nothing was tried for 24
hours, so an issue is never given up on. After 4 attempts in a row that leave the same items unfixed, it waits 1, 2, 4
and then 7 days between attempts, until the items change or a repair pushes. When Claude's usage limit is reached, it
stops without changes, comments when the limit resets and skips runs until then.

Push builds use their own concurrency group; the sync and the repair share `sync-and-build`, so they never push at the
same time, and both build again before pushing on top of a `main` that moved. The workflow needs the repository
secrets `CLAUDE_CODE_OAUTH_TOKEN` (from `claude setup-token`) and `DEEPSEEK_API_KEY`. Run it by hand from the Actions
tab: `repair` with `force` to try right away, or `verify-only` to run the checks on `main` without Claude.

## Google Play

The app is distributed as `com.loxen.app` on the internal and closed testing tracks of Google Play, with Play App
Signing:
Google keeps the app signing key and the builds here are signed with an upload key.

### Versions

- `versionName` is Moblin's `MARKETING_VERSION` from `Config/Base.xcconfig` in the Swift project. Every sync copies it
  into `app/moblin-version.properties` (`tools/resources.py`), so the Android port always shows the version it was
  ported from. Debug builds show it too.
- `versionCode` of release builds is 1000 plus the number of commits of `HEAD` (`git rev-list --count HEAD`). Every
  commit on `main` raises it, and the same commit always gets the same code. It needs the full git history, so a
  shallow clone fails the release build. Debug builds keep `versionCode` 1, so debug APKs from any branch install over
  each other. `./gradlew -q :app:printReleaseVersion` prints both.

### Release build

Release builds are not debuggable and not minified. R8 stays off because this code base depends on names at run
time in ways a keep rule list would have to follow forever: the C++ code finds `SrtNative`, `SrtSendHook`,
`CBytePerfMon`, `RistNative`, the RIST callbacks and `DataChannelNative` and their `on*` callbacks and fields by name
through JNI, `CIFilter` loads filters with `Class.forName`, `StructCopy` copies Swift structs through reflection on
their fields and no-argument constructors, and hundreds of `@Serializable` settings classes are regenerated every
night. The release build is uploaded without anyone testing it on a device, so a missing keep rule would only show up
as a crash on a tester's phone. The dex files are about 40 % of an arm64 download (about 23 of 57 MB compressed), so
R8 would save some megabytes, but not enough to be worth that risk on an internal test track.

The release signing configuration reads these settings, each from an environment variable first and otherwise from a
local properties file with the same keys:

| setting | meaning |
|---|---|
| `MOBLIN_UPLOAD_KEYSTORE_BASE64` | the upload keystore, base64 encoded (used by CI) |
| `MOBLIN_UPLOAD_KEYSTORE_FILE` | or an absolute path to the keystore; default `upload-keystore.jks` next to the properties file |
| `MOBLIN_UPLOAD_KEYSTORE_PASSWORD` | the keystore password |
| `MOBLIN_UPLOAD_KEY_ALIAS` | the key alias, default `upload` |
| `MOBLIN_UPLOAD_KEY_PASSWORD` | the key password, default the keystore password |

The properties file lives outside every checkout, next to the keystore, and is never committed. Point Gradle at it
with a line in `local.properties` (ignored by git) or with `-Pmoblin.uploadKeystoreProperties=...`:

```properties
moblin.uploadKeystoreProperties=<path to the keys folder>/upload-keystore.properties
```

Then `./gradlew :app:bundleRelease :app:assembleRelease` writes the signed app bundle to
`app/build/outputs/bundle/release/app-release.aab` and a signed APK with every ABI to
`app/build/outputs/apk/release/app-release.apk`, which installs directly with `adb install`. Without an upload key the
release outputs are unsigned; debug builds never need the key.

### Automatic uploads

`.github/workflows/play.yml` runs after every successful nightly sync (`Sync and build` started by the schedule or by
hand, not by a push) and by hand from the Actions tab. It always builds `main`:

1. Without the upload key secrets it stops right away.
2. It reads the release `versionCode` and asks Google Play for the highest version code of the app on any track or in
   the bundle library (`tools/play.py`). When that is not lower, `main` has not changed since the last upload and the
   run stops without building. A run started by hand still builds.
3. It builds the signed app bundle and APK, attaches both to the run for 14 days as the artifact
   `loxen-<versionCode>-<versionName>` (`loxen-<versionCode>-<versionName>.aab` and `.apk`), and uploads the bundle
   to the internal track as a completed release with `r0adkll/upload-google-play` v1.1.5, pinned by commit.
4. The release gets release notes (What's new, at most 500 characters per language) from `tools/play.py notes`: the
   previous version code on Google Play is 1000 plus a commit count, so it names the commit of the last upload; the
   notes cover Loxen's commits since then and the Moblin commits that the syncs in between brought in, written in
   English for users by DeepSeek (`DEEPSEEK_API_KEY`) and translated by it into Swedish, as long as the store listing
   has Swedish (sv-SE). Without the key, or when DeepSeek fails, they list the commit subjects in English only, and
   Google Play shows the English notes in every language. The notes stay with the release when it is promoted to
   closed testing.

Without `PLAY_SERVICE_ACCOUNT_JSON`, or while the app does not exist in Play Console, nightly runs stop after step 2 and
runs started by hand only attach the signed bundle and APK. A service account that has no access to the app fails the run.
The workflow has its own concurrency group, `play-internal`, and only reads the repository.

Repository secrets:

| secret | value |
|---|---|
| `MOBLIN_UPLOAD_KEYSTORE_BASE64` | `upload-keystore.jks`, base64 encoded |
| `MOBLIN_UPLOAD_KEYSTORE_PASSWORD`, `MOBLIN_UPLOAD_KEY_ALIAS`, `MOBLIN_UPLOAD_KEY_PASSWORD` | from `upload-keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | the JSON key of the Google Cloud service account |

```powershell
$keys = "<path to the keys folder>"
gh secret set --repo gabbelitoV2/loxen -f "$keys\upload-keystore.properties"
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$keys\upload-keystore.jks")) | gh secret set MOBLIN_UPLOAD_KEYSTORE_BASE64 --repo gabbelitoV2/loxen
Get-Content "$keys\play-service-account.json" -Raw | gh secret set PLAY_SERVICE_ACCOUNT_JSON --repo gabbelitoV2/loxen
```

Setting up the service account:

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project, open *APIs & Services*, and
   enable the *Google Play Android Developer API*.
2. Under *IAM & Admin > Service accounts*, create a service account without any roles, then *Keys > Add key > Create
   new key > JSON*. Save the file as `play-service-account.json` in the keys folder.
3. In [Play Console](https://play.google.com/console/), open *Users and permissions*, invite the service account's
   e-mail address, add the app under *App permissions* with *View app information (read-only)* and *Release apps to
   testing tracks*, and send the invitation. The access can take a few hours to start working.

The first app bundle has to be uploaded by hand, because the API cannot create an app and Play App Signing is chosen
in Play Console:

1. Create the app in Play Console with the package name `com.loxen.app`.
2. Build a bundle: run this workflow by hand before `PLAY_SERVICE_ACCOUNT_JSON` is set and download
   `loxen-<versionCode>-<versionName>` from the run, or build it locally.
3. Under *Test and release*, open *Internal testing*: add testers, create a release, keep the Google-generated app
   signing key (Play App Signing), upload the bundle (`loxen-<versionCode>-<versionName>.aab` from the run, or
   `app-release.aab` from a local build) and start the rollout. The release has to be rolled out, not only saved as a
   draft, or later uploads fail with "Only releases with status draft may be created on draft app".
4. Then add `PLAY_SERVICE_ACCOUNT_JSON`. From then on every sync that changes `main` reaches the testers.

With Play App Signing a lost or leaked upload key is not the end: the account owner can ask Play support to reset the
upload key (*App integrity*, *Play app signing*, *Request upload key reset*) and register a new one.

## Hand-written Android code

Everything in `app/src/main/java/com/moblin/android/platform/` is written by hand and never generated:
camera, encoding, transports and other Android APIs. Generated files call into it through one-line hooks,
which `tools/postprocess.py` restores after a re-translation. Put new Android-specific code there.

The Kotlin packages stay `com.moblin.android.*`, because the pipeline maps every Swift file to them; only the
application id (`com.loxen.app`) and what users see say Loxen. `platform/loxen/Loxen.kt` holds the name: every
`localized()` string names the app Loxen instead of Moblin, except Moblink and the names of Moblin's own website,
Discord, Mobcam host tool and remote control websites, and `moblin://`, `.moblinSettings` and URLs, which are
lower case. `Bundle.image` shows the Loxen icon wherever Moblin shows its icon or mascot. The hooks in
`tools/hooks/LOXEN.json` replace the icon store with a Loxen page, add the attribution and the MIT license to
About, point Help and support to this repository's issues and name the app in the few texts that do not go
through `localized()`.

The Live Activity is split the same way: `Moblin Live Activity/Shared/MoblinLiveActivity.swift` and
`ModelLiveActivity.swift` are ported by the pipeline onto the ActivityKit shim in `platform/activitykit/`, while the
lock screen layout in `Moblin Live Activity/MoblinLiveActivityApp.swift` is a widget that is not ported. It is drawn
by hand in `platform/activitykit/MoblinLiveActivityApp.kt` as an ongoing notification, so a change to
that Swift file needs the same change there. `tools/sync.py` warns when it happens: `tools/check_hand_ported.py`
compares every Swift file listed in `tools/hand_ported.json` with the hash its Kotlin was last brought in step
with. After updating the Kotlin, run `python tools/check_hand_ported.py --update`.

## First port and manual runs

```sh
python tools/inventory.py --moblin .upstream      # scan the Swift code, write tools/inventory.json
python tools/port.py --dry-run --tier all         # show the plan and cost, no LLM calls
python tools/port.py --tier all                   # translate every file that is not done yet
python tools/fix.py --rounds 3                    # compile and let the model fix errors
python tools/port.py --report-only                # rebuild PORT-REPORT.md
```

Runs can be interrupted and resumed. `tools/port-state.json` remembers, for every Swift file, the checksum
and upstream commit it was ported from. Useful flags: `--provider deepseek`, `--model claude-sonnet-5`,
`--workers 8`, `--include Moblin/Moblink`, `--force`, `--incremental`.

## Tiers

| tier | what | expected result |
|---|---|---|
| logic | plain Swift without Apple frameworks | compiles with small fixes |
| platform | Bluetooth, GPS, sockets, crypto | Android counterparts, needs testing |
| test | Swift Testing suites | JUnit tests for the logic tier |
| ui | SwiftUI | Jetpack Compose with the same structure, needs polish |
| media | camera, encoding, effects, transports | protocols and algorithms translated, hardware is TODO |
| apple_only | Apple Watch, HealthKit | empty stubs so the rest compiles |
| skip | watch app, widgets, Mac, screen recording | not ported |

## Layout

- `tools/inventory.py` scans the Swift code, classifies files and computes dependency order.
- `tools/port.py` translates files and writes Kotlin under `app/src/main/java` and `app/src/test/java`.
- `tools/pbswift.py` translates the SwiftProtobuf-generated Tesla protobufs to Kotlin without a model.
- `tools/prompts/` holds the translation rules per tier.
- `tools/degrade.py`, `tools/sync_report.py`, `tools/repair.py` and `tools/usage_limit.py` hold back what does not build,
  write the report and the issue, and drive the automatic repair. Their tests run with
  `python -m unittest discover -s tools/tests`.
- `tests/` is Moblin's system test suite, copied by every sync; `tools/system_tests.py` runs it against Loxen.
- `app/` is the Android Studio project.
