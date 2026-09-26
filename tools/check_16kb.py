#!/usr/bin/env python3
import argparse
import struct
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

PAGE_SIZE = 16384
PT_LOAD = 1
ELFCLASS32 = 1
ELFCLASS64 = 2
LOCAL_HEADER_SIZE = 30
LOCAL_HEADER_SIGNATURE = b"PK\x03\x04"
BUNDLE_CONFIG = "BundleConfig.pb"
PAGE_ALIGNMENT_NAMES = {0: "unspecified", 1: "4 KB", 2: "16 KB", 3: "64 KB"}
PAGE_ALIGNMENT_16K = 2


class ElfError(Exception):
    pass


@dataclass
class Library:
    source: str
    path: str
    elf_class: int = 0
    load_alignments: list = field(default_factory=list)
    stored: bool | None = None
    data_offset: int | None = None
    error: str | None = None

    @property
    def is_64_bit(self):
        return self.elf_class == ELFCLASS64

    @property
    def elf_aligned(self):
        return bool(self.load_alignments) and all(align >= PAGE_SIZE for align in self.load_alignments)

    @property
    def zip_aligned(self):
        return not self.stored or self.data_offset % PAGE_SIZE == 0

    @property
    def problems(self):
        if self.error:
            return [self.error]
        problems = []
        if not self.load_alignments:
            problems.append("no LOAD segments")
        elif not self.elf_aligned:
            problems.append(f"LOAD p_align {min(self.load_alignments)} < {PAGE_SIZE}")
        if not self.zip_aligned:
            problems.append(f"stored at zip offset {self.data_offset}, not a multiple of {PAGE_SIZE}")
        return problems

    @property
    def enforced(self):
        return self.error is not None or self.is_64_bit

    @property
    def failed(self):
        return self.enforced and bool(self.problems)

    @property
    def description(self):
        if self.error:
            return self.error
        parts = [f"{'64' if self.is_64_bit else '32'}-bit", f"LOAD p_align {min(self.load_alignments or [0])}"]
        if self.stored is not None:
            parts.append(f"zip offset {self.data_offset}" if self.stored else "compressed")
        problems = self.problems
        if problems:
            parts.append("; ".join(problems))
            if not self.enforced:
                parts.append("not enforced for 32-bit ABIs")
        return ", ".join(parts)


@dataclass
class BundleAlignment:
    source: str
    path: str = BUNDLE_CONFIG
    uncompressed: bool = False
    alignment: int = 0
    error: str | None = None

    @property
    def failed(self):
        return self.error is not None or (self.uncompressed and self.alignment < PAGE_ALIGNMENT_16K)

    @property
    def description(self):
        if self.error:
            return self.error
        if not self.uncompressed:
            return "native libraries are compressed in the generated APKs"
        name = PAGE_ALIGNMENT_NAMES.get(self.alignment, str(self.alignment))
        return f"uncompressed native libraries are aligned to {name} pages in the generated APKs"


def read_elf(read):
    header = read(64)
    if header[:4] != b"\x7fELF":
        raise ElfError("not an ELF file")
    elf_class = header[4]
    endian = {1: "<", 2: ">"}.get(header[5])
    if endian is None or elf_class not in (ELFCLASS32, ELFCLASS64):
        raise ElfError("unknown ELF class or byte order")
    if elf_class == ELFCLASS64:
        if len(header) < 64:
            raise ElfError("truncated ELF header")
        (phoff,) = struct.unpack_from(endian + "Q", header, 32)
        phentsize, phnum = struct.unpack_from(endian + "HH", header, 54)
        entry = endian + "I44xQ"
        minimum = 56
    else:
        if len(header) < 52:
            raise ElfError("truncated ELF header")
        (phoff,) = struct.unpack_from(endian + "I", header, 28)
        phentsize, phnum = struct.unpack_from(endian + "HH", header, 42)
        entry = endian + "I24xI"
        minimum = 32
    if phnum and phentsize < minimum:
        raise ElfError("program header entries are too small")
    table = read(phoff + phentsize * phnum)
    if len(table) < phoff + phentsize * phnum:
        raise ElfError("truncated program header table")
    alignments = []
    for index in range(phnum):
        p_type, p_align = struct.unpack_from(entry, table, phoff + index * phentsize)
        if p_type == PT_LOAD:
            alignments.append(p_align)
    return elf_class, alignments


def prefix_reader(data_source):
    cache = bytearray()

    def read(size):
        if len(cache) < size:
            cache.extend(data_source(size - len(cache)))
        return bytes(cache[:size])

    return read


def inspect_bytes(library, read):
    try:
        library.elf_class, library.load_alignments = read_elf(read)
    except (ElfError, struct.error) as error:
        library.error = str(error) or "unreadable ELF file"
    return library


