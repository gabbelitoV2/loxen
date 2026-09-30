#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))
import ui_crawl

VENV = ROOT / ".venv"
CACHE = ROOT / ".system-tests"
BIN = CACHE / "bin"
CONFIG = ROOT / "tests" / "config.toml"
REQUIREMENTS = HERE / "system-tests-requirements.txt"
SHIMS = HERE / "system_tests_shims"
DEVICE = "loxen"
CAPABILITIES = ["record", "pip", "background-streaming"]
MINIMUM_PYTHON = (3, 14)
REMOTE_CONTROL_PORT = 2345
REMOTE_CONTROL_PASSWORD = "1234"
WINDOWS_TOOLS = [
    ("GyanD/codexffmpeg", re.compile(r"^ffmpeg-[\d.]+-full_build\.zip$"), ("ffmpeg.exe", "ffprobe.exe")),
    ("bluenviron/mediamtx", re.compile(r"^mediamtx_v[\d.]+_windows_amd64\.zip$"), ("mediamtx.exe",)),
    ("sorairolake/qrtool", re.compile(r"^qrtool-v[\d.]+-x86_64-pc-windows-msvc\.zip$"), ("qrtool.exe",)),
]
WINDOWS_FONT = Path(os.environ.get("WINDIR", r"C:\Windows")) / "Fonts" / "arial.ttf"
INET = re.compile(r"\binet (\d+\.\d+\.\d+\.\d+)/")
MEDIA_VOLUME = re.compile(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]")
MUSIC_STREAM = 3
DEVICE_SETTINGS = "files/SimpleStorage/settings"
BACKUPS = CACHE / "device-settings"
NOT_CONNECTED = 2
PREPARE = """
import sys
import time
from tests.utils.config import Config
from tests.utils.moblin import Moblin

moblin = Moblin(Config(), sys.argv[1])
try:
    moblin.__enter__()
except Exception:
    sys.exit(2)
try:
    moblin.end()
    moblin.stop_recording()
    for attempt in range(5):
        time.sleep(2)
        try:
            moblin.import_settings({})
            break
        except Exception:
            if attempt == 4:
                raise
finally:
    moblin.__exit__(None, None, None)
"""


def windows():
    return os.name == "nt"


def venv_python():
    return VENV / ("Scripts/python.exe" if windows() else "bin/python")


def run(command, check=True, label=None, **kwargs):
    print(label or "$ " + " ".join(str(part) for part in command), flush=True)
    result = subprocess.run([str(part) for part in command], **kwargs)
    if check and result.returncode != 0:
        sys.exit(f"command failed with exit code {result.returncode}")
    return result


def utf8_environment():
    return {**os.environ, "PYTHONUTF8": "1", "PYTHONIOENCODING": "utf-8"}


def requirements_stamp():
    digest = hashlib.sha256(REQUIREMENTS.read_bytes())
    if windows():
        for path in [SHIMS / "pyproject.toml", *sorted((SHIMS / "loxen_shims").glob("*.py"))]:
            digest.update(path.name.encode() + path.read_bytes())
    return digest.hexdigest()


def python_version(python):
    result = subprocess.run([str(python), "-c", "import sys; print(*sys.version_info[:2])"], capture_output=True,
                            text=True)
    if result.returncode != 0:
        return None
    return tuple(int(part) for part in result.stdout.split())


def version_key(path):
    return tuple(int(part) if part.isdigit() else -1 for part in re.split(r"[.-]", path.parent.name))


def python_candidates():
    yield sys.executable
    pyenv = Path.home() / ".pyenv" / "pyenv-win" / "versions"
    yield from sorted(pyenv.glob("*/python.exe"), key=version_key, reverse=True)
    for name in ["python3.14", "python3", "python"]:
        path = shutil.which(name)
        if path:
            yield path


def find_python():
    for candidate in python_candidates():
        version = python_version(candidate)
        if version is not None and version >= MINIMUM_PYTHON:
            return candidate
    raise SystemExit("Moblin's tests need Python 3.14 or newer; install it (winget install Python.Python.3.14, or "
                     "pyenv install 3.14) and run this again")


