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

## Usage

```sh
python tools/inventory.py                        # scan ../moblin, write tools/inventory.json
python tools/port.py --dry-run --tier all         # show the plan and cost, no LLM calls
python tools/port.py --tier logic --limit 5       # port five files and look at the result
python tools/port.py                              # logic, platform and test tiers
python tools/port.py --tier ui                    # SwiftUI to Jetpack Compose
python tools/port.py --tier media                 # media pipeline, hardware parts become TODO
python tools/port.py --report-only                # rebuild PORT-REPORT.md
```

Runs can be interrupted and resumed. `tools/port-state.json` remembers the checksum of every ported
file, so a changed Swift file is ported again on the next run and unchanged files are skipped.
Useful flags: `--provider deepseek`, `--model claude-sonnet-5`, `--workers 8`,
`--include Moblin/Moblink`, `--force`.

Ported Kotlin files are overwritten when their Swift source changes. Keep hand-written code in
separate files.

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
