#!/usr/bin/env python3
import argparse
import json
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

LIMIT_PREFIXES = (
    "You've hit your",
    "You've reached your",
    "You're out of usage credits",
    "You're out of extra usage",
    "Your org is out of usage",
    "Your seat type doesn't include usage",
    "Your seat type doesn't include extra usage",
    "Your usage allocation has been disabled by your admin",
    "Your group's usage limit is set to $0",
    "Fable 5 requires usage credits",
    "Claude AI usage limit reached",
)
LIMIT_PATTERNS = (
    re.compile(r"\bClaude AI usage limit reached\b", re.I),
    re.compile(r"\b(?:usage|session|weekly|opus|sonnet|\d+-hour) limit reached\b", re.I),
    re.compile(r"\byou've (?:hit|reached) your\b[^\n]*?\blimit\b", re.I),
)
EPOCH_RE = re.compile(r"Claude AI usage limit reached\|(\d{9,13})")
RESET_RE = re.compile(
    r"\bresets?\s+(?:at\s+|on\s+)?(?P<when>[^\n·∙|()]+?)\s*(?:\((?P<zone>[^)\n]+)\))?\s*(?:$|[\n·∙|]|\.(?:\s|$))",
    re.I,
)
TIME_RE = re.compile(
    r"^(?:(?P<month>[A-Za-z]{3,9})\.?\s+(?P<day>\d{1,2})(?:st|nd|rd|th)?,?\s+(?:at\s+)?)?"
    r"(?P<hour>\d{1,2})(?::(?P<minute>\d{2}))?\s*(?P<ampm>am|pm)?$",
    re.I,
)
RELATIVE_RE = re.compile(
    r"^in\s+(?:(?P<hours>\d+)\s*h(?:ours?|rs?)?)?\s*(?:(?P<minutes>\d+)\s*m(?:inutes?|ins?)?)?$", re.I
)
MONTHS = ("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")


def zone_named(name):
    if not name or name.strip().upper() in ("UTC", "GMT", "Z"):
        return timezone.utc
    try:
        from zoneinfo import ZoneInfo

        return ZoneInfo(name.strip())
    except Exception:
        return timezone.utc


def parse_reset(text, now):
    epoch = EPOCH_RE.search(text)
    if epoch:
        value = int(epoch.group(1))
        return datetime.fromtimestamp(value / 1000 if value > 10**11 else value, timezone.utc)
    match = RESET_RE.search(text)
    if not match:
        return None
    when = match.group("when").strip().rstrip(",")
    relative = RELATIVE_RE.match(when)
    if relative and (relative.group("hours") or relative.group("minutes")):
        return now + timedelta(hours=int(relative.group("hours") or 0), minutes=int(relative.group("minutes") or 0))
    parsed = TIME_RE.match(when)
    if not parsed:
        return None
    hour = int(parsed.group("hour"))
    minute = int(parsed.group("minute") or 0)
    ampm = (parsed.group("ampm") or "").lower()
    if ampm:
        if not 1 <= hour <= 12:
            return None
        hour = hour % 12 + (12 if ampm == "pm" else 0)
    if hour > 23 or minute > 59:
        return None
    local_now = now.astimezone(zone_named(match.group("zone")))
    try:
        if parsed.group("month"):
            month_name = parsed.group("month")[:3].lower()
            if month_name not in MONTHS:
                return None
            candidate = local_now.replace(month=MONTHS.index(month_name) + 1, day=int(parsed.group("day")),
                                          hour=hour, minute=minute, second=0, microsecond=0)
            if candidate < local_now - timedelta(days=1):
                candidate = candidate.replace(year=candidate.year + 1)
        else:
            candidate = local_now.replace(hour=hour, minute=minute, second=0, microsecond=0)
            if candidate <= local_now:
                candidate += timedelta(days=1)
    except ValueError:
        return None
    return candidate.astimezone(timezone.utc)


def is_limit_text(text):
    stripped = (text or "").strip()
    return stripped.startswith(LIMIT_PREFIXES) or any(pattern.search(stripped) for pattern in LIMIT_PATTERNS)


def message_texts(message):
    content = (message.get("message") or {}).get("content") or []
    if isinstance(content, str):
        return [content]
    return [block.get("text", "") for block in content if isinstance(block, dict) and block.get("type") == "text"]


def load_messages(path):
    try:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return []
    return [message for message in data if isinstance(message, dict)] if isinstance(data, list) else []


def detect(messages, step_failed, now=None):
    now = now or datetime.now(timezone.utc)
    evidence = []
    resets_at = None
    texts = []
    result = None
    for message in messages:
        kind = message.get("type")
        if kind == "rate_limit_event":
            info = message.get("rate_limit_info") or {}
            if info.get("status") == "rejected" and info.get("overageStatus") not in ("allowed", "allowed_warning"):
                evidence.append(f"rate_limit_event rejected ({info.get('rateLimitType', 'unknown')})")
                if info.get("resetsAt"):
                    resets_at = datetime.fromtimestamp(float(info["resetsAt"]), timezone.utc)
        elif kind == "assistant" and message.get("error"):
            if message["error"] == "rate_limit":
                evidence.append("assistant error rate_limit")
            texts += message_texts(message)
        elif kind == "result":
            result = message
    failed = step_failed or result is None or bool(result.get("is_error")) or result.get("subtype") != "success"
    if result is not None and failed:
        texts.append(str(result.get("result") or ""))
        texts += [str(error) for error in result.get("errors") or []]
    for text in texts:
        if is_limit_text(text):
            evidence.append("message: " + text.strip().splitlines()[0][:200])
            if resets_at is None:
                resets_at = parse_reset(text, now)
    limited = failed and bool(evidence)
    evidence = list(dict.fromkeys(evidence)) if limited else []
    return {"limited": limited, "resets_at": resets_at if limited else None, "evidence": evidence}


def summary(messages):
    for message in reversed(messages):
        if message.get("type") == "result" and message.get("result"):
            return str(message["result"])
    for message in reversed(messages):
        if message.get("type") == "assistant":
            texts = [text for text in message_texts(message) if text.strip()]
            if texts:
                return texts[-1]
    return ""


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Tell from a claude-code-action execution file whether Claude stopped at a usage limit, and when "
        "the limit resets."
    )
    parser.add_argument("execution_file", type=Path)
    parser.add_argument("--step-failed", action="store_true", help="the Claude step itself failed")
    args = parser.parse_args()
    result = detect(load_messages(args.execution_file), args.step_failed)
    resets_at = result["resets_at"].isoformat() if result["resets_at"] else None
    print(json.dumps({"limited": result["limited"], "resets_at": resets_at, "evidence": result["evidence"]}, indent=2,
                     ensure_ascii=False))


if __name__ == "__main__":
    main()
