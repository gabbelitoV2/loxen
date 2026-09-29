import contextlib
import io
import json
import os
import random
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import tomllib
import unittest
import urllib.parse
import zipfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))
sys.path.insert(0, str(TOOLS / "system_tests_shims"))
import resources
import system_tests
from loxen_shims import lsof
from loxen_shims import ltc
from loxen_shims import startup

RE_LTCDUMP = re.compile(r"\S+\s+00:(\d+):(\d+):.*")
MOBLIN_CAPABILITIES = {"pip", "record", "background-streaming", "dual-mics", "stereo-mic", "gimbal"}


def frame_indices(lines):
    return [ltc.parse_timecode(line.split()[1]).index(30) for line in lines if RE_LTCDUMP.match(line)]


class LtcSuite(unittest.TestCase):
    def test_frame_bits_end_with_the_sync_word_and_have_an_even_number_of_zeros(self):
        for index in [0, 1, 29, 30, 1799, 107999, 2591999]:
            bits = ltc.frame_bits(ltc.Timecode.from_index(index, 30))
            self.assertEqual(tuple(bits[64:]), ltc.SYNC_WORD)
            self.assertEqual(bits.count(0) % 2, 0)

    def test_frame_bits_round_trip_with_user_bits(self):
        timecode = ltc.parse_timecode("23:59:58:29")
        self.assertEqual(ltc.decode_frame_bits(ltc.frame_bits(timecode, 0x12345678)), (timecode, 0x12345678))

    def test_generated_audio_decodes_to_consecutive_frames(self):
        start = ltc.parse_timecode("00:00:59:15")
        samples = ltc.encode(start, 60, 30, 48000, 4000)
        lines = ltc.dump_lines(ltc.decode(samples, 48000, 30), 30)
        self.assertEqual(lines[0], ltc.HEADER)
        self.assertNotIn("#DISCONTINUITY", lines)
        indices = frame_indices(lines)
        self.assertGreaterEqual(len(indices), 58)
        self.assertEqual(indices, list(range(indices[0], indices[0] + len(indices))))
        self.assertLessEqual(indices[0], start.index(30) + 1)

    def test_decodes_inverted_attenuated_and_noisy_audio_at_44100(self):
        samples = ltc.encode(ltc.parse_timecode("00:00:02:00"), 45, 30, 44100, 6000)
        generator = random.Random(1)
        noisy = [round(-0.3 * sample + generator.gauss(0, 150)) for sample in samples]
        indices = frame_indices(ltc.dump_lines(ltc.decode(noisy, 44100, 30), 30))
        self.assertGreaterEqual(len(indices), 43)
        self.assertEqual(indices, list(range(indices[0], indices[0] + len(indices))))

    def test_a_missing_frame_is_a_discontinuity(self):
        samples = ltc.encode(ltc.parse_timecode("00:00:00:00"), 30, 30, 48000, 4000)
        samples += ltc.encode(ltc.parse_timecode("00:00:01:15"), 15, 30, 48000, 4000)
        lines = ltc.dump_lines(ltc.decode(samples, 48000, 30), 30)
        self.assertEqual(lines.count("#DISCONTINUITY"), 1)
        after = lines[lines.index("#DISCONTINUITY") + 1]
        self.assertEqual(after.split()[1], "00:00:01:15")

    def test_ltcgen_and_ltcdump_write_what_systest_moblin_reads(self):
        directory = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, directory, ignore_errors=True)
        wav = directory / "FfmpegTestStream.wav"
        self.assertEqual(ltc.ltcgen_main(["--fps", "30", "--timecode", "00:00:00:00", "--duration", "00:00:04:00",
                                          str(wav)]), 0)
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(ltc.ltcdump_main(["--fps", "30", str(wav)]), 0)
        seconds = {60 * int(match.group(1)) + int(match.group(2))
                   for match in map(RE_LTCDUMP.match, output.getvalue().splitlines()) if match}
        self.assertEqual(seconds, {0, 1, 2, 3})
        self.assertNotIn("#DISCONTINUITY", output.getvalue())

    def test_duration_is_a_timecode_or_milliseconds(self):
        self.assertEqual(ltc.parse_duration("00:05:00:00", 30), 9000)
        self.assertEqual(ltc.parse_duration("2000", 30), 60)


