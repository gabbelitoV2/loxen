#!/usr/bin/env python3
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
PROMPTS = HERE / "prompts"
ALL_TIERS = ["logic", "platform", "test", "media", "ui", "apple_only"]
DEFAULT_TIERS = ["logic", "platform", "test"]
PROVIDERS = {
    "anthropic": {"base_url": None, "model": "claude-opus-5", "key_env": "ANTHROPIC_API_KEY"},
    "deepseek": {
        "base_url": "https://api.deepseek.com/anthropic",
        "model": "deepseek-flash",
        "key_env": "DEEPSEEK_API_KEY",
    },
}
PRICES = {
    "claude-opus-5": (5.0, 25.0),
    "claude-sonnet-5": (2.0, 10.0),
    "claude-haiku-4-5": (1.0, 5.0),
    "deepseek-flash": (0.15, 0.60),
    "deepseek-v4-pro": (0.66, 1.98),
}


class PortError(Exception):
    pass


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def estimate_tokens(text):
    return len(text) // 3 + 1


class ApiBackend:
    name = "api"

    def __init__(self, model, effort, provider):
        try:
            import anthropic
        except ImportError:
            sys.exit("The anthropic package is missing. Run: pip install anthropic")
        options = {}
        if provider["base_url"]:
            options["base_url"] = provider["base_url"]
        api_key = os.environ.get(provider["key_env"])
        if api_key:
            options["api_key"] = api_key
        self.client = anthropic.Anthropic(**options)
        self.model = model
        self.effort = effort
        self.fallbacks = provider["base_url"] is None

    def complete(self, system, user):
        kwargs = {
            "model": self.model,
            "max_tokens": 100000,
            "system": [{"type": "text", "text": system, "cache_control": {"type": "ephemeral"}}],
            "messages": [{"role": "user", "content": user}],
            "output_config": {"effort": self.effort},
        }
        if self.fallbacks:
            stream = self.client.beta.messages.stream(
                betas=["server-side-fallback-2026-07-01"], fallbacks="default", **kwargs
            )
        else:
            stream = self.client.messages.stream(**kwargs)
        with stream as s:
            message = s.get_final_message()
        if message.stop_reason == "refusal":
            raise PortError("the model refused to translate the file")
        if message.stop_reason == "max_tokens":
            raise PortError("the response was cut off at max_tokens")
        text = "".join(block.text for block in message.content if block.type == "text")
        usage = message.usage
        tokens_in = (
            usage.input_tokens
            + (usage.cache_read_input_tokens or 0)
            + (usage.cache_creation_input_tokens or 0)
        )
        return text, tokens_in, usage.output_tokens


class CliBackend:
    name = "cli"

    def __init__(self, model, effort):
        self.exe = shutil.which("claude.exe") or shutil.which("claude")
        if not self.exe:
            sys.exit("claude was not found in PATH. Install Claude Code or use --backend api.")
        self.model = model
        self.effort = effort

    def complete(self, system, user):
        prompt = "<instructions>\n" + system + "\n</instructions>\n\n" + user
        command = [
            self.exe, "-p", "--no-session-persistence", "--output-format", "text", "--tools", "",
            "--model", self.model, "--effort", self.effort,
        ]
        env = {key: value for key, value in os.environ.items() if key != "CLAUDECODE"}
        result = subprocess.run(
            command, input=prompt, capture_output=True, text=True, encoding="utf-8", errors="replace",
            env=env, timeout=3600,
        )
        if result.returncode != 0:
            detail = (result.stderr or result.stdout).strip()[-600:]
            raise PortError(detail or f"claude exited with code {result.returncode}")
        return result.stdout, estimate_tokens(prompt), estimate_tokens(result.stdout)


def load_json(path, default):
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def save_json(path, data):
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8", newline="\n")
    tmp.replace(path)


def load_prompts():
    system = (PROMPTS / "system.md").read_text(encoding="utf-8")
    tiers = {tier: (PROMPTS / f"{tier}.md").read_text(encoding="utf-8") for tier in ALL_TIERS}
    return system, tiers


def select_entries(inventory, args):
    tiers = ALL_TIERS if "all" in args.tier else args.tier
    entries = [e for e in inventory["files"] if e["tier"] in tiers]
    if args.include:
        entries = [e for e in entries if e["path"].startswith(tuple(args.include))]
    entries.sort(key=lambda e: (e["wave"], e["path"]))
    return entries


