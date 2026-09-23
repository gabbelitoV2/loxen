#!/usr/bin/env python3
import argparse
import fnmatch
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIRS = [ROOT / "app/src/main/java", ROOT / "app/src/test/java"]
KOTLIN_ROOT = ROOT / "app/src/main/java/com/moblin/android"
HOOKS_DIR = ROOT / "tools/hooks"

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
    lines = text.split("\n")
    count = 0
    for i, line in enumerate(lines):
        if not STATEMENT_TODO_RE.match(line):
            continue
        previous = next((lines[j].rstrip() for j in range(i - 1, -1, -1) if lines[j].strip()), "")
        if previous.endswith(("=", "(", ",", "->", "?:", "return", "else", "&&", "||")):
            continue
        lines[i] = STATEMENT_TODO_RE.sub(r"\1Unit", line)
        count += 1
    return "\n".join(lines), count


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


CALL_ARGS = r"\(([^()]*(?:\([^()]*\)[^()]*)*)\)"
PADDING_BACKGROUND_RE = re.compile(r"\.padding" + CALL_ARGS + r"(\s*)\.background" + CALL_ARGS)
BACKGROUND_CLIP_RE = re.compile(
    r"\.background" + CALL_ARGS + r"((?:\s*\.(?:padding|widthIn|heightIn|width|height|size)\([^()]*\))*\s*)\.clip" + CALL_ARGS
)


CLIP_PADDING_BACKGROUND_RE = re.compile(
    r"\.clip" + CALL_ARGS + r"((?:\s*\.(?:padding|widthIn|heightIn|width|height|size)\([^()]*\))+)\s*\.background" + CALL_ARGS
)


def modifier_order(text):
    count = 0

    def swap_padding_background(match):
        nonlocal count
        count += 1
        return ".background(" + match.group(3) + ")" + match.group(2) + ".padding(" + match.group(1) + ")"

    def swap_background_clip(match):
        nonlocal count
        count += 1
        return ".clip(" + match.group(3) + ").background(" + match.group(1) + ")" + match.group(2)

    def fix_clip_padding_background(match):
        nonlocal count
        count += 1
        return ".clip(" + match.group(1) + ").background(" + match.group(3) + ")" + match.group(2)

    previous = None
    while previous != text:
        previous = text
        text = PADDING_BACKGROUND_RE.sub(swap_padding_background, text)
    text = BACKGROUND_CLIP_RE.sub(swap_background_clip, text)
    text = CLIP_PADDING_BACKGROUND_RE.sub(fix_clip_padding_background, text)
    return text, count


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


TEST_CLASS_RE = re.compile(r"^((?:@\w+(?:\([^)\n]*\))?\n)*)(class \w+Suite\b)", re.M)


def robolectric_runner(text):
    if "org.junit.Test" not in text or "@RunWith(" in text:
        return text, 0
    text, n = TEST_CLASS_RE.subn(r"\1@RunWith(RobolectricTestRunner::class)\n\2", text, count=1)
    if n:
        text, _ = add_import(text, "import org.junit.runner.RunWith")
        text, _ = add_import(text, "import org.robolectric.RobolectricTestRunner")
    return text, n


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
    if "@Composable" in text:
        text, modifiers = modifier_order(text)
        counts["modifiers"] = modifiers
    text, runners = robolectric_runner(text)
    counts["runners"] = runners
    return text, counts


def skip_string(text, i):
    n = len(text)
    if text.startswith('"""', i):
        j = i + 3
        while j < n:
            if text.startswith('"""', j):
                j += 3
                while j < n and text[j] == '"':
                    j += 1
                return j
            if text.startswith("${", j):
                j = skip_template(text, j + 2)
                continue
            j += 1
        return n
    j = i + 1
    while j < n:
        c = text[j]
        if c == "\\":
            j += 2
            continue
        if c == '"':
            return j + 1
        if c == "\n":
            return j
        if text.startswith("${", j):
            j = skip_template(text, j + 2)
            continue
        j += 1
    return n


