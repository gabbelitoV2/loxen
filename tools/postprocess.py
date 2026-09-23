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
CONTEXT_ARG_RE = re.compile(r'context = TODO\("[^"]*"\)')
STATEMENT_TODO_RE = re.compile(r'^(\s*)TODO\("[^"\n]*"\)\s*$', re.M)
EXPRESSION_BODY_RE = re.compile(r"(fun [^\n]*?\)\s*:\s*([\w.<>, ?]+?)\s*=\s*)TODO\([^\n]*\)")
RETURN_TODO_RE = re.compile(r"^(\s*)return TODO\([^\n]*\)\s*$")
FUN_HEADER_RE = re.compile(r"fun [^\n]*?\)\s*:\s*([\w.<>, ?]+?)\s*(?:=|\{)")
GETTER_TODO_RE = re.compile(r"((?:val|var) [\w.<>]+\s*:\s*([\w.<>, ?]+?)\s*\n?\s*get\(\)\s*=\s*)TODO\([^\n]*\)")
DELEGATE_TODO_RE = re.compile(r'(\w+)\((?P<before>[^()]*?)delegate = TODO\("(?P<interface>\w+Delegate) is implemented as Model extension functions"\)')
INTERFACE_CACHE = {}
DEFAULTS = {
    "Boolean": "false", "Int": "0", "Long": "0L", "Double": "0.0", "Float": "0f", "String": '""',
    "ByteArray": "ByteArray(0)", "Unit": "Unit",
}


def add_import(text, statement):
    if re.search(r"^" + re.escape(statement) + r"$", text, re.M):
        return text, False
    if statement.startswith("import com.moblin.android.") and statement.count(".") == 3 and re.search(r"^package com\.moblin\.android$", text, re.M):
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


def default_for(type_name):
    name = type_name.strip()
    if name.endswith("?"):
        return "null"
    if name in DEFAULTS:
        return DEFAULTS[name]
    if re.match(r"^(List|Collection|Iterable)<", name):
        return "emptyList()"
    if name.startswith("MutableList<"):
        return "mutableListOf()"
    if name.startswith("Set<"):
        return "emptySet()"
    if name.startswith("Map<"):
        return "emptyMap()"
    return None


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


def context_todos(text):
    text, n1 = CONTEXT_ARG_RE.subn("context = AppDelegate.context", text)
    text, n2 = re.subn(r'= TODO\("PreviewView needs an Android Context[^"]*"\)', "= PreviewView(AppDelegate.context)", text)
    text, n3 = re.subn(r'WebView\(TODO\("[^"]*"\)\)', "WebView(AppDelegate.context)", text)
    text, n4 = re.subn(r'SpeechToText\(TODO\("context"\)\)', "SpeechToText(AppDelegate.context)", text)
    count = n1 + n2 + n3 + n4
    if count:
        text, _ = add_import(text, "import com.moblin.android.AppDelegate")
    return text, count


def statement_todos(text):
    return STATEMENT_TODO_RE.subn(r"\1Unit", text)


def typed_defaults(text):
    count = 0

    def replace_expression(match):
        nonlocal count
        value = default_for(match.group(2))
        if value is None:
            return match.group(0)
        count += 1
        return match.group(1) + value

    text = EXPRESSION_BODY_RE.sub(replace_expression, text)
    text = GETTER_TODO_RE.sub(replace_expression, text)
    lines = text.split("\n")
    for i, line in enumerate(lines):
        m = RETURN_TODO_RE.match(line)
        if not m:
            continue
        header = None
        for j in range(i - 1, max(-1, i - 60), -1):
            h = FUN_HEADER_RE.search(lines[j])
            if h and lines[j].lstrip().startswith(("fun ", "private fun ", "internal fun ", "override fun ", "suspend fun ", "private suspend fun ")):
                header = h.group(1)
                break
        if header is None:
            continue
        value = default_for(header)
        if value is None:
            continue
        lines[i] = m.group(1) + "return " + value
        count += 1
    return "\n".join(lines), count


def interface_body(name, sources):
    if name in INTERFACE_CACHE:
        return INTERFACE_CACHE[name]
    for text in sources.values():
        m = re.search(r"^interface " + name + r"\b[^\n]*\{", text, re.M)
        if m:
            depth, i = 1, m.end()
            while depth and i < len(text):
                depth += {"{": 1, "}": -1}.get(text[i], 0)
                i += 1
            INTERFACE_CACHE[name] = text[m.end():i - 1]
            return INTERFACE_CACHE[name]
    INTERFACE_CACHE[name] = None
    return None