def ensure_venv(upgrade=False):
    stamp = CACHE / "venv.stamp"
    if venv_python().exists() and (python_version(venv_python()) or (0, 0)) < MINIMUM_PYTHON:
        shutil.rmtree(VENV)
    if not venv_python().exists():
        run([find_python(), "-m", "venv", VENV])
        upgrade = True
    if not upgrade and stamp.exists() and stamp.read_text(encoding="utf-8") == requirements_stamp():
        return
    env = utf8_environment()
    run([venv_python(), "-m", "pip", "install", "-q", "--upgrade", "pip"], env=env)
    run([venv_python(), "-m", "pip", "install", "-q", "--upgrade", "-r", REQUIREMENTS], env=env)
    if windows():
        run([venv_python(), "-m", "pip", "install", "-q", "--upgrade", SHIMS], env=env)
        (VENV / "Lib" / "site-packages" / "loxen_system_tests.pth").write_text("import loxen_shims.startup\n",
                                                                           encoding="utf-8")
    run([venv_python(), "-m", "playwright", "install", "chromium"], env=env)
    CACHE.mkdir(parents=True, exist_ok=True)
    stamp.write_text(requirements_stamp(), encoding="utf-8")


def latest_asset(repository, pattern):
    request = urllib.request.Request(f"https://api.github.com/repos/{repository}/releases/latest",
                                     headers={"Accept": "application/vnd.github+json"})
    with urllib.request.urlopen(request, timeout=60) as response:
        release = json.load(response)
    for asset in release["assets"]:
        if pattern.match(asset["name"]):
            return asset["name"], asset["browser_download_url"]
    raise SystemExit(f"no release asset of {repository} matches {pattern.pattern}")


def extract(archive, names, target):
    found = set()
    with zipfile.ZipFile(archive) as source:
        for member in source.infolist():
            name = Path(member.filename).name
            if name in names and not member.is_dir():
                with source.open(member) as data, open(target / name, "wb") as output:
                    shutil.copyfileobj(data, output)
                found.add(name)
    missing = set(names) - found
    if missing:
        raise SystemExit(f"{archive.name} does not contain {', '.join(sorted(missing))}")


def install_windows_tools(upgrade=False):
    BIN.mkdir(parents=True, exist_ok=True)
    for repository, pattern, names in WINDOWS_TOOLS:
        if not upgrade and all((BIN / name).exists() for name in names):
            continue
        asset, url = latest_asset(repository, pattern)
        print(f"downloading {asset}", flush=True)
        with tempfile.TemporaryDirectory(dir=CACHE) as directory:
            archive = Path(directory) / asset
            urllib.request.urlretrieve(url, archive)
            extract(archive, names, BIN)


def setup(upgrade=False):
    ensure_venv(upgrade)
    if windows():
        install_windows_tools(upgrade)


def find_adb():
    candidates = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    local_properties = ROOT / "local.properties"
    if local_properties.exists():
        for line in local_properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                candidates.append(line.split("=", 1)[1].replace("\\:", ":").replace("\\\\", "\\"))
    for sdk in candidates:
        if sdk:
            adb = Path(sdk) / "platform-tools" / ("adb.exe" if windows() else "adb")
            if adb.exists():
                return str(adb)
    adb = shutil.which("adb")
    if adb is None:
        raise SystemExit("adb not found: set ANDROID_HOME or sdk.dir in local.properties")
    return adb


def find_serial(adb, requested):
    if requested:
        return requested
    if os.environ.get("ANDROID_SERIAL"):
        return os.environ["ANDROID_SERIAL"]
    output = subprocess.run([adb, "devices"], capture_output=True, text=True).stdout
    serial = one_device(line.split("\t")[0] for line in output.splitlines()[1:] if line.endswith("\tdevice"))
    if serial is None:
        raise SystemExit("none or several devices connected, pick one with --serial")
    return serial


def one_device(serials):
    serials = list(serials)
    devices = [serial for serial in serials if not any(serial.startswith(f"adb-{other}-") for other in serials)]
    return devices[0] if len(devices) == 1 else None


def parse_device_ip(output):
    for address in INET.findall(output):
        if not address.startswith("127."):
            return address
    return None