def needs_port(entry, state, force):
    previous = state.get(entry["path"])
    if force or previous is None:
        return True
    return previous.get("status") != "ok" or previous.get("sha256") != entry["sha256"]


def glossary_for(entry, by_path):
    rows = []
    for dep in entry["deps"]:
        other = by_path.get(dep)
        if other is None or other["tier"] == "skip":
            continue
        for name in other["declared_types"]:
            rows.append(f"- {name} -> {other['kotlin_package']}")
        for name in other["declared_functions"]:
            rows.append(f"- fun {name}() -> {other['kotlin_package']}")
        for name in other["declared_globals"]:
            rows.append(f"- val {name} -> {other['kotlin_package']}")
    return rows


DECLARATION_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|internal|open|abstract|override|suspend|inline|operator|infix|const|lateinit)\s+)*"
    r"(?:fun|class|data class|sealed class|enum class|object|interface|val|var|typealias)\s"
)


def signatures(path, limit=80):
    if not path.exists():
        return []
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    result = []
    index = 0
    while index < len(lines) and len(result) < limit:
        line = lines[index]
        index += 1
        if not DECLARATION_RE.match(line) or line.lstrip().startswith("private "):
            continue
        signature = line.strip()
        while signature.count("(") > signature.count(")") and index < len(lines):
            signature += " " + lines[index].strip()
            index += 1
        signature = re.sub(r"\s*\{.*$", "", signature)
        result.append(signature)
    return result


def dependency_signatures(entry, by_path, out_dir):
    sections = []
    for dep in entry.get("deps", []):
        other = by_path.get(dep)
        if other is None or other["tier"] == "skip":
            continue
        lines = signatures(out_dir / other["kotlin_path"])
        if lines:
            sections.append(f"// {other['kotlin_package']} ({Path(other['kotlin_path']).name})\n" + "\n".join(lines))
    return "\n\n".join(sections)[:60000]


def build_user_prompt(entry, glossary, source, dependencies=""):
    parts = [
        f"Swift file: {entry['path']}",
        f"Tier: {entry['tier']}",
        f"Kotlin package: {entry['kotlin_package']}",
        f"Kotlin file: {entry['kotlin_path']}",
    ]
    if glossary:
        parts.append(
            "Types, top-level functions and globals from other files, with the Kotlin package they live in. "
            "Import them from there when used:\n" + "\n".join(glossary)
        )
    if dependencies:
        parts.append(
            "Current Kotlin declarations in the files this file depends on. Call them exactly as declared:\n"
            + dependencies
        )
    parts.append("```swift\n" + source + "\n```")
    return "\n\n".join(parts)


def fenced_blocks(text):
    blocks = []
    language = None
    buffer = []
    for line in text.split("\n"):
        stripped = line.strip()
        if language is None:
            if stripped.startswith("```"):
                language = stripped[3:].strip().lower()
                buffer = []
        elif stripped == "```":
            blocks.append((language, "\n".join(buffer)))
            language = None
        else:
            buffer.append(line)
    if language is not None and buffer:
        blocks.append((language, "\n".join(buffer)))
    return blocks


def parse_response(text):
    blocks = fenced_blocks(text)
    kotlin = next((body for language, body in blocks if language in ("kotlin", "kt")), None)
    if kotlin is None:
        raise PortError("the response had no kotlin block")
    if not kotlin.lstrip().startswith("package "):
        raise PortError("the kotlin block does not start with a package line")
    meta = {}
    json_body = next((body for language, body in blocks if language == "json"), None)
    if json_body:
        try:
            meta = json.loads(json_body)
        except json.JSONDecodeError:
            meta = {}
    return meta, kotlin


def port_one(backend, entry, root, out_dir, by_path, system, tiers):
    source = (root / entry["path"]).read_text(encoding="utf-8", errors="replace")
    prompt_system = system + "\n\n" + tiers[entry["tier"]]
    prompt_user = build_user_prompt(
        entry, glossary_for(entry, by_path), source, dependency_signatures(entry, by_path, out_dir)
    )
    started = time.time()
    text, tokens_in, tokens_out = backend.complete(prompt_system, prompt_user)
    meta, kotlin = parse_response(text)
    target = out_dir / entry["kotlin_path"]
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(kotlin.rstrip() + "\n", encoding="utf-8", newline="\n")
    return {
        "status": "ok",
        "sha256": entry["sha256"],
        "tier": entry["tier"],
        "kotlin_path": entry["kotlin_path"],
        "notes": list(meta.get("notes", []))[:8],
        "unsupported": list(meta.get("unsupported", [])),
        "model": backend.model,
        "backend": backend.name,
        "tokens_in": tokens_in,
        "tokens_out": tokens_out,
        "seconds": round(time.time() - started, 1),
        "ported_at": now_iso(),
    }