def skip_char(text, i):
    j = i + 1
    if j < len(text) and text[j] == "\\":
        j += 2
    else:
        j += 1
    end = text.find("'", j, j + 8)
    return end + 1 if end != -1 else i + 1


def skip_block_comment(text, i):
    n = len(text)
    depth = 0
    j = i
    while j < n:
        if text.startswith("/*", j):
            depth += 1
            j += 2
        elif text.startswith("*/", j):
            depth -= 1
            j += 2
            if depth == 0:
                return j
        else:
            j += 1
    return n


def skip_template(text, j):
    n = len(text)
    depth = 1
    while j < n:
        c = text[j]
        if c == '"':
            j = skip_string(text, j)
            continue
        if c == "'":
            j = skip_char(text, j)
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return j + 1
        j += 1
    return n


def mask_kotlin(text):
    pieces = []
    n = len(text)
    start = 0
    i = 0

    def blank(segment, fill):
        return "".join(ch if ch == "\n" else fill for ch in segment)

    while i < n:
        c = text[i]
        if c == "/" and text.startswith("//", i):
            end = text.find("\n", i)
            end = n if end == -1 else end
            pieces.append(text[start:i])
            pieces.append(" " * (end - i))
            i = start = end
        elif c == "/" and text.startswith("/*", i):
            end = skip_block_comment(text, i)
            pieces.append(text[start:i])
            pieces.append(blank(text[i:end], " "))
            i = start = end
        elif c == '"':
            end = skip_string(text, i)
            pieces.append(text[start:i])
            pieces.append('"' + blank(text[i + 1:end], "x"))
            i = start = end
        elif c == "'":
            end = skip_char(text, i)
            pieces.append(text[start:i])
            pieces.append("'" + blank(text[i + 1:end], "x"))
            i = start = end
        else:
            i += 1
    pieces.append(text[start:])
    return "".join(pieces)


SCOPE_RE = re.compile(r"^(fun|val|var|class|object|interface)\s+([\w.]+)")
HOOK_OPS = ("replace", "delete", "insert_after", "add_import")
HOOK_KEYS = {"id", "file", "scope", "scope_occurrence", "op", "old", "new", "anchor", "occurrence", "note"}
CONTINUATION_ENDINGS = (",", "(", "[", "=", ":", "->", ".", "?:", "&&", "||", "+", "-", "*")
CONTINUATION_STARTS = (".", "?.", "?:", "&&", "||", ":", "=", "{", ")", "]", "->", "where ", "by ")
ACCESSOR_RE = re.compile(r"^(?:(?:private|protected|internal|public)\s+)?(?:get|set)\b")


def natural_key(name):
    return [int(part) if part.isdigit() else part for part in re.split(r"(\d+)", name)]


def indentation(line):
    return len(line) - len(line.lstrip(" \t"))


