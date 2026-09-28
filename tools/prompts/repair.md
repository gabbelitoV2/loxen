# Repair the Android port

You run unattended in GitHub Actions on a checkout of the `main` branch of moblin-android, the Kotlin/Jetpack Compose
port of Moblin, the iOS IRL streaming app by Erik Moqvist (eerimoq). Erik only changes the Swift app. The Android port
follows it automatically: every night `tools/sync.py` translates the changed Swift files, builds the app and commits
what builds. What it could not bring in is listed in the report at the end of this prompt. Your task is to fix the
port so that every item in the report goes away. Nobody answers questions during the run; do the work, then stop.

## How to run commands here

This session is headless: ending your turn ends the run, and nothing will ever wake you up again. There are no
background-task notifications in this environment.

- Run every command in the foreground, including `tools/port.py` and Gradle. Commands may run for up to 60 minutes;
  pass a timeout of up to 3600000 ms when a command can take longer than a few minutes. Never use
  `run_in_background`, never start processes with `&` or `nohup`, and never end your turn to wait for something.
- When a translation run would take longer than about 45 minutes, split `--include` into smaller batches and run
  them one after the other.
- Only end your turn when the checks below pass, or when you have run out of ideas. Then summarise what you did and
  what is left.

## Where things are

- `.upstream/` is the Swift source of eerimoq/moblin, checked out at the upstream commit of the report. It is read
  only: never edit it, never commit or push to it, and never contact eerimoq/moblin or any other upstream.
- `tools/port.py` translates Swift files to Kotlin under `app/src/main/java` and `app/src/test/java`.
  `tools/port-state.json` records for every Swift file the checksum and upstream commit it was last ported from.
- `app/src/main/java/com/moblin/android/platform/**` is hand-written Android code behind Apple-named shims
  (`AVCaptureDevice`, `CBCentralManager`, `NWConnection`, ...). Generated code calls them with the Swift shape.
- `tools/prompts/shims/*.md` tell the translation model how each Apple API maps to a shim, and `tools/prompts/*.md`
  hold the translation rules per tier. `python tools/port.py --print-prompt "<swift path>" --incremental` shows the
  prompt a file gets.
- `tools/hooks/*.json` are one-line fixes that `tools/postprocess.py` re-applies to generated files after every
  translation. `tools/hooks/README.md` describes them.
- `tools/pbswift.py` translates the Tesla protobufs without a model.
- `tools/hand_ported.json` lists Swift files whose Kotlin is written by hand; `tools/check_hand_ported.py` reports when
  they change upstream.

## How to fix

Prefer fixes that survive the next sync, in this order:

1. Fix the cause in a shim under `platform/**` or in the guidance in `tools/prompts/shims/*.md` or
   `tools/prompts/*.md`, then translate the Swift file again through the pipeline:
   `python tools/port.py --tier all --incremental --provider deepseek --include "<swift path>"`. It runs
   `tools/postprocess.py` on the result and updates `tools/port-state.json`. DEEPSEEK_API_KEY is set.
2. A hook, only as a one-line fix in a generated file, following `tools/hooks/README.md`. Never several hooks to rewrite
   logic, and never hooks in hand-written files.
3. Editing a generated Kotlin file directly is the last resort, because the next translation of that Swift file
   replaces it unless a hook keeps the line.

Per kind of item:

- Held back: the Kotlin file still matches the Swift it was last ported from and its entry in
  `tools/port-state.json` is stale on purpose, so the next sync retries it. Translate it again as in step 1, compile,
  and fix what breaks. A held-back file is done when `python tools/port.py --tier all --dry-run` no longer lists it.
- Translation failed: run `tools/port.py` again for that file.
- Missing hook: the generated line under the hook changed. Re-derive the hook as `tools/hooks/README.md` says.
- Swift files ported by hand changed upstream: bring the Kotlin in step by hand, then run
  `python tools/check_hand_ported.py --update`.
- Effects checks: add the missing shim member. Add an entry to `tools/effects_known_gaps.json` only for a real gap and
  with the reason.
- Tesla protobufs: teach `tools/pbswift.py` the new construct, run `python tools/pbswift.py`, then
  `python tools/pbswift.py --check`.
- The sync stopped: reproduce it with `python tools/sync.py --ref <upstream commit of the report>` (without
  `--commit` and `--push`) and fix the cause. When it was a one-off (a network error, a timeout) and the sync now runs
  through, keep what it leaves in the working tree.
- The app does not build: the sync committed nothing but the report. When `main` does not compile, fix the compile
  errors, in hand-written code when that is where they are. When `main` compiles, run the sync as above: it ports the
  files, holds back what does not build and leaves the result in the working tree. Then fix what it holds back.

When something cannot be fixed properly, leave it held back (the app still builds that way) and say why in your
summary. Do not hack around it.

## Rules

- Do not write code comments in Kotlin or Python. `TODO("reason")` is the only allowed marker in Kotlin.
- Keep the Swift structure, type names and function names in Kotlin. Keep diffs small.
- Do not commit, push, open pull requests or call `gh`. The workflow verifies your changes and pushes them.
- Do not change `.github/`, `tools/repair.py`, `tools/sync_report.py`, `tools/usage_limit.py`, `tools/check_hooks.py`,
  `tools/known_test_failures.json`, `tools/prompts/repair.md` or `tools/tests/`. Changes there are thrown away.
- Do not delete, disable or weaken unit tests.
- Run every command from the repository root, without `cd`. Leave no scratch files; new files belong under `app/`
  or `tools/`.
- Compile quickly with `./gradlew :app:compileDebugUnitTestKotlin -q`.

## Before you stop

Everything below must pass, because the workflow runs the same checks and pushes nothing when one fails:

- `python tools/check_hooks.py` reports 0 missing and nothing not applied.
- `python tools/pbswift.py --check`
- `./gradlew :app:assembleDebug -q`
- `./gradlew :app:testDebugUnitTest --continue -q` with no failures except the known ones in
  `tools/known_test_failures.json` (four in RecorderSuite and MpegTsReaderSuite.ffmpegAudioOnlyPeriodicBeep).

Finish with a short plain-text summary: what you changed, what is left and why. It is posted on the issue.
