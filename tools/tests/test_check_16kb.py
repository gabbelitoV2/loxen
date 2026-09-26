import contextlib
import io
import struct
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import check_16kb

PT_LOAD = 1
PT_GNU_STACK = 0x6474E551


def elf64(segments):
    header = bytearray(64)
    header[:7] = b"\x7fELF\x02\x01\x01"
    struct.pack_into("<HHI", header, 16, 3, 183, 1)
    struct.pack_into("<Q", header, 32, 64)
    struct.pack_into("<HHHHHH", header, 52, 64, 56, len(segments), 64, 0, 0)
    table = b"".join(struct.pack("<IIQQQQQQ", kind, 5, 0, 0, 0, 0, 0, align) for kind, align in segments)
    return bytes(header) + table


def elf32(segments):
    header = bytearray(52)
    header[:7] = b"\x7fELF\x01\x01\x01"
    struct.pack_into("<HHI", header, 16, 3, 40, 1)
    struct.pack_into("<I", header, 28, 52)
    struct.pack_into("<HHHHHH", header, 40, 52, 32, len(segments), 40, 0, 0)
    table = b"".join(struct.pack("<IIIIIIII", kind, 0, 0, 0, 0, 0, 5, align) for kind, align in segments)
    return bytes(header) + table


ALIGNED = elf64([(PT_LOAD, 16384), (PT_LOAD, 16384), (PT_GNU_STACK, 16)])
UNALIGNED = elf64([(PT_LOAD, 16384), (PT_LOAD, 4096)])
AGP_BUNDLE_CONFIG = bytes.fromhex("0a081206312e31372e32120c0a0012040801100232020801")


def bundle_config(uncompressed, alignment):
    libraries = bytes([0x08, int(uncompressed), 0x10, alignment])
    optimizations = bytes([0x0A, 0x00, 0x12, len(libraries)]) + libraries
    return bytes.fromhex("0a081206312e31372e32") + bytes([0x12, len(optimizations)]) + optimizations


def padding_for(offset, name):
    need = -(offset + 30 + len(name.encode())) % check_16kb.PAGE_SIZE
    if 0 < need < 4:
        need += check_16kb.PAGE_SIZE
    if need == 0:
        return b""
    return struct.pack("<HH", 0xD935, need - 4) + bytes(need - 4)


def write_archive(path, entries):
    with zipfile.ZipFile(path, "w") as archive:
        for name, data, mode in entries:
            info = zipfile.ZipInfo(name)
            if mode == "deflated":
                info.compress_type = zipfile.ZIP_DEFLATED
            else:
                info.compress_type = zipfile.ZIP_STORED
                if mode == "aligned":
                    info.extra = padding_for(archive.fp.tell(), name)
            archive.writestr(info, data)
    return path