class LsofSuite(unittest.TestCase):
    def test_parses_the_arguments_systest_moblin_passes(self):
        self.assertEqual(lsof.parse(["-nP", "-a", "-p", "123", "-iUDP:4004"]), (123, "udp", 4004))
        self.assertEqual(lsof.parse(["-nP", "-a", "-p", "7", "-iTCP:1935"]), (7, "tcp", 1935))

    def test_rejects_other_arguments(self):
        with self.assertRaises(SystemExit):
            lsof.parse(["-p", "1", "+D", "/tmp"])

    def test_finds_a_socket_of_the_process_by_port(self):
        try:
            import psutil  # noqa: F401
        except ImportError:
            self.skipTest("psutil is not installed")
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as bound:
            bound.bind(("127.0.0.1", 0))
            port = bound.getsockname()[1]
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                self.assertEqual(lsof.main(["-nP", "-a", "-p", str(os.getpid()), f"-iUDP:{port}"]), 0)
            self.assertEqual(len(output.getvalue().splitlines()), 2)
            self.assertEqual(lsof.main(["-nP", "-a", "-p", str(os.getpid()), f"-iTCP:{port}"]), 1)


class DrawtextFontSuite(unittest.TestCase):
    FONT = r"C:\Windows\Fonts\arial.ttf"

    def test_drawtext_without_a_font_gets_the_font_file(self):
        graph = "qrencode=text=n %{frame_num}:q=400:x=150,drawtext=fontsize=60:text=%{frame_num}:x=10:y=100"
        self.assertEqual(
            startup.with_font(graph, self.FONT),
            "qrencode=text=n %{frame_num}:q=400:x=150,"
            "drawtext=fontfile='C\\:/Windows/Fonts/arial.ttf':fontsize=60:text=%{frame_num}:x=10:y=100",
        )

    def test_a_given_font_and_other_filters_are_kept(self):
        for graph in ["drawtext=fontfile=a.ttf:text=x", "scale=640:360", "qrencode=text=drawtext"]:
            self.assertEqual(startup.with_font(graph, self.FONT), graph)

    def test_only_ffmpeg_command_lines_change(self):
        arguments = ["ffmpeg", "-vf", "drawtext=text=x"]
        self.assertIn("fontfile=", startup.ffmpeg_arguments(arguments, self.FONT)[2])
        self.assertIn("fontfile=", startup.ffmpeg_arguments([r"C:\bin\ffmpeg.exe", "-vf", "drawtext=text=x"],
                                                            self.FONT)[2])
        self.assertEqual(startup.ffmpeg_arguments(["python", "drawtext=text=x"], self.FONT),
                         ["python", "drawtext=text=x"])

    def test_the_tests_still_use_drawtext_without_a_font(self):
        ingests = (ROOT / "tests/suites/ingests.py").read_text(encoding="utf-8")
        self.assertIn('"drawtext=fontsize=60:text=%{frame_num}:x=10:y=100"', ingests)


class RunnerSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.directory, ignore_errors=True)

    def test_config_is_what_the_tests_expect(self):
        config = tomllib.loads(system_tests.config_text("192.0.2.8", "192.0.2.6"))
        device = config["device"][system_tests.DEVICE]
        self.assertEqual(device["moblin-ip-address"], "192.0.2.8")
        self.assertLessEqual(set(device["capabilities"]), MOBLIN_CAPABILITIES)
        self.assertEqual(config["general"]["tester-ip-address"], "192.0.2.6")
        self.assertEqual(config["general"]["remote-control-port"], system_tests.REMOTE_CONTROL_PORT)

    def test_capabilities_are_moblins(self):
        test_config = (ROOT / "tests/utils/config.py").read_text(encoding="utf-8")
        for capability in system_tests.CAPABILITIES:
            self.assertIn(f'= "{capability}"', test_config)

    def update(self, path, device_ip, tester_ip):
        original = system_tests.ROOT
        system_tests.ROOT = self.directory
        try:
            return system_tests.update_config(path, device_ip, tester_ip)
        finally:
            system_tests.ROOT = original

    def test_a_missing_config_is_written(self):
        path = self.directory / "config.toml"
        self.assertIsNotNone(self.update(path, "10.0.0.2", "10.0.0.1"))
        self.assertEqual(path.read_text(encoding="utf-8"), system_tests.config_text("10.0.0.2", "10.0.0.1"))

    def test_only_the_addresses_of_an_edited_config_change(self):
        path = self.directory / "config.toml"
        path.write_text(
            '[device.iphone]\nmoblin-ip-address = "10.0.0.9"\ncapabilities = []\n\n'
            '[device.loxen]\nmoblin-ip-address = "10.0.0.2"\ncapabilities = ["record"]\n\n'
            '[general]\ntester-ip-address = "10.0.0.1"\nremote-control-port = 2345\ngeneric-stream-urls = ["x"]\n',
            encoding="utf-8",
        )
        self.assertIsNotNone(self.update(path, "10.0.0.3", "10.0.0.4"))
        config = tomllib.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(config["device"]["iphone"]["moblin-ip-address"], "10.0.0.9")
        self.assertEqual(config["device"]["loxen"], {"moblin-ip-address": "10.0.0.3", "capabilities": ["record"]})
        self.assertEqual(config["general"]["tester-ip-address"], "10.0.0.4")
        self.assertEqual(config["general"]["generic-stream-urls"], ["x"])
        self.assertIsNone(self.update(path, "10.0.0.3", "10.0.0.4"))

    def test_remote_control_url_is_shell_safe_and_decodes_to_the_settings(self):
        url = system_tests.remote_control_url("192.0.2.6")
        self.assertRegex(url, r"^moblin://\?[A-Za-z0-9%._-]+$")
        settings = json.loads(urllib.parse.unquote(urllib.parse.urlsplit(url).query))
        self.assertEqual(settings["remoteControl"]["streamer"], {"enabled": True, "url": "ws://192.0.2.6:2345"})
        self.assertEqual(settings["remoteControl"]["password"], system_tests.REMOTE_CONTROL_PASSWORD)

    def test_remote_control_settings_match_the_tests(self):
        test_config = (ROOT / "tests/utils/config.py").read_text(encoding="utf-8")
        self.assertIn(f'REMOTE_CONTROL_PASSWORD = "{system_tests.REMOTE_CONTROL_PASSWORD}"', test_config)

    def test_device_ip_skips_loopback(self):
        output = "1: lo    inet 127.0.0.1/8 scope host lo\n30: wlan0    inet 192.0.2.8/24 brd 192.0.2.255\n"
        self.assertEqual(system_tests.parse_device_ip(output), "192.0.2.8")
        self.assertIsNone(system_tests.parse_device_ip(""))

    def test_one_device_prefers_usb_over_wireless_debugging_of_the_same_device(self):
        wireless = "adb-R00000000AB-abc123._adb-tls-connect._tcp"
        self.assertEqual(system_tests.one_device(["R00000000AB", wireless]), "R00000000AB")
        self.assertEqual(system_tests.one_device([wireless]), wireless)
        self.assertIsNone(system_tests.one_device(["R00000000AB", "emulator-5554"]))
        self.assertIsNone(system_tests.one_device([]))

    def test_settings_backups_follow_the_hardware_serial_over_usb_and_wifi(self):
        class Device(system_tests.ui_crawl.Device):
            def __init__(self, serial):
                super().__init__("adb", serial)

            def run(self, *args, timeout=30):
                return "R00000000AB\n" if args == ("shell", "getprop", "ro.serialno") else ""

        usb = system_tests.settings_backup(system_tests.hardware_serial(Device("R00000000AB")))
        wireless = system_tests.settings_backup(
            system_tests.hardware_serial(Device("adb-R00000000AB-abc123._adb-tls-connect._tcp")))
        self.assertEqual(usb, wireless)
        self.assertEqual(usb.name, "R00000000AB.settings")
        self.assertEqual(system_tests.settings_backup("192.0.2.8:5555").name, "192.0.2.8_5555.settings")

    def test_the_last_exit_reason_is_read_from_exit_info(self):
        class Device(system_tests.ui_crawl.Device):
            def run(self, *args, timeout=30):
                return (
                    "ApplicationExitInfo #0:\n"
                    "  timestamp=2026-09-29 17:29:28.006 pid=27210 realUid=10312 user=0\n"
                    "  process=com.loxen.app reason=3 (LOW_MEMORY) subreason=0 (UNKNOWN) status=0\n"
                    "ApplicationExitInfo #1:\n"
                    "  timestamp=2026-09-29 16:20:51.262 pid=697 user=0\n"
                    "  process=com.loxen.app reason=10 (USER REQUESTED) subreason=22 (REMOVE TASK)\n"
                )

        self.assertEqual(system_tests.last_exit_reason(Device("adb", "serial")), "LOW_MEMORY at 2026-09-29 17:29:28.006")

    def test_preparation_uses_the_tests_own_moblin_client(self):
        compile(system_tests.PREPARE, "prepare", "exec")
        self.assertIn(f"sys.exit({system_tests.NOT_CONNECTED})", system_tests.PREPARE)
        moblin = (ROOT / "tests/utils/moblin.py").read_text(encoding="utf-8")
        for method in ["def import_settings(self, overrides", "def end(self)", "def stop_recording(self)"]:
            self.assertIn(method, moblin)
        self.assertIn("settings = base_settings(self.config", moblin)

    def test_tool_patterns_pick_the_windows_builds(self):
        names = {
            "GyanD/codexffmpeg": (["ffmpeg-9.0.2-full_build.zip"], ["ffmpeg-9.0.2-essentials_build.zip",
                                                                   "ffmpeg-9.0.2-full_build-shared.zip"]),
            "bluenviron/mediamtx": (["mediamtx_v1.21.1_windows_amd64.zip"], ["mediamtx_v1.21.1_darwin_arm64.tar.gz"]),
            "sorairolake/qrtool": (["qrtool-v0.13.2-x86_64-pc-windows-msvc.zip"],
                                   ["qrtool-v0.13.2-x86_64-pc-windows-msvc.7z",
                                    "qrtool-v0.13.2-aarch64-pc-windows-msvc.zip"]),
        }
        for repository, pattern, _ in system_tests.WINDOWS_TOOLS:
            matching, other = names[repository]
            for name in matching:
                self.assertTrue(pattern.match(name), name)
            for name in other:
                self.assertFalse(pattern.match(name), name)

    def test_extract_takes_executables_from_nested_folders(self):
        archive = self.directory / "tools.zip"
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("ffmpeg-9.0.2-full_build/bin/ffmpeg.exe", b"ffmpeg")
            output.writestr("ffmpeg-9.0.2-full_build/bin/ffprobe.exe", b"ffprobe")
            output.writestr("ffmpeg-9.0.2-full_build/doc/ffmpeg.html", b"doc")
        target = self.directory / "bin"
        target.mkdir()
        system_tests.extract(archive, ("ffmpeg.exe", "ffprobe.exe"), target)
        self.assertEqual(sorted(path.name for path in target.iterdir()), ["ffmpeg.exe", "ffprobe.exe"])
        with self.assertRaises(SystemExit):
            system_tests.extract(archive, ("mediamtx.exe",), target)

    def test_firewall_script_allows_the_tools_on_private_networks(self):
        script = system_tests.firewall_script()
        for name in ["ffmpeg.exe", "mediamtx.exe", "python.exe"]:
            self.assertIn(name, script)
        self.assertIn("-Action Allow -Profile Private", script)
        self.assertIn("$_.Action -eq 'Block'", script)


class MirrorSystemTestsSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.directory, ignore_errors=True)

    def repository(self, name, files):
        root = self.directory / name
        root.mkdir()
        subprocess.run(["git", "init", "-q", str(root)], check=True)
        for path, text in files.items():
            (root / path).parent.mkdir(parents=True, exist_ok=True)
            (root / path).write_text(text, encoding="utf-8")
        if files:
            subprocess.run(["git", "-C", str(root), "add", *files], check=True)
        return root

    def test_copies_upstream_tests_and_removes_only_previously_copied_files(self):
        moblin = self.repository("moblin", {"tests/test.py": "new", "tests/utils/moblin.py": "moblin",
                                            "Moblin/App.swift": "swift"})
        loxen = self.repository("loxen", {"tests/test.py": "old", "tests/gone.py": "gone"})
        (loxen / "tests/config.toml").write_text("mine", encoding="utf-8")
        (loxen / "tests/logs").mkdir()
        (loxen / "tests/logs/test.log").write_text("log", encoding="utf-8")
        self.assertEqual(resources.mirror_system_tests(moblin, loxen), 2)
        self.assertEqual((loxen / "tests/test.py").read_text(encoding="utf-8"), "new")
        self.assertEqual((loxen / "tests/utils/moblin.py").read_text(encoding="utf-8"), "moblin")
        self.assertFalse((loxen / "tests/gone.py").exists())
        self.assertEqual((loxen / "tests/config.toml").read_text(encoding="utf-8"), "mine")
        self.assertTrue((loxen / "tests/logs/test.log").exists())
        self.assertFalse((loxen / "Moblin").exists())

    def test_keeps_the_copy_when_upstream_has_no_tests(self):
        moblin = self.repository("moblin", {"Moblin/App.swift": "swift"})
        loxen = self.repository("loxen", {"tests/test.py": "old"})
        self.assertIsNone(resources.mirror_system_tests(moblin, loxen))
        self.assertTrue((loxen / "tests/test.py").exists())


if __name__ == "__main__":
    unittest.main()