def local_data_offset(archive_file, info):
    archive_file.seek(info.header_offset)
    header = archive_file.read(LOCAL_HEADER_SIZE)
    if len(header) < LOCAL_HEADER_SIZE or header[:4] != LOCAL_HEADER_SIGNATURE:
        raise ElfError(f"bad local header for {info.filename}")
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return info.header_offset + LOCAL_HEADER_SIZE + name_length + extra_length


def protobuf_fields(data):
    position = 0

    def varint():
        nonlocal position
        value = 0
        shift = 0
        while True:
            if position >= len(data):
                raise ValueError("truncated varint")
            byte = data[position]
            position += 1
            value |= (byte & 0x7F) << shift
            shift += 7
            if not byte & 0x80:
                return value

    while position < len(data):
        key = varint()
        number, wire_type = key >> 3, key & 7
        if wire_type == 0:
            yield number, wire_type, varint()
        elif wire_type == 1:
            position += 8
        elif wire_type == 2:
            length = varint()
            if position + length > len(data):
                raise ValueError("truncated field")
            yield number, wire_type, data[position:position + length]
            position += length
        elif wire_type == 5:
            position += 4
        else:
            raise ValueError(f"unsupported wire type {wire_type}")


def bundle_alignment(source, data):
    result = BundleAlignment(source=source)
    try:
        for number, wire_type, optimizations in protobuf_fields(data):
            if number != 2 or wire_type != 2:
                continue
            for inner, inner_type, libraries in protobuf_fields(optimizations):
                if inner != 2 or inner_type != 2:
                    continue
                for setting, setting_type, value in protobuf_fields(libraries):
                    if setting == 1 and setting_type == 0:
                        result.uncompressed = bool(value)
                    elif setting == 2 and setting_type == 0:
                        result.alignment = value
    except ValueError as error:
        result.error = f"unreadable {BUNDLE_CONFIG}: {error}"
    return result


def inspect_archive(path, bundle):
    checks = []
    with zipfile.ZipFile(path) as archive, open(path, "rb") as raw:
        for info in archive.infolist():
            if info.is_dir() or not info.filename.endswith(".so"):
                continue
            library = Library(source=str(path), path=info.filename)
            with archive.open(info) as stream:
                inspect_bytes(library, prefix_reader(stream.read))
            if not bundle:
                library.stored = info.compress_type == zipfile.ZIP_STORED
                if library.stored:
                    try:
                        library.data_offset = local_data_offset(raw, info)
                    except ElfError as error:
                        library.error = str(error)
            checks.append(library)
        if bundle:
            try:
                data = archive.read(BUNDLE_CONFIG)
            except KeyError:
                checks.append(BundleAlignment(source=str(path), error=f"no {BUNDLE_CONFIG}"))
            else:
                checks.append(bundle_alignment(str(path), data))
    return checks


def inspect_file(path):
    library = Library(source=str(path.parent), path=path.name)
    with open(path, "rb") as stream:
        return inspect_bytes(library, prefix_reader(stream.read))


def inspect(path):
    path = Path(path)
    if path.is_dir():
        checks = []
        for child in sorted(path.rglob("*")):
            if child.is_file() and child.suffix in (".apk", ".aab", ".so"):
                checks.extend(inspect(child))
        return checks
    if path.suffix == ".apk":
        return inspect_archive(path, bundle=False)
    if path.suffix == ".aab":
        return inspect_archive(path, bundle=True)
    return [inspect_file(path)]


def main(argv=None):
    parser = argparse.ArgumentParser(
        description=f"Check that every native library is ready for {PAGE_SIZE // 1024} KB memory pages: each ELF LOAD "
        f"segment of a 64-bit .so has p_align >= {PAGE_SIZE}, each uncompressed .so in an APK starts at a zip offset "
        f"that is a multiple of {PAGE_SIZE}, and an app bundle asks for {PAGE_SIZE // 1024} KB aligned uncompressed "
        "libraries in the APKs generated from it. Takes APKs, app bundles, .so files and directories of those. "
        "Exits with 1 when a check fails."
    )
    parser.add_argument("paths", nargs="+", type=Path)
    parser.add_argument("--quiet", action="store_true", help="only print checks that fail")
    args = parser.parse_args(argv)
    checks = []
    for path in args.paths:
        if not path.exists():
            print(f"{path}: not found")
            return 2
        checks.extend(inspect(path))
    for check in checks:
        if args.quiet and not check.failed:
            continue
        status = "FAIL" if check.failed else "ok  "
        print(f"{status} {check.source}!{check.path}: {check.description}")
    libraries = [check for check in checks if isinstance(check, Library)]
    failed_libraries = [library for library in libraries if library.failed]
    bundles = [check for check in checks if isinstance(check, BundleAlignment)]
    print(f"{len(libraries)} native libraries checked, {len(failed_libraries)} not {PAGE_SIZE // 1024} KB page aligned")
    if bundles:
        failed_bundles = [bundle for bundle in bundles if bundle.failed]
        print(f"{len(bundles)} app bundles checked, {len(failed_bundles)} with a bad native library alignment")
    return 1 if any(check.failed for check in checks) else 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