class KotlinText:
    def __init__(self, text):
        self.text = text
        self.lines = text.split("\n")
        self.masked = mask_kotlin(text)
        self.masked_lines = self.masked.split("\n")
        self.starts = []
        offset = 0
        for line in self.lines:
            self.starts.append(offset)
            offset += len(line) + 1

    def line_of(self, index):
        low, high = 0, len(self.starts) - 1
        while low < high:
            middle = (low + high + 1) // 2
            if self.starts[middle] <= index:
                low = middle
            else:
                high = middle - 1
        return low

    def matching_brace(self, index):
        depth = 0
        for j in range(index, len(self.masked)):
            c = self.masked[j]
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    return j
        return len(self.masked) - 1

    def next_code_line(self, line):
        for j in range(line + 1, len(self.masked_lines)):
            if self.masked_lines[j].strip():
                return j
        return None

    def continues(self, line, indent, mode):
        current = self.masked_lines[line].rstrip()
        if current.endswith(CONTINUATION_ENDINGS) and not current.endswith(("++", "--")):
            return True
        following = self.next_code_line(line)
        if following is None:
            return False
        stripped = self.masked_lines[following].strip()
        if stripped.startswith(CONTINUATION_STARTS):
            return True
        deeper = indentation(self.masked_lines[following]) > indent
        if mode == "header":
            return deeper and bool(ACCESSOR_RE.match(stripped))
        return deeper

    def scope_end(self, line):
        masked = self.masked
        indent = indentation(self.masked_lines[line])
        keyword = re.search(r"\b(fun|val|var|class|object|interface)\b", self.masked_lines[line])
        is_property = keyword is not None and keyword.group(1) in ("val", "var")
        mode = "header"
        parens = 0
        angles = 0
        braces = 0
        i = self.starts[line]
        while i < len(masked):
            c = masked[i]
            if c in "([":
                parens += 1
            elif c in ")]":
                parens -= 1
                if parens < 0 and braces == 0:
                    closing = self.line_of(i)
                    before = masked[self.starts[closing]:i]
                    return max(line, closing - 1) if not before.strip() else closing
            elif mode == "header" and c == "<":
                angles += 1
            elif mode == "header" and c == ">" and masked[i - 1] != "-":
                angles -= 1
            elif c == "," and is_property and parens <= 0 and braces == 0 and (mode == "expression" or angles <= 0):
                return self.line_of(i)
            elif c == "{":
                if mode == "header" and parens <= 0:
                    return self.line_of(self.matching_brace(i))
                braces += 1
            elif c == "}":
                braces -= 1
                if braces < 0:
                    closing = self.line_of(i)
                    before = masked[self.starts[closing]:i]
                    return max(line, closing - 1) if not before.strip() else closing
            elif c == "=" and mode == "header" and parens <= 0 and is_assignment(masked, i):
                mode = "expression"
            elif c == "\n" and parens <= 0 and braces == 0:
                current = self.line_of(i)
                if not self.continues(current, indent, mode):
                    return current
            i += 1
        return len(self.lines) - 1

    def find_scope(self, scope, occurrence=1):
        parsed = parse_scope(scope)
        if parsed is None:
            return None
        kind, name, context = parsed
        if "." in name:
            receiver, simple = name.rsplit(".", 1)
            target = re.escape(receiver) + r"(?:<[^>\n]*>)?\??\." + re.escape(simple)
        else:
            target = r"(?:[\w.<>?*, ]+\.)?" + re.escape(name)
        pattern = re.compile(r"\b" + kind + r"\s+(?:<[^>\n]*>\s*)?" + target + r"(?!\w)")
        seen = 0
        for index, line in enumerate(self.masked_lines):
            if not pattern.search(line):
                continue
            if context:
                window = "\n".join(self.lines[index:index + 4])
                if not all(part in window for part in context):
                    continue
            seen += 1
            if seen == occurrence:
                return index, self.scope_end(index)
        return None


_KOTLIN_TEXTS = {}


def kotlin_text(text):
    parsed = _KOTLIN_TEXTS.get(text)
    if parsed is None:
        if len(_KOTLIN_TEXTS) >= 8:
            _KOTLIN_TEXTS.clear()
        parsed = _KOTLIN_TEXTS[text] = KotlinText(text)
    return parsed


def is_assignment(masked, i):
    previous = masked[i - 1] if i > 0 else ""
    following = masked[i + 1] if i + 1 < len(masked) else ""
    return previous not in "=!<>+-*/%" and following not in "=>"


def parse_scope(scope):
    if not isinstance(scope, str) or not scope.strip():
        return None
    first, *rest = scope.strip("\n").split("\n")
    match = SCOPE_RE.match(first.strip())
    if not match:
        return None
    context = [part.strip() for part in rest if part.strip()]
    return match.group(1), match.group(2), context


def is_glob(pattern):
    return any(ch in pattern for ch in "*?[")


def excluded_from_hooks(rel):
    return rel.startswith("platform/") or rel == "media/MediaSample.kt"


def single_line(value):
    return isinstance(value, str) and "\n" not in value and "\r" not in value


