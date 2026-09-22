#!/usr/bin/env python3
import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIRS = [ROOT / "app/src/main/java", ROOT / "app/src/test/java"]

BACKING_PAIR_RE = re.compile(
    r"^(?P<indent>[ \t]*)(?:internal |private |public |protected )?val _(?P<name>\w+)(?P<type>\s*:\s*[^=\n]+?)?\s*=\s*"
    r"MutableStateFlow(?P<init>.*)\n"
    r"[ \t]*(?:internal |private |public |protected )?val (?P=name)\s*(?::[^=\n]+)?=\s*_(?P=name)\.asStateFlow\(\)\n",
    re.M,
)
DELEGATE_RE = re.compile(r"\bby\s+(remember\b|mutableStateOf\b|mutableIntStateOf\b|mutableFloatStateOf\b|[\w.]+\.collectAsState\()")
LOCALIZED_RE = re.compile(r"\blocalized\(")
COMPOSABLE_RE = re.compile(r"@Composable\s+(?:(?:private|internal|public)\s+)?fun\s+\w+\s*\(")
MODEL_PARAM_RE = re.compile(r"\bmodel:\s*Model\b(?!\s*[=?.<])")
NAVIGATE_PARAM_RE = re.compile(r"\bonNavigate:\s*\(String\)\s*->\s*Unit\b(?!\s*=)")


def default_composable_params(text):
    pieces = []
    position = 0
    changed = 0
    for match in COMPOSABLE_RE.finditer(text):
        start = match.end()
        depth = 1
        index = start
        while index < len(text) and depth:
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
            index += 1
        params = text[start:index - 1]
        new_params = MODEL_PARAM_RE.sub("model: Model = LocalModel.current", params)
        new_params = NAVIGATE_PARAM_RE.sub("onNavigate: (String) -> Unit = LocalOnNavigate.current", new_params)
        if new_params != params:
            changed += 1
        pieces.append(text[position:start])
        pieces.append(new_params)
        position = index - 1
    pieces.append(text[position:])
    text = "".join(pieces)
    if "LocalModel.current" in text:
        text, _ = add_import(text, "import com.moblin.android.LocalModel")
    if "LocalOnNavigate.current" in text:
        text, _ = add_import(text, "import com.moblin.android.LocalOnNavigate")
    return text, changed


def add_import(text, statement):
    if re.search(r"^" + re.escape(statement) + r"$", text, re.M):
        return text, False
    imports = list(re.finditer(r"^import .+$", text, re.M))
    if imports:
        position = imports[-1].end()
        return text[:position] + "\n" + statement + text[position:], True
    package = re.search(r"^package .+$", text, re.M)
    if package:
        position = package.end()
        return text[:position] + "\n\n" + statement + text[position:], True
    return statement + "\n" + text, True


def expose_mutable_flows(text):
    names = []

    def replace(match):
        names.append(match.group("name"))
        type_part = match.group("type") or ""
        return f"{match.group('indent')}val {match.group('name')}{type_part} = MutableStateFlow{match.group('init')}\n"

    text = BACKING_PAIR_RE.sub(replace, text)
    return text, names


def rename_backing_references(text, names):
    changed = False
    for name in names:
        new_text = re.sub(r"\b_" + re.escape(name) + r"\b", name, text)
        if new_text != text:
            text = new_text
            changed = True
    return text, changed


def process_file(text, renamed_names):
    counts = {}
    package = re.search(r"^package (\S+)$", text, re.M)
    package_name = package.group(1) if package else ""
    if DELEGATE_RE.search(text):
        text, added = add_import(text, "import androidx.compose.runtime.getValue")
        counts["getValue"] = int(added)
        if re.search(r"\bvar \w+\s+by\s+(remember|mutableStateOf|mutableIntStateOf|mutableFloatStateOf)\b", text):
            text, added = add_import(text, "import androidx.compose.runtime.setValue")
            counts["setValue"] = int(added)
    if LOCALIZED_RE.search(text) and package_name != "com.moblin.android":
        text, added = add_import(text, "import com.moblin.android.localized")
        counts["localized"] = int(added)
    text, names = expose_mutable_flows(text)
    counts["flows"] = len(names)
    renamed_names.update(names)
    text, defaults = default_composable_params(text)
    counts["defaults"] = defaults
    return text, counts


def kotlin_files():
    for directory in SOURCE_DIRS:
        if directory.exists():
            yield from sorted(directory.rglob("*.kt"))


def run(dry_run):
    totals = {}
    renamed = set()
    changed_files = 0
    files = list(kotlin_files())
    contents = {}
    for path in files:
        original = path.read_text(encoding="utf-8", errors="replace")
        text, counts = process_file(original, renamed)
        contents[path] = (original, text)
        for key, value in counts.items():
            totals[key] = totals.get(key, 0) + value
    renames = 0
    for path in files:
        original, text = contents[path]
        text, changed = rename_backing_references(text, renamed)
        renames += int(changed)
        if text != original:
            changed_files += 1
            if not dry_run:
                path.write_text(text, encoding="utf-8", newline="\n")
    print(f"files: {len(files)}  changed: {changed_files}")
    print(f"getValue imports: {totals.get('getValue', 0)}  setValue imports: {totals.get('setValue', 0)}  "
          f"localized imports: {totals.get('localized', 0)}")
    print(f"flows exposed as mutable: {totals.get('flows', 0)}  files with backing references renamed: {renames}")
    print(f"composables given LocalModel/LocalOnNavigate defaults: {totals.get('defaults', 0)}")


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Deterministic fixes applied to every generated Kotlin file.")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    run(args.dry_run)


if __name__ == "__main__":
    main()
