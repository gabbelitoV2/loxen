# Moblin for Android

An automated port of [Moblin](https://github.com/eerimoq/moblin), the iOS IRL streaming app, to
Android. A small pipeline reads the Swift sources in the sibling `../moblin` checkout, sorts every file
by how portable it is, and lets an LLM translate it file by file into the Android Studio project under
`app/`. The Kotlin keeps the same structure, type names and function names as the Swift code. Anything
without an Android counterpart is marked `TODO("...")` and listed in `PORT-REPORT.md`.

This is a starting point, not a finished app. Camera capture, MediaCodec encoding, OpenGL video
effects and the libsrt binding have to be written by hand.

## Requirements

- Python 3.10 or newer.
- One of:
  - Claude Code logged in (`claude` in a terminal, then `/login`). Used when no API key is set.
  - `pip install anthropic` and `ANTHROPIC_API_KEY`.
  - `pip install anthropic` and `DEEPSEEK_API_KEY`, then `--provider deepseek`. Uses `deepseek-flash`,
    the current DeepSeek V4.1 Flash. About forty times cheaper than Claude Opus. Prices double during
    peak hours, 01:00-04:00 and 06:00-10:00 UTC on weekdays.

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

`python tools/sync.py --dry-run` shows the new upstream commits and the files that would be ported.

The GitHub Actions workflow in `.github/workflows/android.yml` runs the same sync every night and on
demand, then builds `app-debug.apk` and attaches it to the run. It needs the repository secret
`DEEPSEEK_API_KEY`.

## Hand-written Android code

Everything in `app/src/main/java/com/moblin/android/platform/` is written by hand and never generated:
camera, encoding, transports and other Android APIs. Generated files call into it through one-line hooks,
which `tools/postprocess.py` restores after a re-translation. Put new Android-specific code there.

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
- `tools/prompts/` holds the translation rules per tier.
- `app/` is the Android Studio project.