def parse_methods(body):
    methods = []
    for m in re.finditer(r"^\s*((?:suspend\s+)?)fun\s+(\w+)\s*\(", body, re.M):
        i = m.end()
        depth = 1
        while depth:
            depth += {"(": 1, ")": -1}.get(body[i], 0)
            i += 1
        params = body[m.end():i - 1]
        rest = body[i:body.find("\n", i)].strip()
        ret = rest[1:].strip() if rest.startswith(":") else ""
        names = []
        depth = 0
        current = ""
        stripped = re.sub(r"->", "→", params)
        for ch in stripped + ",":
            if ch in "(<[":
                depth += 1
            if ch in ")>]":
                depth -= 1
            if ch == "," and depth == 0:
                if current.strip():
                    names.append(current.strip().split(":")[0].strip().replace("vararg ", ""))
                current = ""
            else:
                current += ch
        clean_params = re.sub(r"\s+", " ", params.strip()).rstrip(",")
        methods.append((m.group(1).strip(), m.group(2), clean_params, ret, names))
    return methods


def adapter(interface, body, receiver, indent):
    pad = " " * indent
    out = ["object : " + interface + " {"]
    for modifier, name, params, ret, names in parse_methods(body):
        prefix = (modifier + " ") if modifier else ""
        call = receiver + "." + name + "(" + ", ".join(names) + ")"
        if ret and ret != "Unit":
            out.append(pad + "    override " + prefix + "fun " + name + "(" + params + "): " + ret + " = " + call)
        else:
            out.append(pad + "    override " + prefix + "fun " + name + "(" + params + ") {")
            out.append(pad + "        " + call)
            out.append(pad + "    }")
    out.append(pad + "}")
    return "\n".join(out)


def delegate_adapters(text, sources):
    count = 0

    def replace(match):
        nonlocal count
        interface = match.group("interface")
        body = interface_body(interface, sources)
        if body is None:
            return match.group(0)
        position = match.start()
        line_start = text.rfind("\n", 0, position) + 1
        indent = len(text[line_start:position]) - len(text[line_start:position].lstrip())
        preceding = text[:position]
        extension = re.findall(r"^(?:private |internal )?fun Model\.(\w+)\(", preceding, re.M)
        member = re.search(r"^class Model\b", preceding, re.M)
        receiver = "this@Model" if member and not extension else ("this@" + extension[-1] if extension else "this")
        count += 1
        return match.group(1) + "(" + match.group("before") + "delegate = " + adapter(interface, body, receiver, indent)

    return DELEGATE_TODO_RE.sub(replace, text), count


MAIN_ACTIVITY_RE = re.compile(
    r"(class MainActivity[^\n]*\{\s*\n\s*override fun onCreate\(savedInstanceState: Bundle\?\) \{\n\s*super\.onCreate\(savedInstanceState\)\n)"
)
APP_DELEGATE_RE = re.compile(r"(class AppDelegate\s*:\s*Application\(\)[^\n]*\{(?:.*?\n)*?\s*override fun onCreate\(\) \{\n\s*super\.onCreate\(\)\n)")


def host_hooks(text):
    count = 0
    if "class MainActivity" in text and "AndroidHost.onActivityCreated(this)" not in text:
        text, n = MAIN_ACTIVITY_RE.subn(r"\1        com.moblin.android.platform.AndroidHost.onActivityCreated(this)\n", text, count=1)
        count += n
    if "class AppDelegate" in text and "context = applicationContext" not in text:
        text, n = APP_DELEGATE_RE.subn(r"\1        context = applicationContext\n", text, count=1)
        count += n
        if n and "lateinit var context: Context" not in text:
            text = text.replace("    companion object {\n", "    companion object {\n        lateinit var context: Context\n", 1)
            text, _ = add_import(text, "import android.content.Context")
    return text, count


def process_file(text, renamed_names, sources=None):
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
    text, contexts = context_todos(text)
    counts["contexts"] = contexts
    if sources is not None:
        text, adapters = delegate_adapters(text, sources)
        counts["adapters"] = adapters
    text, typed = typed_defaults(text)
    counts["typed"] = typed
    text, statements = statement_todos(text)
    counts["statements"] = statements
    text, hooks = host_hooks(text)
    counts["hooks"] = hooks
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
    sources = {path: path.read_text(encoding="utf-8", errors="replace") for path in files}
    contents = {}
    for path in files:
        original = sources[path]
        text, counts = process_file(original, renamed, sources)
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
    print(f"imports added: getValue {totals.get('getValue', 0)}, setValue {totals.get('setValue', 0)}, localized {totals.get('localized', 0)}")
    print(f"flows exposed as mutable: {totals.get('flows', 0)}  files with backing references renamed: {renames}")
    print(f"composables given LocalModel/LocalOnNavigate defaults: {totals.get('defaults', 0)}")
    print(f"context TODOs resolved: {totals.get('contexts', 0)}  delegate adapters generated: {totals.get('adapters', 0)}")
    print(f"typed TODOs defaulted: {totals.get('typed', 0)}  statement TODOs turned into no-ops: {totals.get('statements', 0)}")


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Deterministic fixes applied to every generated Kotlin file.")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    run(args.dry_run)


if __name__ == "__main__":
    main()
