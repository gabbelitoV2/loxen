#!/usr/bin/env python3
import argparse
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLASSES = ROOT / "app/build/tmp/kotlin-classes/debug"
OWNERS = {"java/util/List", "java/util/ArrayList", "java/util/AbstractList", "java/util/Vector",
          "java/util/SequencedCollection"}
METHODS = {"removeFirst", "removeLast", "getFirst", "getLast", "addFirst", "addLast", "reversed"}
SIZES = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}


def references(data):
    count = struct.unpack_from(">H", data, 8)[0]
    pool = [None] * count
    offset = 10
    index = 1
    while index < count:
        tag = data[offset]
        if tag == 1:
            length = struct.unpack_from(">H", data, offset + 1)[0]
            pool[index] = (1, data[offset + 3:offset + 3 + length].decode("utf-8", "replace"))
            offset += 3 + length
        else:
            size = SIZES[tag]
            if tag in (7, 8, 16, 19, 20):
                pool[index] = (tag, struct.unpack_from(">H", data, offset + 1)[0])
            elif tag in (9, 10, 11, 12, 17, 18):
                pool[index] = (tag, struct.unpack_from(">HH", data, offset + 1))
            offset += 1 + size
        index += 2 if tag in (5, 6) else 1
    this_class = struct.unpack_from(">H", data, offset + 2)[0]
    owner = pool[pool[this_class][1]][1]
    for entry in pool:
        if entry and entry[0] in (10, 11):
            class_index, name_and_type = entry[1]
            target = pool[pool[class_index][1]][1]
            name = pool[pool[name_and_type][1][0]][1]
            yield owner, target, name


def scan(classes):
    found = set()
    for path in classes.rglob("*.class"):
        for owner, target, name in references(path.read_bytes()):
            if target in OWNERS and name in METHODS:
                found.add((owner.replace("/", "."), f"{target.replace('/', '.')}.{name}"))
    return sorted(found)


def main():
    parser = argparse.ArgumentParser(
        description="Report calls to java.util.List methods that only exist from Android 15 (API 35) "
        "and crash on older devices, such as MutableList.removeFirst() compiled against Java 21."
    )
    parser.add_argument("--classes", type=Path, default=CLASSES)
    args = parser.parse_args()
    if not args.classes.exists():
        print(f"{args.classes} not found; compile first", file=sys.stderr)
        return 2
    found = scan(args.classes)
    for owner, method in found:
        print(f"{method} in {owner}")
    print(f"{len(found)} API 35 list calls")
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