def validate_hook(hook):
    problems = []
    if not isinstance(hook.get("id"), str) or not hook["id"].strip():
        problems.append("missing id")
    file = hook.get("file")
    if not isinstance(file, str) or not file.strip():
        problems.append("missing file")
    elif file.startswith("/") or "\\" in file or ".." in file.split("/"):
        problems.append("file must be a path relative to app/src/main/java/com/moblin/android with forward slashes")
    elif not is_glob(file) and excluded_from_hooks(file):
        problems.append("hand-written files never get hooks")
    op = hook.get("op")
    if op not in HOOK_OPS:
        problems.append(f"op must be one of {', '.join(HOOK_OPS)}")
    required = {"replace": ("old", "new"), "delete": ("old",), "insert_after": ("anchor", "new"), "add_import": ("new",)}
    for key in required.get(op, ()):
        if not single_line(hook.get(key)) or not hook.get(key).strip():
            problems.append(f"{key} must be one non-empty line")
    if op == "replace" and hook.get("old") == hook.get("new"):
        problems.append("old and new are the same line")
    if op == "add_import":
        if isinstance(hook.get("new"), str) and not hook["new"].startswith("import "):
            problems.append("add_import needs a line starting with 'import '")
        if isinstance(file, str) and is_glob(file):
            problems.append("add_import needs a single file")
        if "scope" in hook:
            problems.append("add_import takes no scope")
    for key in ("occurrence", "scope_occurrence"):
        value = hook.get(key, 1)
        if isinstance(value, bool) or not isinstance(value, int) or value < 1:
            problems.append(f"{key} must be an integer of at least 1")
    if "scope" in hook and parse_scope(hook["scope"]) is None:
        problems.append("scope must look like 'fun NAME', 'val NAME', 'var NAME', 'class NAME', 'object NAME' or 'interface NAME'")
    return problems


def load_hooks(hooks_dir=None):
    hooks_dir = Path(hooks_dir or HOOKS_DIR)
    hooks = []
    problems = []
    warnings = []
    if not hooks_dir.exists():
        return hooks, problems, warnings
    seen = {}
    for path in sorted(hooks_dir.glob("*.json"), key=lambda p: natural_key(p.name)):
        try:
            data = json.loads(path.read_text(encoding="utf-8-sig"))
        except (OSError, ValueError) as error:
            problems.append(f"{path.name}: cannot be read: {error}")
            continue
        entries = data.get("hooks") if isinstance(data, dict) else None
        if not isinstance(entries, list):
            problems.append(f"{path.name}: expected an object with a 'hooks' list")
            continue
        task = data.get("task") or path.stem
        for index, raw in enumerate(entries):
            if not isinstance(raw, dict):
                problems.append(f"{path.name} #{index + 1}: a hook must be an object")
                continue
            hook = dict(raw)
            label = f"{path.name} {hook.get('id', '#' + str(index + 1))}"
            unknown = sorted(set(hook) - HOOK_KEYS)
            if unknown:
                warnings.append(f"{label}: unknown keys {', '.join(unknown)}")
            errors = validate_hook(hook)
            if errors:
                problems.append(f"{label}: " + "; ".join(errors))
                continue
            if hook["id"] in seen:
                problems.append(f"{label}: id already used in {seen[hook['id']]}")
                continue
            seen[hook["id"]] = path.name
            hook["_source"] = path.name
            hook["_task"] = task
            hook["_order"] = len(hooks)
            hooks.append(hook)
    groups = {}
    for hook in hooks:
        if hook["op"] in ("replace", "insert_after"):
            key = (hook["file"], hook.get("scope"), hook.get("scope_occurrence", 1), hook["new"])
            groups.setdefault(key, []).append(hook["id"])
    for ids in groups.values():
        if len(ids) > 1:
            problems.append(f"{', '.join(ids)}: same file, scope and new line, so they cannot be told apart once applied")
    deletes = {}
    for hook in hooks:
        if hook["op"] == "delete":
            key = (hook["file"], hook.get("scope"), hook.get("scope_occurrence", 1), hook["old"])
            deletes.setdefault(key, []).append(hook["id"])
    for ids in deletes.values():
        if len(ids) > 1:
            problems.append(f"{', '.join(ids)}: two deletes of the same line in the same scope are not idempotent; "
                            "give each its own scope")
    olds = {}
    for hook in hooks:
        if hook["op"] == "replace":
            olds.setdefault((hook["file"], hook["old"]), []).append(hook["id"])
    for hook in hooks:
        if hook["op"] in ("replace", "insert_after") and (hook["file"], hook["new"]) in olds:
            others = [other for other in olds[(hook["file"], hook["new"])] if other != hook["id"]]
            if others:
                warnings.append(f"{hook['id']}: its new line is the old line of {', '.join(others)}; chained hooks report MISSING")
    return hooks, problems, warnings