def tester_ip_for(device_ip):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
        probe.connect((device_ip, 9))
        return probe.getsockname()[0]


def config_text(device_ip, tester_ip):
    capabilities = ", ".join(f'"{capability}"' for capability in CAPABILITIES)
    return (
        f"[device.{DEVICE}]\n"
        f'moblin-ip-address = "{device_ip}"\n'
        f"capabilities = [{capabilities}]\n"
        "\n"
        "[general]\n"
        f'tester-ip-address = "{tester_ip}"\n'
        f"remote-control-port = {REMOTE_CONTROL_PORT}\n"
        "generic-stream-urls = []\n"
    )


def update_config(path, device_ip, tester_ip):
    if not path.exists():
        path.write_text(config_text(device_ip, tester_ip), encoding="utf-8", newline="\n")
        return f"wrote {path.relative_to(ROOT)}"
    text = path.read_text(encoding="utf-8")
    section = re.search(rf"^\[device\.{DEVICE}\]\n(.*?)(?=^\[|\Z)", text, re.MULTILINE | re.DOTALL)
    if section is None:
        raise SystemExit(f"{path} has no [device.{DEVICE}] section")
    body = re.sub(r'^moblin-ip-address = ".*"$', f'moblin-ip-address = "{device_ip}"', section.group(1),
                  flags=re.MULTILINE)
    updated = text[: section.start(1)] + body + text[section.end(1):]
    updated = re.sub(r'^tester-ip-address = ".*"$', f'tester-ip-address = "{tester_ip}"', updated, flags=re.MULTILINE)
    if updated == text:
        return None
    path.write_text(updated, encoding="utf-8", newline="\n")
    return f"updated the addresses in {path.relative_to(ROOT)}"


def remote_control_url(tester_ip):
    settings = {
        "remoteControl": {
            "streamer": {"enabled": True, "url": f"ws://{tester_ip}:{REMOTE_CONTROL_PORT}"},
            "password": REMOTE_CONTROL_PASSWORD,
        }
    }
    return "moblin://?" + urllib.parse.quote(json.dumps(settings, separators=(",", ":")), safe="")


def find_node(device, text, seconds):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        root = device.dump()
        if root is not None:
            for node in ui_crawl.nodes(root):
                if node["text"] == text:
                    return node
        time.sleep(1)
    return None


