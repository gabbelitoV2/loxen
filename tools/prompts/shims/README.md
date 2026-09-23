# Shim glossary fragments

`tools/port.py` appends every `*.md` file in this directory except this README to the translation prompt of every tier,
under an "Apple API shims" heading, in natural order (`T2.md`, `T3.md`, ..., `T10.md`). After them it adds a
generated "Platform API" section with the public declarations of `platform/**` and `media/MediaSample.kt` (600 lines
at most; a file that does not fit is added to the request of each Swift file that uses one of its names). `tools/fix.py`
gives the compile fixer the same two sections.

So when Erik changes Swift code that uses an Apple API, the incremental sync translates the change against the shims
instead of writing `TODO()` or calling Android APIs directly.

## Rules

- One file per task, `<task>.md`, owned by that task.
- Start with one heading: `## <what> (<task>, package <com.moblin.android.platform.x>)`.
- Then one bullet per Apple API or group of APIs: the Swift call, `->`, and the exact Kotlin call against the shim,
  with argument labels where the shim keeps them. Name the package once per type.
- Say what the shim does not do when a translation could expect it (for example that a call never throws, or that a
  value is an `Int` constant and not an enum).
- Only rules for translating Swift. Implementation notes belong in the Kotlin code, not here.
- Keep it short: every line is sent with every file that is translated.