def hook_label(hook):
    scope = f" [{hook['scope'].splitlines()[0]}]" if hook.get("scope") else ""
    return f"{hook['id']} ({hook['_source']}) {hook['file']}{scope}"


def same_line(line, expected):
    return line.rstrip("\r") == expected


def locate(region, expected, occurrence, alternatives=()):
    for normalize in (lambda line: line, lambda line: line.strip()):
        wanted = normalize(expected)
        others = {normalize(line) for line in alternatives}
        seen = 0
        for index, line in enumerate(region):
            candidate = normalize(line.rstrip("\r"))
            if candidate == wanted or candidate in others:
                seen += 1
                if seen == occurrence:
                    return index if line.rstrip("\r") == expected else None
    return None


def sibling_news(hook, hooks):
    if hook["op"] != "replace":
        return ()
    return tuple(
        other["new"] for other in hooks
        if other is not hook and other["op"] == "replace" and other["file"] == hook["file"]
        and other.get("scope") == hook.get("scope") and other.get("scope_occurrence", 1) == hook.get("scope_occurrence", 1)
        and other["old"] == hook["old"]
    )


def run_hook(text, hook, siblings=(), apply=True):
    if hook["op"] == "add_import":
        if any(same_line(line, hook["new"]) for line in text.split("\n")):
            return "applied", text, ""
        if not apply:
            return "pending", text, ""
        new_text, added = add_import(text, hook["new"])
        return ("changed" if added else "applied"), new_text, ""
    source = kotlin_text(text) if hook.get("scope") else None
    lines = list(source.lines) if source else text.split("\n")
    first, last = 0, len(lines) - 1
    if source:
        found = source.find_scope(hook["scope"], hook.get("scope_occurrence", 1))
        if found is None:
            return "missing", text, "scope not found"
        first, last = found
    region = lines[first:last + 1]
    occurrence = hook.get("occurrence", 1)
    op = hook["op"]
    if op in ("replace", "insert_after") and any(same_line(line, hook["new"]) for line in region):
        return "applied", text, ""
    if op == "replace":
        index = locate(region, hook["old"], occurrence, siblings)
        if index is None:
            return "missing", text, "neither the old nor the new line was found"
        if not apply:
            return "pending", text, ""
        position = first + index
        ending = "\r" if lines[position].endswith("\r") else ""
        lines = lines[:position] + [hook["new"] + ending] + lines[position + 1:]
        return "changed", "\n".join(lines), ""
    if op == "delete":
        matches = [index for index, line in enumerate(region) if same_line(line, hook["old"])]
        if len(matches) < occurrence:
            return "applied", text, ""
        if len(matches) > occurrence:
            return "missing", text, (
                f"the old line is in the scope {len(matches)} times, so deleting occurrence {occurrence} would delete "
                "another line on every run; a delete must target the last matching line of its scope"
            )
        if not apply:
            return "pending", text, ""
        position = first + matches[occurrence - 1]
        return "changed", "\n".join(lines[:position] + lines[position + 1:]), ""
    index = locate(region, hook["anchor"], occurrence)
    if index is None:
        return "missing", text, "neither the anchor nor the new line was found"
    if not apply:
        return "pending", text, ""
    position = first + index
    ending = "\r" if lines[position].endswith("\r") else ""
    return "changed", "\n".join(lines[:position + 1] + [hook["new"] + ending] + lines[position + 1:]), ""


