# One-line hooks

Generated Kotlin files are rewritten whenever the Swift file changes upstream. A hook is a one-line Android fix inside a
generated file that must survive that: it connects the generated code to the hand-written platform layer, or fixes one
line the translation gets wrong. `tools/postprocess.py` re-applies every hook after its other rules, so a hook costs
nothing after a re-translation.

Each task keeps its hooks in its own file, `tools/hooks/<task>.json`. Nobody edits another task's file.

## Format

```json
{
  "task": "T4",
  "hooks": [
    {"id": "H4.1", "file": "media/haishinkit/media/video/VideoUnit.kt", "scope": "fun attachDefault",
     "op": "replace", "old": "<exact old line>", "new": "<exact new line>", "occurrence": 1},
    {"id": "H4.14", "file": "media/haishinkit/media/video/PreviewView.kt", "scope": "fun setup",
     "op": "insert_after", "anchor": "    private fun setup() {", "new": "        layer.setup()"},
    {"id": "H4.17", "file": "media/haishinkit/media/video/PreviewView.kt", "op": "add_import",
     "new": "import com.moblin.android.platform.video.layer"},
    {"id": "G1", "file": "*", "op": "replace", "old": "import android.media.Image",
     "new": "import com.moblin.android.platform.video.CVPixelBuffer as Image"}
  ]
}
```

- `id`: unique across all hook files. `H<task>.<n>` for a single file, `G<n>` for a global hook.
- `file`: path relative to `app/src/main/java/com/moblin/android/`, with forward slashes. `*` means every `.kt` file
  there except `platform/**` and `media/MediaSample.kt`. Other glob patterns (`videoeffects/*.kt`) work the same way.
  A global hook changes every file that has the old line and ignores the others.
- `scope` (optional): `fun NAME`, `val NAME`, `var NAME`, `class NAME`, `object NAME` or `interface NAME`. It selects
  the first declaration line that matches `\b(fun|val|var|class|object|interface) ([\w.]+\.)?NAME\b` outside strings
  and comments, so a receiver (`fun Model.NAME`) is matched too. Write `fun Model.NAME` to require that receiver.
  The scope runs to the brace that closes the body. For an expression body (`= ...`) or a declaration without a body,
  it runs to the end of the expression, which is the next line at the declaration's indentation or less. A `val` or
  `var` in a constructor parameter list ends at its comma.
  - `scope_occurrence` (optional, default 1) picks the n-th matching declaration, for overloads.
  - For overloads, the scope can also carry lines to look for: `"fun getBuiltinCameraDevices(\n    videoSource"`
    picks the first matching declaration whose line or next 3 lines contain `videoSource`.
- `op`:
  - `replace`: the line equal to `old` becomes `new`.
  - `delete`: the line equal to `old` is removed. Its `occurrence` must be the number of lines equal to `old` in the
    scope, so that nothing matches once it is removed; otherwise every run would remove one more line, and the hook
    reports MISSING instead. Only one `delete` per file, scope and `old`.
  - `insert_after`: `new` is inserted after the line equal to `anchor`.
  - `add_import`: `new` (an `import` line) is added after the last import. No scope.
- `old`, `anchor` and `new` are whole lines, indentation included, and are compared exactly. Never put a newline in
  them.
- `occurrence` (optional, default 1): which matching line in the scope. It counts lines equal to `old` (or `anchor`).
  When the scope has fewer of those, it counts lines whose text without indentation matches, and the chosen line must
  still match exactly. Hooks that share the same file, scope and `old` count the lines the others already replaced,
  so their order in the file does not matter.
- `note` (optional): free text for people, ignored by the tools.

## Behaviour

- Idempotent: a hook whose `new` line is already in its scope is skipped. A `delete` whose `old` line is gone is done,
  and so is a global `delete` that no file matches any more.
- MISSING: neither `old` (or `anchor`) nor `new` is in the scope, or the scope or file is gone. That means the Swift
  code under the hook changed. Look at the new generated code and re-derive the hook by hand.
- Global hooks run before file hooks, then hooks run in file order (`T2.json` before `T10.json`).
- `python tools/postprocess.py` applies the hooks and prints `hooks: N applied, M missing`. `--dry-run` only reports,
  `--no-hooks` skips them, `--verbose` lists every hook.
- `python tools/check_hooks.py` changes nothing. It exits with 1 when a hook is missing, not applied yet or invalid.
  `--task T4` checks one task, `--verbose` lists every hook.
- `tools/port.py` runs the postprocess on the files it writes (turn it off with `--no-postprocess`), `tools/fix.py`
  runs it before every compile and on every file it fixes, and `tools/sync.py` runs it once more after `fix.py`. When a
  hook is missing in a file the sync re-translated, the sync puts back the previous version of that file and reports the
  hook in `tools/sync-report.json`.

## Rules for writing hooks

- One line per hook. Prefer fully qualified names (`com.moblin.android.platform.core.PipelineThread.dispatcher`) over
  an extra `add_import` hook.
- Never chain hooks: a hook's `new` line must not be another hook's `old` line, or the first one reports MISSING.
- Two hooks with the same file, scope and `new` line cannot be told apart once applied; give each its own `new`.
- A `delete` hook cannot notice upstream drift. Pair it with a `replace` in the same scope.
- Apply your hooks to the generated files yourself when you add them, and run `python tools/check_hooks.py`.
- Hooks are for generated files only. Hand-written files (`platform/**`, `media/MediaSample.kt`) are edited directly.