class ElfSuite(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def library(self, data, name="libfoo.so"):
        path = self.root / name
        path.write_bytes(data)
        return check_16kb.inspect(path)[0]

    def test_aligned_64_bit_library_passes(self):
        library = self.library(ALIGNED)
        self.assertEqual(library.load_alignments, [16384, 16384])
        self.assertFalse(library.failed)

    def test_4_kb_load_segment_fails(self):
        library = self.library(UNALIGNED)
        self.assertTrue(library.failed)
        self.assertIn("LOAD p_align 4096 < 16384", library.problems)

    def test_larger_power_of_two_alignment_passes(self):
        self.assertFalse(self.library(elf64([(PT_LOAD, 65536)])).failed)

    def test_32_bit_library_is_reported_but_not_enforced(self):
        library = self.library(elf32([(PT_LOAD, 4096)]))
        self.assertFalse(library.is_64_bit)
        self.assertTrue(library.problems)
        self.assertFalse(library.failed)
        self.assertFalse(self.library(elf32([(PT_LOAD, 16384)]), "libbar.so").problems)

    def test_library_without_load_segments_fails(self):
        self.assertIn("no LOAD segments", self.library(elf64([(PT_GNU_STACK, 16)])).problems)

    def test_not_an_elf_file_fails(self):
        library = self.library(b"not a shared library")
        self.assertTrue(library.failed)
        self.assertEqual(library.problems, ["not an ELF file"])

    def test_truncated_program_headers_fail(self):
        self.assertTrue(self.library(ALIGNED[:80]).failed)

    def test_big_endian_library(self):
        header = bytearray(64)
        header[:7] = b"\x7fELF\x02\x02\x01"
        struct.pack_into(">Q", header, 32, 64)
        struct.pack_into(">HH", header, 54, 56, 1)
        data = bytes(header) + struct.pack(">IIQQQQQQ", PT_LOAD, 5, 0, 0, 0, 0, 0, 4096)
        self.assertEqual(self.library(data).load_alignments, [4096])


class ArchiveSuite(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def test_stored_libraries_at_16_kb_offsets_pass(self):
        apk = write_archive(self.root / "app.apk", [
            ("classes.dex", b"dex", "deflated"),
            ("lib/arm64-v8a/liba.so", ALIGNED, "aligned"),
            ("lib/x86_64/libb.so", ALIGNED, "aligned"),
        ])
        libraries = check_16kb.inspect(apk)
        self.assertEqual([library.path for library in libraries], ["lib/arm64-v8a/liba.so", "lib/x86_64/libb.so"])
        for library in libraries:
            self.assertTrue(library.stored)
            self.assertEqual(library.data_offset % 16384, 0)
            self.assertFalse(library.failed)

    def test_stored_library_at_4_kb_offset_fails(self):
        apk = write_archive(self.root / "app.apk", [
            ("classes.dex", b"dex", "deflated"),
            ("lib/arm64-v8a/liba.so", ALIGNED, "unaligned"),
        ])
        [library] = check_16kb.inspect(apk)
        self.assertTrue(library.failed)
        self.assertIn("not a multiple of 16384", library.problems[0])

    def test_compressed_library_needs_no_zip_alignment(self):
        apk = write_archive(self.root / "app.apk", [
            ("classes.dex", b"dex", "deflated"),
            ("lib/arm64-v8a/liba.so", ALIGNED, "deflated"),
        ])
        [library] = check_16kb.inspect(apk)
        self.assertFalse(library.stored)
        self.assertFalse(library.failed)

    def test_elf_alignment_is_checked_inside_apk(self):
        apk = write_archive(self.root / "app.apk", [("lib/arm64-v8a/liba.so", UNALIGNED, "aligned")])
        [library] = check_16kb.inspect(apk)
        self.assertTrue(library.zip_aligned)
        self.assertTrue(library.failed)

    def bundle(self, config, *libraries):
        entries = [("base/manifest/AndroidManifest.xml", b"manifest", "deflated")]
        if config is not None:
            entries.append((check_16kb.BUNDLE_CONFIG, config, "deflated"))
        return check_16kb.inspect(write_archive(self.root / "app.aab", entries + list(libraries)))

    def test_bundle_checks_elf_alignment_but_not_zip_offsets(self):
        first, second, config = self.bundle(
            AGP_BUNDLE_CONFIG,
            ("base/lib/arm64-v8a/liba.so", ALIGNED, "unaligned"),
            ("base/lib/x86_64/libb.so", UNALIGNED, "deflated"),
        )
        self.assertIsNone(first.stored)
        self.assertFalse(first.failed)
        self.assertTrue(second.failed)
        self.assertFalse(config.failed)

    def test_bundle_from_agp_asks_for_16_kb_aligned_libraries(self):
        [config] = self.bundle(AGP_BUNDLE_CONFIG)
        self.assertTrue(config.uncompressed)
        self.assertEqual(config.alignment, 2)
        self.assertIn("16 KB", config.description)

    def test_bundle_with_4_kb_or_unspecified_alignment_fails(self):
        self.assertTrue(self.bundle(bundle_config(True, 1))[0].failed)
        self.assertTrue(self.bundle(bundle_config(True, 0))[0].failed)
        self.assertFalse(self.bundle(bundle_config(True, 3))[0].failed)

    def test_bundle_with_compressed_libraries_passes(self):
        [config] = self.bundle(bundle_config(False, 0))
        self.assertFalse(config.failed)
        self.assertIn("compressed", config.description)

    def test_bundle_without_or_with_a_broken_config_fails(self):
        self.assertEqual(self.bundle(None)[0].error, "no BundleConfig.pb")
        self.assertTrue(self.bundle(bytes.fromhex("12400a"))[0].failed)

    def test_directory_is_searched_for_packages_and_libraries(self):
        (self.root / "apk").mkdir()
        write_archive(self.root / "apk" / "app-debug.apk", [("lib/arm64-v8a/liba.so", ALIGNED, "aligned")])
        (self.root / "apk" / "output-metadata.json").write_text("{}")
        (self.root / "jni" / "x86_64").mkdir(parents=True)
        (self.root / "jni" / "x86_64" / "libc.so").write_bytes(UNALIGNED)
        libraries = check_16kb.inspect(self.root)
        self.assertEqual(sorted(library.path for library in libraries), ["lib/arm64-v8a/liba.so", "libc.so"])
        self.assertEqual([library.path for library in libraries if library.failed], ["libc.so"])


class MainSuite(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def run_main(self, *args):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            status = check_16kb.main([str(arg) for arg in args])
        return status, output.getvalue()

    def test_exit_status_and_report(self):
        good = write_archive(self.root / "good.apk", [("lib/arm64-v8a/liba.so", ALIGNED, "aligned")])
        bad = write_archive(self.root / "bad.apk", [("lib/arm64-v8a/liba.so", UNALIGNED, "unaligned")])
        status, output = self.run_main(good)
        self.assertEqual(status, 0)
        self.assertIn("1 native libraries checked, 0 not 16 KB page aligned", output)
        status, output = self.run_main("--quiet", good, bad)
        self.assertEqual(status, 1)
        self.assertNotIn("good.apk", output)
        self.assertIn("FAIL", output)
        self.assertIn("2 native libraries checked, 1 not 16 KB page aligned", output)

    def test_missing_path(self):
        status, output = self.run_main(self.root / "missing.apk")
        self.assertEqual(status, 2)
        self.assertIn("not found", output)


if __name__ == "__main__":
    unittest.main()