def hook_targets(hook, rel_paths):
    if is_glob(hook["file"]):
        return [rel for rel in rel_paths if not excluded_from_hooks(rel) and fnmatch.fnmatchcase(rel, hook["file"])]
    return [hook["file"]]


def application_order(hooks):
    first_seen = {}
    keyed = []
    for hook in hooks:
        group = (hook["file"], hook.get("scope"), hook.get("scope_occurrence", 1), hook["op"],
                 hook.get("old") or hook.get("anchor") or hook.get("new"))
        first_seen.setdefault(group, hook["_order"])
        keyed.append(((0 if is_glob(hook["file"]) else 1, first_seen[group], -hook.get("occurrence", 1)), hook))
    return [hook for _, hook in sorted(keyed, key=lambda item: item[0])]


def apply_hooks(contents, hooks, apply=True, only=None):
    rel_paths = sorted(contents)
    results = []
    for hook in application_order(hooks):
        siblings = sibling_news(hook, hooks)
        targets = hook_targets(hook, rel_paths)
        if only is not None:
            targets = [rel for rel in targets if rel in only]
            if not targets:
                continue
        states = {}
        detail = ""
        glob = is_glob(hook["file"])
        for rel in targets:
            if rel not in contents:
                states[rel] = "missing"
                detail = "file not found"
                continue
            if glob and not any(value in contents[rel] for value in (hook.get("old"), hook.get("anchor"), hook.get("new")) if value):
                continue
            state, text, reason = run_hook(contents[rel], hook, siblings, apply)
            if glob and state == "missing":
                continue
            states[rel] = state
            if state == "changed":
                contents[rel] = text
            if reason:
                detail = reason
        if not states and glob and only is not None:
            continue
        if not states and glob and hook["op"] == "delete":
            state = "applied"
        elif not states:
            state = "missing"
            detail = "no file contains the old or the new line"
        elif "missing" in states.values():
            state = "missing"
        elif "pending" in states.values():
            state = "pending"
        elif "changed" in states.values():
            state = "changed"
        else:
            state = "applied"
        changed = sorted(rel for rel, value in states.items() if value == "changed")
        pending = sorted(rel for rel, value in states.items() if value == "pending")
        results.append({"hook": hook, "state": state, "detail": detail, "files": sorted(states),
                        "changed": changed, "pending": pending})
    results.sort(key=lambda result: result["hook"]["_order"])
    return results


def summarize_hooks(results):
    counts = {"applied": 0, "changed": 0, "pending": 0, "missing": 0}
    for result in results:
        counts[result["state"]] += 1
    return counts


def report_hooks(results, problems=(), warnings=(), verbose=False, pending_label="pending"):
    counts = summarize_hooks(results)
    applied = counts["applied"] + counts["changed"]
    line = f"hooks: {applied} applied, {counts['missing']} missing"
    extras = []
    if counts["pending"]:
        extras.append(f"{counts['pending']} {pending_label}")
    if problems:
        extras.append(f"{len(problems)} invalid")
    if extras:
        line += ", " + ", ".join(extras)
    if counts["changed"]:
        files = len({rel for result in results for rel in result["changed"]})
        line += f" ({counts['changed']} newly applied in {files} files)"
    print(line)
    for result in results:
        hook = result["hook"]
        if result["state"] == "missing":
            print(f"  MISSING {hook_label(hook)}: {result['detail']}")
        elif result["state"] == "pending":
            where = ", ".join(result["pending"][:3]) + (" ..." if len(result["pending"]) > 3 else "")
            print(f"  {pending_label.upper()} {hook_label(hook)} in {where}")
        elif verbose:
            files = f" in {len(result['files'])} files" if is_glob(hook["file"]) else ""
            print(f"  {result['state']:<8} {hook_label(hook)}{files}")
    for problem in problems:
        print(f"  INVALID {problem}")
    for warning in warnings:
        print(f"  warning: {warning}")
    return counts


def hook_relative(path):
    try:
        return Path(path).resolve().relative_to(KOTLIN_ROOT.resolve()).as_posix()
    except ValueError:
        return None


