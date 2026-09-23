import argparse
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.moblin.android"
DANGER = re.compile(
    r"^(delete|remove|reset|create|duplicate|import|export|log ?out|sign ?out|clear|save|go live|end|"
    r"start|stop|connect|disconnect|record|pair|scan|donate|buy|purchase|restore|upgrade|rate|share|"
    r"copy|paste|add|new|login|log in|sign in|authorize|test|send|restart|reboot|shutdown|power|"
    r"factory|erase|wipe|cancel|done|ok|yes|no|close|settings|back)\b",
    re.IGNORECASE,
)
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


class Device:
    def __init__(self, adb, serial):
        self.adb = adb
        self.serial = serial

    def run(self, *args, timeout=30):
        return subprocess.run(
            [self.adb, "-s", self.serial, *args],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
        ).stdout

    def tap(self, x, y):
        self.run("shell", "input", "tap", str(x), str(y))

    def swipe(self, x1, y1, x2, y2, ms=400):
        self.run("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))

    def key(self, code):
        self.run("shell", "input", "keyevent", str(code))

    def pid(self):
        return self.run("shell", "pidof", PACKAGE).strip()

    def dump(self):
        for _ in range(3):
            out = self.run("exec-out", "uiautomator", "dump", "/dev/tty", timeout=40)
            start = out.find("<?xml")
            end = out.rfind("</hierarchy>")
            if start >= 0 and end > start:
                return ET.fromstring(out[start : end + len("</hierarchy>")])
            time.sleep(1)
        return None

    def crash_log(self):
        return self.run("logcat", "-d", "-b", "crash")

    def clear_crash_log(self):
        self.run("logcat", "-c", "-b", "crash")

    def launch(self):
        self.run("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")


def nodes(root):
    result = []
    for node in root.iter("node"):
        match = BOUNDS.match(node.get("bounds", ""))
        if not match:
            continue
        x1, y1, x2, y2 = map(int, match.groups())
        result.append(
            {
                "text": (node.get("text") or node.get("content-desc") or "").strip(),
                "clickable": node.get("clickable") == "true",
                "checkable": node.get("checkable") == "true",
                "package": node.get("package"),
                "bounds": (x1, y1, x2, y2),
                "node": node,
            }
        )
    return result


def label_of(node):
    texts = []
    for child in node["node"].iter("node"):
        text = (child.get("text") or child.get("content-desc") or "").strip()
        if text:
            texts.append(text)
    return " | ".join(texts[:3])


class Crawler:
    def __init__(self, device, panel_left, panel_right, top, bottom, max_depth, out):
        self.device = device
        self.panel_left = panel_left
        self.panel_right = panel_right
        self.top = top
        self.bottom = bottom
        self.max_depth = max_depth
        self.out = out
        self.visited = set()
        self.crashes = []
        self.screens = 0
        self.pid = device.pid()

    def log(self, message):
        print(message, flush=True)
        with open(self.out.with_suffix(".log"), "a", encoding="utf-8") as file:
            file.write(message + "\n")

    def title(self, root):
        candidates = [
            n
            for n in nodes(root)
            if n["text"]
            and n["bounds"][1] < self.top
            and self.panel_left < (n["bounds"][0] + n["bounds"][2]) // 2 < self.panel_right
        ]
        centre = (self.panel_left + self.panel_right) // 2
        candidates.sort(key=lambda n: abs((n["bounds"][0] + n["bounds"][2]) // 2 - centre))
        return candidates[0]["text"] if candidates else ""

    def in_app(self, root):
        return root is not None and any(n["package"] == PACKAGE for n in nodes(root))

    def alive(self):
        pid = self.device.pid()
        if pid and pid == self.pid:
            return True
        return False

    def targets(self, root):
        result = []
        for n in nodes(root):
            if not n["clickable"] or n["checkable"]:
                continue
            x1, y1, x2, y2 = n["bounds"]
            if x1 < self.panel_left or x2 > self.panel_right + 5 or y1 < self.top or y2 > self.bottom:
                continue
            label = label_of(n)
            if not label or DANGER.search(label.split(" | ")[0]):
                continue
            result.append((label, ((x1 + x2) // 2, (y1 + y2) // 2)))
        return result

    def back(self, expected_title):
        root = self.device.dump()
        if root is None:
            return
        for n in nodes(root):
            x1, y1, x2, y2 = n["bounds"]
            if n["clickable"] and y2 < self.top and x1 < self.panel_left + 250 and x1 >= self.panel_left - 5:
                self.device.tap((x1 + x2) // 2, (y1 + y2) // 2)
                time.sleep(1.0)
                return
        self.device.key(4)
        time.sleep(1.0)

    def recover(self, path):
        self.device.launch()
        time.sleep(6)
        self.pid = self.device.pid()
        self.open_settings()
        for label in path:
            root = self.device.dump()
            if root is None:
                return False
            found = False
            for _ in range(8):
                for target_label, (x, y) in self.targets(root):
                    if target_label == label:
                        self.device.tap(x, y)
                        time.sleep(1.5)
                        found = True
                        break
                if found:
                    break
                self.scroll_down()
                root = self.device.dump()
                if root is None:
                    return False
            if not found:
                return False
        return True

    def open_settings(self):
        self.device.tap(1880, 95)
        time.sleep(2)

    def scroll_down(self):
        x = (self.panel_left + self.panel_right) // 2
        self.device.swipe(x, self.bottom - 60, x, self.top + 120, 500)
        time.sleep(1.0)

    def scroll_to_top(self):
        x = (self.panel_left + self.panel_right) // 2
        for _ in range(6):
            self.device.swipe(x, self.top + 120, x, self.bottom - 40, 250)
        time.sleep(0.8)

    def record_crash(self, path, label):
        log = self.device.crash_log()
        lines = log.splitlines()
        fatal = [i for i, line in enumerate(lines) if "FATAL EXCEPTION" in line]
        stack = "\n".join(lines[fatal[-1] : fatal[-1] + 30]) if fatal else log[-3000:]
        self.crashes.append({"path": path + [label], "stack": stack})
        self.log(f"CRASH at {' > '.join(path + [label])}")
        self.log(stack)
        self.device.clear_crash_log()

    def crawl(self, path, depth):
        root = self.device.dump()
        if root is None:
            self.log(f"dump failed at {' > '.join(path)}")
            return
        title = self.title(root)
        key = " > ".join(path)
        if key in self.visited:
            return
        self.visited.add(key)
        self.screens += 1
        self.log(f"[{self.screens}] {key or 'Settings'} (title {title!r})")
        if depth >= self.max_depth:
            return
        done = set()
        stale_rounds = 0
        while stale_rounds < 2:
            root = self.device.dump()
            if root is None:
                break
            pending = [(label, point) for label, point in self.targets(root) if label not in done]
            if not pending:
                stale_rounds += 1
                self.scroll_down()
                continue
            stale_rounds = 0
            label, (x, y) = pending[0]
            done.add(label)
            self.device.tap(x, y)
            time.sleep(1.5)
            if not self.alive():
                self.record_crash(path, label)
                if not self.recover(path):
                    self.log(f"could not return to {key}")
                    return
                continue
            after = self.device.dump()
            if not self.in_app(after):
                self.log(f"left the app at {key} > {label}")
                self.device.key(4)
                time.sleep(1.5)
                after = self.device.dump()
                if not self.in_app(after):
                    self.recover(path)
                continue
            new_title = self.title(after)
            if new_title and new_title != title:
                self.crawl(path + [label], depth + 1)
                self.back(title)
                check = self.device.dump()
                if check is not None and self.title(check) != title:
                    self.log(f"back did not return to {title!r}, recovering")
                    if not self.recover(path):
                        return
            else:
                if after is not None and len(nodes(after)) != len(nodes(root)):
                    self.device.key(4)
                    time.sleep(1.0)
                    check = self.device.dump()
                    if check is not None and self.title(check) != title:
                        if not self.recover(path):
                            return
        self.scroll_to_top()


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--max-depth", type=int, default=5)
    parser.add_argument("--start", nargs="*", default=[])
    parser.add_argument("--panel-left", type=int, default=1250)
    parser.add_argument("--panel-right", type=int, default=1765)
    parser.add_argument("--top", type=int, default=130)
    parser.add_argument("--bottom", type=int, default=1120)
    args = parser.parse_args()
    device = Device(args.adb, args.serial)
    out = Path(args.out)
    crawler = Crawler(device, args.panel_left, args.panel_right, args.top, args.bottom, args.max_depth, out)
    device.clear_crash_log()
    if not crawler.recover(args.start):
        print("could not reach start path", file=sys.stderr)
        return 1
    crawler.crawl(args.start, len(args.start))
    out.write_text(json.dumps({"screens": crawler.screens, "crashes": crawler.crashes}, indent=1), encoding="utf-8")
    print(f"screens {crawler.screens}, crashes {len(crawler.crashes)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
