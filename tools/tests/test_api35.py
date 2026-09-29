import contextlib
import io
import struct
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import check_api35
import postprocess


def utf8(value):
    data = value.encode("utf-8")
    return b"\x01" + struct.pack(">H", len(data)) + data


def class_file(owner, target, name):
    pool = [
        utf8(owner),
        b"\x07" + struct.pack(">H", 1),
        utf8(target),
        b"\x07" + struct.pack(">H", 3),
        utf8(name),
        utf8("()Ljava/lang/Object;"),
        b"\x0c" + struct.pack(">HH", 5, 6),
        b"\x0b" + struct.pack(">HH", 4, 7),
        b"\x05" + struct.pack(">q", 42),
    ]
    header = b"\xca\xfe\xba\xbe" + struct.pack(">HHH", 0, 65, len(pool) + 2)
    return header + b"".join(pool) + struct.pack(">HHH", 0x21, 2, 0)


class ListCallsSuite(unittest.TestCase):
    def test_rewrites_calls_on_lists_declared_in_the_file(self):
        text = (
            "    private var items: MutableList<String> = mutableListOf()\n"
            "    val names = arrayListOf<String>()\n"
            "        items.removeFirst()\n"
            "        this.items.removeLast()\n"
            "        names.removeFirst()\n"
        )
        result, count = postprocess.api35_list_calls(text)
        self.assertEqual(count, 3)
        self.assertIn("        items.removeAt(0)\n", result)
        self.assertIn("        this.items.removeAt(this.items.lastIndex)\n", result)
        self.assertIn("        names.removeAt(0)\n", result)

    def test_leaves_deques_and_other_receivers_alone(self):
        text = (
            "    private val history: ArrayDeque<Int> = ArrayDeque()\n"
            "    private var items: MutableList<String> = mutableListOf()\n"
            "        history.removeFirst()\n"
            "        other.items.removeFirst()\n"
            "        myitems.removeFirst()\n"
        )
        result, count = postprocess.api35_list_calls(text)
        self.assertEqual(count, 0)
        self.assertEqual(result, text)

    def test_is_idempotent(self):
        text = "    private var items: MutableList<String> = mutableListOf()\n        items.removeFirst()\n"
        once, _ = postprocess.api35_list_calls(text)
        twice, count = postprocess.api35_list_calls(once)
        self.assertEqual(count, 0)
        self.assertEqual(once, twice)


class CheckApi35Suite(unittest.TestCase):
    def scan(self, files):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name, data in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
            return check_api35.scan(root)

    def test_reports_list_remove_first(self):
        found = self.scan({"a/Foo.class": class_file("a/Foo", "java/util/List", "removeFirst")})
        self.assertEqual(found, [("a.Foo", "java.util.List.removeFirst")])

    def test_ignores_deque_calls(self):
        found = self.scan({
            "a/Bar.class": class_file("a/Bar", "java/util/ArrayDeque", "removeFirst"),
            "a/Baz.class": class_file("a/Baz", "kotlin/collections/ArrayDeque", "removeFirst"),
        })
        self.assertEqual(found, [])

    def test_main_fails_when_calls_are_found(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Foo.class").write_bytes(class_file("Foo", "java/util/ArrayList", "removeLast"))
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                sys.argv = ["check_api35.py", "--classes", str(root)]
                code = check_api35.main()
            self.assertEqual(code, 1)
            self.assertIn("java.util.ArrayList.removeLast in Foo", output.getvalue())


if __name__ == "__main__":
    unittest.main()