def connect_app(device, tester_ip):
    if device.run("shell", "pm", "path", ui_crawl.PACKAGE).strip() == "":
        raise SystemExit(f"{ui_crawl.PACKAGE} is not installed on {device.serial}")
    device.key("KEYCODE_WAKEUP")
    device.run("shell", "wm", "dismiss-keyguard")
    device.launch()
    time.sleep(3)
    device.run("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", f"'{remote_control_url(tester_ip)}'",
               ui_crawl.PACKAGE)
    button = find_node(device, "Import settings", 20)
    if button is None:
        raise SystemExit("Loxen did not ask to import the remote control settings (is it live or recording?)")
    x1, y1, x2, y2 = button["bounds"]
    device.tap((x1 + x2) // 2, (y1 + y2) // 2)
    print(f"Loxen connects to the test assistant at ws://{tester_ip}:{REMOTE_CONTROL_PORT}", flush=True)


def settings_backup(serial):
    return BACKUPS / (re.sub(r"[^A-Za-z0-9._-]", "_", serial) + ".settings")


def hardware_serial(device):
    return device.run("shell", "getprop", "ro.serialno").strip() or device.serial


def backup_settings(device):
    path = settings_backup(hardware_serial(device))
    if path.exists():
        return None
    data = subprocess.run([device.adb, "-s", device.serial, "exec-out", "run-as", ui_crawl.PACKAGE, "cat",
                           DEVICE_SETTINGS], capture_output=True).stdout
    if not data or data.startswith(b"run-as:"):
        print("warning: could not back up Loxen's settings (only debug builds allow it); the tests replace them")
        return None
    BACKUPS.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return path


def restore_settings(args):
    adb = find_adb()
    device = ui_crawl.Device(adb, find_serial(adb, args.serial))
    path = settings_backup(hardware_serial(device))
    if not path.exists():
        raise SystemExit(f"no backup of {device.serial}'s settings in {BACKUPS}")
    device.key("KEYCODE_HOME")
    time.sleep(2)
    device.run("shell", "am", "force-stop", ui_crawl.PACKAGE)
    subprocess.run([adb, "-s", device.serial, "exec-in", "run-as", ui_crawl.PACKAGE, "sh", "-c",
                    f"cat > {DEVICE_SETTINGS}"], input=path.read_bytes(), check=True)
    device.launch()
    print(f"restored Loxen's settings from {path.relative_to(ROOT)}")
    return 0


def last_exit_reason(device):
    output = device.run("shell", "dumpsys", "activity", "exit-info", ui_crawl.PACKAGE)
    match = re.search(r"timestamp=(\S+ \S+).*?reason=\d+ \(([^)]+)\)", output, re.DOTALL)
    return f"{match.group(2)} at {match.group(1)}" if match else "unknown reason"


class Watchdog(threading.Thread):
    def __init__(self, device, interval=5):
        super().__init__(daemon=True)
        self.device = device
        self.interval = interval
        self.stopped = threading.Event()
        self.restarts = []

    def run(self):
        while not self.stopped.wait(self.interval):
            if self.device.pid() == "":
                reason = last_exit_reason(self.device)
                self.restarts.append(reason)
                print(f"warning: Loxen was not running ({reason}), starting it again", flush=True)
                self.device.launch()

    def stop(self):
        self.stopped.set()
        self.join(timeout=2 * self.interval)


def prepare_app():
    command = [venv_python(), "-c", PREPARE, DEVICE]
    return run(command, check=False, cwd=ROOT, env=test_environment(),
               label="waiting up to a minute for Loxen, then ending any stream or recording and importing the base "
                     "settings of the tests").returncode


def test_environment():
    env = utf8_environment()
    paths = [str(BIN), str(venv_python().parent), env.get("PATH", "")]
    if windows() and shutil.which("openssl") is None:
        git = shutil.which("git")
        if git:
            usr_bin = Path(git).resolve().parent.parent / "usr" / "bin"
            if (usr_bin / "openssl.exe").exists():
                paths.append(str(usr_bin))
    env["PATH"] = os.pathsep.join(paths)
    if windows() and WINDOWS_FONT.exists():
        env["LOXEN_DRAWTEXT_FONT"] = str(WINDOWS_FONT)
    return env


def prepare_device(args):
    adb = find_adb()
    device = ui_crawl.Device(adb, find_serial(adb, args.serial))
    device_ip = parse_device_ip(device.run("shell", "ip", "-f", "inet", "addr", "show", "wlan0"))
    if device_ip is None:
        device_ip = parse_device_ip(device.run("shell", "ip", "-f", "inet", "addr"))
    if device_ip is None:
        raise SystemExit(f"{device.serial} has no IPv4 address; connect it to the same network as this computer")
    tester_ip = tester_ip_for(device_ip)
    message = update_config(CONFIG, device_ip, tester_ip)
    if message:
        print(message)
    print(f"device {device.serial} at {device_ip}, this computer at {tester_ip}", flush=True)
    return device, tester_ip


def run_tests(module, args, extra):
    setup()
    device, tester_ip = prepare_device(args)
    stay_on = device.run("shell", "settings", "get", "global", "stay_on_while_plugged_in").strip()
    device.run("shell", "svc", "power", "stayon", "true")
    volume = media_volume(device)
    if volume is not None:
        set_media_volume(device, volume[1])
    try:
        if not args.no_connect:
            backup = backup_settings(device)
            if backup:
                print(f"backed up Loxen's settings to {backup.relative_to(ROOT)} "
                      "(python tools/system_tests.py restore-settings puts them back)")
            result = prepare_app()
            if result == NOT_CONNECTED:
                connect_app(device, tester_ip)
                result = prepare_app()
            if result != 0:
                raise SystemExit("Loxen did not connect to the test assistant or did not import the base settings")
        watchdog = Watchdog(device)
        watchdog.start()
        command = [venv_python(), "-m", module, "--device", DEVICE, *extra]
        try:
            return run(command, check=False, cwd=ROOT, env=test_environment()).returncode
        finally:
            watchdog.stop()
            for reason in watchdog.restarts:
                print(f"Loxen was restarted during the run: {reason}")
    finally:
        if stay_on.isdigit():
            device.run("shell", "settings", "put", "global", "stay_on_while_plugged_in", stay_on)
        if volume is not None:
            set_media_volume(device, volume[0])


def media_volume(device):
    output = device.run("shell", "cmd", "media_session", "volume", "--stream", str(MUSIC_STREAM), "--get")
    match = MEDIA_VOLUME.search(output)
    return (int(match.group(1)), int(match.group(3))) if match else None


def set_media_volume(device, volume):
    device.run("shell", "cmd", "media_session", "volume", "--stream", str(MUSIC_STREAM), "--set", str(volume))


def firewall_script():
    base = Path(sys.executable)
    config = VENV / "pyvenv.cfg"
    if config.exists():
        for line in config.read_text(encoding="utf-8").splitlines():
            key, _, value = line.partition("=")
            if key.strip() == "home":
                base = Path(value.strip()) / "python.exe"
    programs = [BIN / "ffmpeg.exe", BIN / "mediamtx.exe", venv_python(), base]
    listed = ", ".join(f"'{program}'" for program in programs)
    return (
        f"foreach ($program in @({listed})) {{\n"
        "  Get-NetFirewallApplicationFilter -Program $program -ErrorAction SilentlyContinue | Get-NetFirewallRule |\n"
        "    Where-Object { $_.Direction -eq 'Inbound' -and $_.Action -eq 'Block' } | Remove-NetFirewallRule\n"
        "  $name = 'Loxen system tests: ' + $program\n"
        "  Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue | Remove-NetFirewallRule\n"
        "  New-NetFirewallRule -DisplayName $name -Direction Inbound -Program $program -Action Allow "
        "-Profile Private | Out-Null\n"
        "}\n"
    )


def firewall():
    if not windows():
        print("only needed on Windows")
        return 0
    setup()
    script = CACHE / "firewall.ps1"
    script.write_text(firewall_script(), encoding="utf-8-sig")
    print("Windows asks for administrator rights to let the device reach ffmpeg, mediamtx and Python on this "
          "computer (private networks only).", flush=True)
    return run(["powershell", "-NoProfile", "-Command",
                f"Start-Process powershell -Verb RunAs -Wait -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass',"
                f"'-File','\"{script}\"'"], check=False).returncode


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Run Moblin's system tests (tests/) against Loxen on an Android device.",
        epilog="Arguments after the command that this script does not know go to python -m tests.test "
               "(or tests.stability), for example a test name like Talkback, or --interactive.",
    )
    commands = parser.add_subparsers(dest="command", required=True)
    setup_parser = commands.add_parser("setup", help="create .venv and download ffmpeg, mediamtx and qrtool")
    setup_parser.add_argument("--upgrade", action="store_true", help="reinstall packages and download tools again")
    commands.add_parser("firewall", help="allow the device to reach the test tools on this computer (Windows)")
    for name in ["test", "stability"]:
        command = commands.add_parser(name, help=f"run python -m tests.{name} against Loxen")
        command.add_argument("--serial", help="adb serial of the device (default: the only connected one)")
        command.add_argument("--no-connect", action="store_true",
                             help="do not back up Loxen's settings and point its remote control at this computer first")
    restore = commands.add_parser("restore-settings",
                                  help="put back the Loxen settings that were on the device before the first test run")
    restore.add_argument("--serial", help="adb serial of the device (default: the only connected one)")
    args, extra = parser.parse_known_args()
    if args.command in ("setup", "firewall", "restore-settings") and extra:
        parser.error(f"unknown arguments: {' '.join(extra)}")
    if args.command == "setup":
        setup(args.upgrade)
        print("done. Next: python tools/system_tests.py firewall (once, Windows), then python tools/system_tests.py test")
        return 0
    if args.command == "firewall":
        return firewall()
    if args.command == "restore-settings":
        return restore_settings(args)
    return run_tests(f"tests.{'test' if args.command == 'test' else 'stability'}", args, extra)


if __name__ == "__main__":
    sys.exit(main())