_HOOK_CACHE = {}


def cached_hooks():
    if "hooks" not in _HOOK_CACHE:
        _HOOK_CACHE["hooks"] = load_hooks()
    return _HOOK_CACHE["hooks"]


def apply_hooks_to_file(path, text):
    rel = hook_relative(path)
    if rel is None or excluded_from_hooks(rel):
        return text
    hooks, _, _ = cached_hooks()
    contents = {rel: text}
    for result in apply_hooks(contents, hooks, apply=True, only={rel}):
        if result["state"] == "missing":
            print(f"  hook MISSING {hook_label(result['hook'])}: {result['detail']}")
    return contents[rel]


def kotlin_files():
    for directory in SOURCE_DIRS:
        if directory.exists():
            yield from sorted(directory.rglob("*.kt"))


def run(dry_run, only=None, hooks=True, verbose=False):
    totals = {}
    renamed = set()
    changed_files = 0
    files = list(kotlin_files())
    sources = {path: path.read_text(encoding="utf-8", errors="replace") for path in files}
    selected = files
    if only is not None:
        wanted = {Path(path).resolve() for path in only}
        selected = [path for path in files if path.resolve() in wanted]
    selected_set = set(selected)
    contents = {path: (sources[path], sources[path]) for path in files}
    for path in selected:
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
        contents[path] = (original, text)
    summary = {"files": len(selected), "changed": 0, "hooks": None}
    if hooks:
        hook_list, problems, warnings = load_hooks()
        by_rel = {}
        for path in files:
            rel = hook_relative(path)
            if rel is not None:
                by_rel[rel] = path
        hook_texts = {rel: contents[path][1] for rel, path in by_rel.items()}
        only_rel = None if only is None else {rel for rel, path in by_rel.items() if path in selected_set}
        results = apply_hooks(hook_texts, hook_list, apply=True, only=only_rel)
        for rel, text in hook_texts.items():
            path = by_rel[rel]
            contents[path] = (contents[path][0], text)
        summary["hooks"] = (results, problems, warnings)
    for path in files:
        original, text = contents[path]
        if text != original:
            changed_files += 1
            if not dry_run:
                path.write_text(text, encoding="utf-8", newline="\n")
    summary["changed"] = changed_files
    print(f"files: {len(selected)}  changed: {changed_files}")
    print(f"imports added: getValue {totals.get('getValue', 0)}, setValue {totals.get('setValue', 0)}, localized {totals.get('localized', 0)}")
    print(f"flows exposed as mutable: {totals.get('flows', 0)}  files with backing references renamed: {renames}")
    print(f"composables given LocalModel/LocalOnNavigate defaults: {totals.get('defaults', 0)}")
    print(f"context TODOs resolved: {totals.get('contexts', 0)}  delegate adapters generated: {totals.get('adapters', 0)}")
    print(f"typed TODOs defaulted: {totals.get('typed', 0)}  statement TODOs turned into no-ops: {totals.get('statements', 0)}")
    print(f"host hooks: {totals.get('hooks', 0)}  modifier chains reordered: {totals.get('modifiers', 0)}")
    if summary["hooks"] is not None:
        results, problems, warnings = summary["hooks"]
        summary["hook_counts"] = report_hooks(results, problems, warnings, verbose)
        summary["hook_problems"] = len(problems)
    return summary


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Deterministic fixes applied to every generated Kotlin file.")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--no-hooks", action="store_true", help="skip the one-line hooks in tools/hooks/*.json")
    parser.add_argument("--only", nargs="+", type=Path, default=None, help="process only these Kotlin files")
    parser.add_argument("--verbose", action="store_true", help="list every hook, not only the missing ones")
    args = parser.parse_args()
    only = None
    if args.only is not None:
        only = [path if path.exists() or not (ROOT / path).exists() else ROOT / path for path in args.only]
    run(args.dry_run, only=only, hooks=not args.no_hooks, verbose=args.verbose)


if __name__ == "__main__":
    main()