def dry_run(todo, root, args, system, tiers):
    per_tier = {}
    tokens_in = 0
    tokens_out = 0
    for entry in todo:
        source = (root / entry["path"]).read_text(encoding="utf-8", errors="replace")
        source_tokens = estimate_tokens(source)
        tokens_in += estimate_tokens(system + tiers[entry["tier"]]) + source_tokens
        tokens_out += int(source_tokens * 1.1) + 200
        row = per_tier.setdefault(entry["tier"], [0, 0])
        row[0] += 1
        row[1] += entry["lines"]
    print(f"{'tier':<12}{'files':>8}{'lines':>10}")
    for tier in ALL_TIERS:
        if tier in per_tier:
            print(f"{tier:<12}{per_tier[tier][0]:>8}{per_tier[tier][1]:>10}")
    print(f"{'total':<12}{len(todo):>8}{sum(r[1] for r in per_tier.values()):>10}")
    print(f"\nestimated tokens in: {tokens_in:,}  out: {tokens_out:,}")
    price = PRICES.get(args.model)
    if price:
        cost = tokens_in / 1e6 * price[0] + tokens_out / 1e6 * price[1]
        if args.provider == "deepseek":
            print(f"estimated cost with {args.model}: ${cost:,.2f} off-peak, ${cost * 2:,.2f} during peak hours "
                  "(01:00-04:00 and 06:00-10:00 UTC on weekdays)")
        else:
            print(f"estimated API cost with {args.model}: ${cost:,.0f} (free with the cli backend)")
    if len(todo) <= 60:
        print()
        for entry in todo:
            print(f"  wave {entry['wave']:>3}  {entry['tier']:<10} {entry['path']} -> {entry['kotlin_path']}")


def write_report(out_dir, inventory, state):
    lines = ["# Port report", "", f"Generated {now_iso()}", "", "## Summary", ""]
    statuses = {}
    for entry in inventory["files"]:
        saved = state.get(entry["path"])
        if entry["tier"] == "skip":
            status = "skipped"
        elif saved is None:
            status = "pending"
        elif saved.get("status") != "ok":
            status = "error"
        elif saved.get("sha256") != entry["sha256"]:
            status = "stale"
        else:
            status = "done"
        statuses.setdefault(entry["tier"], {}).setdefault(status, 0)
        statuses[entry["tier"]][status] += 1
    lines.append("| tier | done | error | stale | pending | skipped |")
    lines.append("|---|---|---|---|---|---|")
    for tier in ALL_TIERS + ["skip"]:
        row = statuses.get(tier)
        if row:
            lines.append(
                f"| {tier} | {row.get('done', 0)} | {row.get('error', 0)} | {row.get('stale', 0)} | "
                f"{row.get('pending', 0)} | {row.get('skipped', 0)} |"
            )
    manual = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "ok" and s.get("unsupported")]
    if manual:
        lines += ["", "## Needs manual work", ""]
        for path, saved in manual:
            lines.append(f"- {path}")
            for item in saved["unsupported"]:
                lines.append(f"  - {item}")
    errors = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "error"]
    if errors:
        lines += ["", "## Errors", ""]
        for path, saved in errors:
            lines.append(f"- {path}: {saved.get('error', '')}")
    done = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "ok"]
    if done:
        lines += ["", "## Ported files", "", "| Swift | Kotlin | model | seconds |", "|---|---|---|---|"]
        for path, saved in done:
            lines.append(f"| {path} | {saved['kotlin_path']} | {saved.get('model', '')} | {saved.get('seconds', '')} |")
    (out_dir / "PORT-REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def load_dotenv(path):
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip().removeprefix("export ")
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip("\"'"))


def pick_backend(args):
    provider = PROVIDERS[args.provider]
    backend = args.backend
    if backend == "auto":
        backend = "api" if os.environ.get(provider["key_env"]) or args.provider != "anthropic" else "cli"
    if backend == "cli" and args.provider != "anthropic":
        sys.exit(f"{args.provider} only works with --backend api")
    if backend == "api":
        if args.provider != "anthropic" and not os.environ.get(provider["key_env"]):
            sys.exit(f"Set the {provider['key_env']} environment variable to your {args.provider} API key")
        return ApiBackend(args.model, args.effort, provider)
    return CliBackend(args.model, args.effort)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    load_dotenv(HERE.parent / ".env")
    parser = argparse.ArgumentParser(description="Translate Moblin's Swift files to Kotlin with an LLM.")
    parser.add_argument("--inventory", type=Path, default=HERE / "inventory.json")
    parser.add_argument("--moblin", type=Path, default=None)
    parser.add_argument("--out", type=Path, default=HERE.parent)
    parser.add_argument("--tier", nargs="+", default=DEFAULT_TIERS, choices=ALL_TIERS + ["all"])
    parser.add_argument("--include", nargs="+", default=[])
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--backend", choices=["auto", "api", "cli"], default="auto")
    parser.add_argument("--provider", choices=sorted(PROVIDERS), default="anthropic")
    parser.add_argument("--model", default=None)
    parser.add_argument("--effort", default="high")
    parser.add_argument("--workers", type=int, default=2)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--report-only", action="store_true")
    args = parser.parse_args()
    if args.model is None:
        args.model = PROVIDERS[args.provider]["model"]

    if not args.inventory.exists():
        sys.exit(f"{args.inventory} is missing. Run python tools/inventory.py first.")
    inventory = load_json(args.inventory, None)
    root = (args.moblin or Path(inventory["root"])).resolve()
    out_dir = args.out.resolve()
    state_path = HERE / "port-state.json"
    state = load_json(state_path, {})
    by_path = {e["path"]: e for e in inventory["files"]}

    if args.report_only:
        write_report(out_dir, inventory, state)
        print(f"wrote {out_dir / 'PORT-REPORT.md'}")
        return

    system, tiers = load_prompts()
    entries = select_entries(inventory, args)
    todo = [e for e in entries if needs_port(e, state, args.force)]
    print(f"{len(entries)} files selected, {len(entries) - len(todo)} already done, {len(todo)} to port")
    if args.limit:
        todo = todo[: args.limit]
        print(f"limited to {len(todo)}")
    if not todo:
        write_report(out_dir, inventory, state)
        return
    if args.dry_run:
        dry_run(todo, root, args, system, tiers)
        return

    backend = pick_backend(args)
    print(f"backend {backend.name} ({args.provider}), model {args.model}, effort {args.effort}, {args.workers} workers")
    lock = threading.Lock()
    finished = 0

    def work(entry):
        try:
            return entry, port_one(backend, entry, root, out_dir, by_path, system, tiers), None
        except Exception as exc:
            return entry, None, str(exc)

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(work, entry) for entry in todo]
        try:
            for future in as_completed(futures):
                entry, result, error = future.result()
                with lock:
                    finished += 1
                    if error:
                        state[entry["path"]] = {
                            "status": "error",
                            "error": error,
                            "sha256": entry["sha256"],
                            "tier": entry["tier"],
                            "kotlin_path": entry["kotlin_path"],
                            "ported_at": now_iso(),
                        }
                        print(f"[{finished}/{len(todo)}] FAIL {entry['path']}: {error}")
                    else:
                        state[entry["path"]] = result
                        print(
                            f"[{finished}/{len(todo)}] ok   {entry['path']} -> {entry['kotlin_path']} "
                            f"({result['seconds']}s, {len(result['unsupported'])} TODO)"
                        )
                    save_json(state_path, state)
        except KeyboardInterrupt:
            print("\ninterrupted, saving state for the files that finished")
            pool.shutdown(wait=False, cancel_futures=True)
            save_json(state_path, state)
            write_report(out_dir, inventory, state)
            raise SystemExit(130)

    import postprocess
    postprocess.run(False)
    write_report(out_dir, inventory, state)
    ok = sum(1 for e in todo if state.get(e["path"], {}).get("status") == "ok")
    print(f"\ndone: {ok} ok, {len(todo) - ok} failed. Report: {out_dir / 'PORT-REPORT.md'}")


if __name__ == "__main__":
    main()
