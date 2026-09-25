#!/usr/bin/env python3
import argparse
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SWIFT_DIR = "Moblin/Integrations/Tesla/Protobuf"
OUTPUT_DIR = ROOT / "app/src/main/java/com/moblin/android/integrations/tesla/protobuf"
PACKAGE = "com.moblin.android.integrations.tesla.protobuf"
SHIM_PACKAGE = "com.moblin.android.platform.swiftprotobuf"
TIMESTAMP = "SwiftProtobuf.Google_Protobuf_Timestamp"

IDENT = r"`?([A-Za-z_][A-Za-z0-9_]*)`?"
TYPE = r"([A-Za-z_][A-Za-z0-9_.]*)"
MODIFIERS = r"(?:nonisolated )?"
ENUM_DECLARATION = MODIFIERS + r"enum " + IDENT + r": SwiftProtobuf\.Enum, Swift\.CaseIterable \{"
STRUCT_DECLARATION = MODIFIERS + r"struct " + IDENT + r": (?:@unchecked )?Sendable \{"

KOTLIN_KEYWORDS = {
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
    "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
    "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import",
    "init", "param", "property", "receiver", "set", "setparam", "value", "where", "abstract", "actual", "annotation",
    "companion", "const", "crossinline", "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
    "internal", "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public",
    "reified", "sealed", "suspend", "tailrec", "vararg", "it",
}
KOTLIN_TYPE_NAMES = {
    "Any", "Boolean", "ByteArray", "Double", "Float", "Int", "List", "Long", "MutableList", "String", "UInt", "ULong",
    "Unit", "Companion", "BinaryDecoder", "BinaryEncodingVisitor", "GeneratedMessage", "ProtobufList",
    "ProtobufOneofCase", "Google_Protobuf_Timestamp",
}
RESERVED_MEMBERS = {
    "class", "unknownFields", "isInitialized", "decodeMessage", "traverse", "copy", "equals", "hashCode",
    "toString", "serializedData", "merge",
}
RESERVED_ENUM_CASES = {
    "name", "ordinal", "entries", "values", "valueOf", "UNRECOGNIZED", "Companion", "rawValue", "allCases",
    "fromRawValue", "compareTo", "equals", "hashCode", "toString", "declaringJavaClass",
}


@dataclass(frozen=True)
class Scalar:
    swift: str
    kotlin: str
    default: str
    nonzero: str
    swift_default: str
    swift_condition: str


SCALARS = {
    "Int32": Scalar("Int32", "Int", "0", "{} != 0", "0", "zero"),
    "SInt32": Scalar("Int32", "Int", "0", "{} != 0", "0", "zero"),
    "SFixed32": Scalar("Int32", "Int", "0", "{} != 0", "0", "zero"),
    "Int64": Scalar("Int64", "Long", "0L", "{} != 0L", "0", "zero"),
    "SInt64": Scalar("Int64", "Long", "0L", "{} != 0L", "0", "zero"),
    "SFixed64": Scalar("Int64", "Long", "0L", "{} != 0L", "0", "zero"),
    "UInt32": Scalar("UInt32", "UInt", "0u", "{} != 0u", "0", "zero"),
    "Fixed32": Scalar("UInt32", "UInt", "0u", "{} != 0u", "0", "zero"),
    "UInt64": Scalar("UInt64", "ULong", "0uL", "{} != 0uL", "0", "zero"),
    "Fixed64": Scalar("UInt64", "ULong", "0uL", "{} != 0uL", "0", "zero"),
    "Float": Scalar("Float", "Float", "0f", "{}.toRawBits() != 0", "0", "bitPattern"),
    "Double": Scalar("Double", "Double", "0.0", "{}.toRawBits() != 0L", "0", "bitPattern"),
    "Bool": Scalar("Bool", "Boolean", "false", "{}", "false", "false"),
    "String": Scalar("String", "String", '""', "{}.isNotEmpty()", "String()", "isEmpty"),
    "Bytes": Scalar("Data", "ByteArray", "ByteArray(0)", "{}.isNotEmpty()", "Data()", "isEmpty"),
}
PACKABLE = {
    "Int32", "SInt32", "SFixed32", "Int64", "SInt64", "SFixed64", "UInt32", "Fixed32", "UInt64", "Fixed64", "Float",
    "Double", "Bool", "Enum",
}


class GeneratorError(Exception):
    pass


class Source:
    def __init__(self, display, text):
        self.display = display
        self.items = []
        for number, raw in enumerate(text.split("\n"), 1):
            stripped = raw.strip()
            if not stripped or stripped.startswith("//"):
                continue
            self.items.append((number, stripped))
        self.pos = 0

    def line(self):
        if self.pos < len(self.items):
            return self.items[self.pos][0]
        return self.items[-1][0] if self.items else 1

    def error(self, message, line=None):
        return GeneratorError(f"{self.display}:{line if line is not None else self.line()}: {message}")

    def at_end(self):
        return self.pos >= len(self.items)

    def peek(self):
        return None if self.at_end() else self.items[self.pos][1]

    def match(self, pattern):
        text = self.peek()
        if text is None:
            return None
        found = re.fullmatch(pattern, text)
        if found:
            self.pos += 1
        return found

    def expect(self, pattern, what):
        found = self.match(pattern)
        if not found:
            text = self.peek()
            raise self.error(f"expected {what}, found {'end of file' if text is None else repr(text)}")
        return found

    def literal(self, text):
        return self.expect(re.escape(text), repr(text))


@dataclass
class EnumDecl:
    source: Source
    line: int
    name: str
    full: str
    parent: object
    cases: list
    default: str
    name_map_seen: bool = False


@dataclass
class OneofDecl:
    line: int
    enum_name: str
    cases: list
    prop: object = None


@dataclass
class PropDecl:
    kind: str
    name: str
    swift_type: str
    line: int
    key: str
    default: str = None
    oneof_key: str = None
    case: str = None
    has_name: str = None
    clear_name: str = None


@dataclass
class DecodeCase:
    number: int
    line: int
    function: str
    key: str = None
    oneof_key: str = None
    case: str = None
    value_type: str = None
    merges: bool = False


@dataclass
class TraverseItem:
    number: int
    line: int
    function: str
    key: str = None
    oneof_key: str = None
    case: str = None
    condition: str = None
    condition_value: str = None


@dataclass
class MessageDecl:
    source: Source
    line: int
    name: str
    full: str
    parent: object
    props: list = field(default_factory=list)
    oneofs: dict = field(default_factory=dict)
    body: list = field(default_factory=list)
    backings: dict = field(default_factory=dict)
    has_names: dict = field(default_factory=dict)
    clear_names: dict = field(default_factory=dict)
    uses_storage: bool = False
    proto_name: str = None
    name_map: list = None
    storage: dict = None
    decode: list = None
    traverse: list = None
    equality: list = None
    extension_line: int = None
    fields: list = None


@dataclass
class Field:
    number: int
    prop: PropDecl
    kind: str
    repeated: bool
    presence: bool
    type_ref: object
    oneof: PropDecl = None
    packed: bool = False


def swift_string(source, text, line):
    result = []
    index = 0
    while index < len(text):
        char = text[index]
        if char == "\\":
            following = text[index + 1:index + 2]
            if following == "0":
                result.append("\0")
                index += 2
            elif following == "u":
                found = re.match(r"\\u\{([0-9a-fA-F]{1,8})\}", text[index:])
                if not found:
                    raise source.error("malformed \\u escape in a string literal", line)
                result.append(chr(int(found.group(1), 16)))
                index += len(found.group(0))
            elif following in ("\\", '"', "'"):
                result.append(following)
                index += 2
            elif following in ("t", "n", "r"):
                result.append({"t": "\t", "n": "\n", "r": "\r"}[following])
                index += 2
            else:
                raise source.error(f"unknown escape \\{following} in a string literal", line)
        elif char == '"':
            raise source.error("unescaped quote in a string literal", line)
        else:
            result.append(char)
            index += 1
    return "".join(result)


def decode_name_map(source, text, line):
    data = swift_string(source, text, line)
    position = 0

    def read_int():
        nonlocal position
        value = 0
        shift = 0
        while True:
            if position >= len(data):
                raise source.error("truncated integer in a name map", line)
            code = ord(data[position])
            position += 1
            if code >= 0x80:
                raise source.error("integer digit out of range in a name map", line)
            value |= (code & 0x3F) << shift
            if code & 0x40 == 0:
                return value
            shift += 6

    def read_name():
        nonlocal position
        end = data.find("\0", position)
        if end < 0:
            raise source.error("unterminated name in a name map", line)
        name = data[position:end]
        position = end + 1
        return name

    if read_int() != 0:
        raise source.error("unknown name map bytecode version", line)
    number = 0
    entries = []
    while position < len(data):
        opcode = read_int()
        if opcode in (1, 3):
            number += 1
            entries.append((number, read_name()))
        elif opcode in (2, 4):
            number += read_int()
            entries.append((number, read_name()))
        elif opcode == 12:
            read_int()
            read_int()
        else:
            raise source.error(f"unknown name map opcode {opcode}", line)
    return entries


def parse_header(source):
    if source.match(r"#if canImport\(FoundationEssentials\)"):
        source.literal("import FoundationEssentials")
        source.literal("#else")
        source.literal("import Foundation")
        source.literal("#endif")
    else:
        source.match(r"import Foundation")
    source.literal("import SwiftProtobuf")
    source.expect(
        r"fileprivate " + MODIFIERS
        + r"struct _GeneratedWithProtocGenSwiftVersion: SwiftProtobuf\.ProtobufAPIVersionCheck \{",
        "the _GeneratedWithProtocGenSwiftVersion check",
    )
    source.literal("struct _2: SwiftProtobuf.ProtobufAPIVersion_2 {}")
    source.literal("typealias Version = _2")
    source.literal("}")


def parse_enum(source, name, parent, line):
    full = f"{parent.full}.{name}" if parent else name
    source.literal("typealias RawValue = Int")
    cases = []
    while True:
        found = source.match(r"case " + IDENT + r" // = (-?\d+)")
        if not found:
            break
        cases.append((found.group(1), int(found.group(2))))
    if not cases:
        raise source.error(f"enum {full} has no cases")
    source.expect(r"case UNRECOGNIZED\(Int\)", "case UNRECOGNIZED(Int) (only proto3 open enums are supported)")
    source.literal("init() {")
    default = source.expect(r"self = \." + IDENT, "the default case").group(1)
    source.literal("}")
    source.literal("init?(rawValue: Int) {")
    source.literal("switch rawValue {")
    from_raw = {}
    while True:
        found = source.match(r"case (-?\d+): self = \." + IDENT)
        if not found:
            break
        from_raw[int(found.group(1))] = found.group(2)
    source.literal("default: self = .UNRECOGNIZED(rawValue)")
    source.literal("}")
    source.literal("}")
    source.literal("var rawValue: Int {")
    source.literal("switch self {")
    to_raw = {}
    while True:
        found = source.match(r"case \." + IDENT + r": return (-?\d+)")
        if not found:
            break
        to_raw[found.group(1)] = int(found.group(2))
    source.literal("case .UNRECOGNIZED(let i): return i")
    source.literal("}")
    source.literal("}")
    source.expect(r"static let allCases: \[" + re.escape(full) + r"\] = \[", f"allCases of {full}")
    all_cases = []
    while True:
        found = source.match(r"\." + IDENT + ",")
        if not found:
            break
        all_cases.append(found.group(1))
    source.literal("]")
    source.literal("}")
    names = [name for name, _ in cases]
    if len(set(names)) != len(names) or len({raw for _, raw in cases}) != len(cases):
        raise source.error(f"enum {full} has duplicate case names or values (aliases are not supported)", line)
    if from_raw != {raw: name for name, raw in cases} or to_raw != dict(cases) or all_cases != names:
        raise source.error(f"enum {full}: init?(rawValue:), rawValue and allCases disagree with the case list", line)
    if default != names[0] or cases[0][1] != 0:
        raise source.error(f"enum {full}: the default must be the first case with raw value 0", line)
    for case_name, raw in cases:
        if case_name in RESERVED_ENUM_CASES:
            raise source.error(f"enum {full}: case name {case_name} collides with a generated Kotlin member", line)
        if raw == -1:
            raise source.error(f"enum {full}: raw value -1 is reserved for UNRECOGNIZED in Kotlin", line)
    return EnumDecl(source, line, name, full, parent, cases, default)


def parse_oneof_enum(source, name, line):
    cases = []
    while True:
        found = source.match(r"case " + IDENT + r"\(" + TYPE + r"\)")
        if not found:
            break
        cases.append((found.group(1), found.group(2)))
    source.literal("}")
    if not cases:
        raise source.error(f"oneof {name} has no cases", line)
    return OneofDecl(line, name, cases)


def parse_computed(source, message, name, swift_type, line):
    found = source.match(r"get \{(_storage\.)?_" + IDENT + r" \?\? (.+)\}")
    if found:
        backing = "_" + found.group(2)
        message.uses_storage |= bool(found.group(1))
        setter = source.expect(r"set \{(_uniqueStorage\(\)\.)?_" + IDENT + r" = newValue\}", "the setter")
        if "_" + setter.group(2) != backing or bool(setter.group(1)) != bool(found.group(1)):
            raise source.error(f"setter of {name} does not match its getter")
        source.literal("}")
        return PropDecl("presence", name, swift_type, line, backing, default=found.group(3))
    found = source.match(r"get \{_storage\._" + IDENT + r"\}")
    if found:
        message.uses_storage = True
        backing = "_" + found.group(1)
        setter = source.expect(r"set \{_uniqueStorage\(\)\._" + IDENT + r" = newValue\}", "the setter")
        if "_" + setter.group(1) != backing:
            raise source.error(f"setter of {name} does not match its getter")
        source.literal("}")
        return PropDecl("storage", name, swift_type, line, backing)
    found = source.match(r"get \{return _storage\._" + IDENT + r"\}")
    if found:
        message.uses_storage = True
        backing = "_" + found.group(1)
        setter = source.expect(r"set \{_uniqueStorage\(\)\._" + IDENT + r" = newValue\}", "the setter")
        if "_" + setter.group(1) != backing:
            raise source.error(f"setter of {name} does not match its getter")
        source.literal("}")
        return PropDecl("oneof", name, swift_type, line, backing)
    if source.match(r"get \{"):
        found = source.expect(
            r"if case \." + IDENT + r"\(let v\)\? = (_storage\._|self\.)?" + IDENT + r" \{return v\}", "a oneof case getter"
        )
        case = found.group(1)
        storage = found.group(2) == "_storage._"
        message.uses_storage |= storage
        oneof_key = ("_" if storage else "") + found.group(3)
        default = source.expect(r"return (.+)", "the default value").group(1)
        source.literal("}")
        if storage:
            setter = source.expect(r"set \{_uniqueStorage\(\)\._" + IDENT + r" = \." + IDENT + r"\(newValue\)\}", "the setter")
            setter_key = "_" + setter.group(1)
        else:
            setter = source.expect(r"set \{" + IDENT + r" = \." + IDENT + r"\(newValue\)\}", "the setter")
            setter_key = setter.group(1)
        if setter_key != oneof_key or setter.group(2) != case:
            raise source.error(f"setter of {name} does not match its getter")
        source.literal("}")
        if case != name:
            raise source.error(f"oneof case accessor {name} reads case {case}")
        return PropDecl("case", name, swift_type, line, None, default=default, oneof_key=oneof_key, case=case)
    raise source.error(f"unsupported accessor for property {name}")


def parse_struct(source, name, parent, line):
    message = MessageDecl(source, line, name, f"{parent.full}.{name}" if parent else name, parent)
    unknown_fields = False
    init_seen = False
    storage_seen = False
    while True:
        text = source.peek()
        if text is None:
            raise source.error(f"struct {message.full} is not closed")
        at = source.line()
        if source.match(r"\}"):
            break
        found = source.match(r"var " + IDENT + r": (.+?) = (.+)")
        if found:
            prop_name, swift_type, default = found.groups()
            if swift_type.endswith("?"):
                if default != "nil":
                    raise source.error(f"optional property {prop_name} must default to nil")
                message.props.append(PropDecl("oneof", prop_name, swift_type, at, prop_name))
            else:
                message.props.append(PropDecl("plain", prop_name, swift_type, at, prop_name, default=default))
            continue
        found = source.match(r"var " + IDENT + r": (.+) \{")
        if found:
            message.props.append(parse_computed(source, message, found.group(1), found.group(2), at))
            continue
        found = source.match(r"var " + IDENT + r": Bool \{(self\.|_storage\.)_" + IDENT + r" != nil\}")
        if found:
            message.has_names["_" + found.group(3)] = (found.group(1), at)
            continue
        found = source.match(r"mutating func " + IDENT + r"\(\) \{(self\.|_uniqueStorage\(\)\.)_" + IDENT + r" = nil\}")
        if found:
            message.clear_names["_" + found.group(3)] = (found.group(1), at)
            continue
        if source.match(r"var unknownFields = SwiftProtobuf\.UnknownStorage\(\)"):
            unknown_fields = True
            continue
        found = source.match(MODIFIERS + r"enum (OneOf_[A-Za-z0-9_]+): Equatable, Sendable \{")
        if found:
            oneof = parse_oneof_enum(source, found.group(1), at)
            if oneof.enum_name in message.oneofs:
                raise source.error(f"duplicate oneof {oneof.enum_name}")
            message.oneofs[oneof.enum_name] = oneof
            message.body.append(oneof)
            continue
        found = source.match(ENUM_DECLARATION)
        if found:
            message.body.append(parse_enum(source, found.group(1), message, at))
            continue
        found = source.match(STRUCT_DECLARATION)
        if found:
            message.body.append(parse_struct(source, found.group(1), message, at))
            continue
        if source.match(r"init\(\) \{\}"):
            init_seen = True
            continue
        found = source.match(r"fileprivate var _" + IDENT + r": " + TYPE + r"\? = nil")
        if found:
            message.backings["_" + found.group(1)] = (found.group(2), at)
            continue
        if source.match(r"fileprivate var _storage = _StorageClass\.defaultInstance"):
            storage_seen = True
            continue
        raise source.error(f"unsupported declaration in struct {message.full}: {text!r}")
    if not unknown_fields or not init_seen:
        raise source.error(f"struct {message.full} lacks unknownFields or init()", line)
    if storage_seen != message.uses_storage:
        raise source.error(f"struct {message.full}: _storage declaration and accessors disagree", line)
    if message.uses_storage and message.backings:
        raise source.error(f"struct {message.full} mixes _storage and fileprivate backing fields", line)
    return message


def parse_storage_class(source, message):
    variables = {}
    order = []
    while True:
        at = source.line()
        found = source.match(r"var _" + IDENT + r": (.+?)(?: = (.+))?")
        if not found:
            break
        name = "_" + found.group(1)
        swift_type, default = found.group(2), found.group(3)
        if swift_type.endswith("?") and default not in (None, "nil"):
            raise source.error(f"optional storage variable {name} must default to nil")
        if not swift_type.endswith("?") and default is None:
            raise source.error(f"storage variable {name} has no default")
        variables[name] = (swift_type, default, at)
        order.append(name)
    source.expect(r"static (?:nonisolated\(unsafe\) )?let defaultInstance = _StorageClass\(\)", "defaultInstance")
    source.literal("private init() {}")
    source.literal("init(copying source: _StorageClass) {")
    copied = []
    while True:
        found = source.match(r"_" + IDENT + r" = source\._" + IDENT)
        if not found:
            break
        if found.group(1) != found.group(2):
            raise source.error("storage copy assigns a different variable")
        copied.append("_" + found.group(1))
    source.literal("}")
    source.literal("}")
    if copied != order:
        raise source.error(f"{message.full}: init(copying:) does not copy every storage variable in order")
    source.literal("fileprivate mutating func _uniqueStorage() -> _StorageClass {")
    source.literal("if !isKnownUniquelyReferenced(&_storage) {")
    source.literal("_storage = _StorageClass(copying: _storage)")
    source.literal("}")
    source.literal("return _storage")
    source.literal("}")
    return variables


def parse_decode(source, message):
    source.literal("mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {")
    cases = []
    if source.match(r"while try decoder\.nextFieldNumber\(\) != nil \{\}"):
        source.literal("}")
        return cases
    storage = False
    if source.match(r"_ = _uniqueStorage\(\)"):
        storage = True
        source.literal("try withExtendedLifetime(_storage) { (_storage: _StorageClass) in")
    if storage != message.uses_storage:
        raise source.error(f"{message.full}: decodeMessage storage use disagrees with the struct")
    source.literal("while let fieldNumber = try decoder.nextFieldNumber() {")
    source.literal("switch fieldNumber {")
    access = r"(self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*)"
    oneof_access = r"(self\.|_storage\._)([A-Za-z_][A-Za-z0-9_]*)"
    while True:
        at = source.line()
        if source.match(r"default: break"):
            break
        found = source.match(r"case (\d+): try \{ try decoder\.decode(\w+)\(value: &" + access + r"\) \}\(\)")
        if found:
            number, function, prefix, name = int(found.group(1)), found.group(2), found.group(3), found.group(4)
            if (prefix == "_storage.") != storage or (prefix == "_storage." and not name.startswith("_")):
                raise source.error("decode target does not match the storage layout")
            cases.append(DecodeCase(number, at, function, key=name))
            continue
        found = source.match(r"case (\d+): try \{")
        if not found:
            raise source.error(f"unsupported decode case in {message.full}: {source.peek()!r}")
        number = int(found.group(1))
        value_type = source.expect(r"var v: " + TYPE + r"\?", "var v: T?").group(1)
        merges = False
        current_key = None
        current_case = None
        if source.match(r"var hadOneofValue = false"):
            merges = True
            found = source.expect(r"if let current = " + oneof_access + r" \{", "if let current = oneof {")
            current_key = ("_" if found.group(1) == "_storage._" else "") + found.group(2)
            source.literal("hadOneofValue = true")
            current_case = source.expect(r"if case \." + IDENT + r"\(let m\) = current \{v = m\}", "the current case").group(1)
            source.literal("}")
        function = source.expect(r"try decoder\.decode(\w+)\(value: &v\)", "try decoder.decodeX(value: &v)").group(1)
        source.literal("if let v = v {")
        if merges:
            source.literal("if hadOneofValue {try decoder.handleConflictingOneOf()}")
        else:
            found = source.expect(
                r"if " + oneof_access + r" != nil \{try decoder\.handleConflictingOneOf\(\)\}", "the conflicting oneof check"
            )
            current_key = ("_" if found.group(1) == "_storage._" else "") + found.group(2)
        found = source.expect(oneof_access + r" = \." + IDENT + r"\(v\)", "the oneof assignment")
        assigned_key = ("_" if found.group(1) == "_storage._" else "") + found.group(2)
        case = found.group(3)
        if assigned_key != current_key or (merges and current_case != case):
            raise source.error("oneof decode case reads and writes different oneofs or cases")
        if (found.group(1) == "_storage._") != storage:
            raise source.error("oneof decode target does not match the storage layout")
        source.literal("}")
        source.literal("}()")
        cases.append(DecodeCase(number, at, function, oneof_key=assigned_key, case=case, value_type=value_type, merges=merges))
    source.literal("}")
    source.literal("}")
    if storage:
        source.literal("}")
    source.literal("}")
    return cases


CONDITIONS = [
    (r"if (self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*) != 0 \{", "zero"),
    (r"if (self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*) != false \{", "false"),
    (r"if (self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*)\.bitPattern != 0 \{", "bitPattern"),
    (r"if !(self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*)\.isEmpty \{", "isEmpty"),
]


def parse_visit(source, value_pattern):
    found = source.expect(
        r"try visitor\.visit(\w+)\(value: " + value_pattern + r", fieldNumber: (\d+)\)", "a visitor call"
    )
    return found


def parse_traverse(source, message):
    source.literal("func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {")
    items = []
    storage = bool(source.match(r"try withExtendedLifetime\(_storage\) \{ \(_storage: _StorageClass\) in"))
    if storage != message.uses_storage:
        raise source.error(f"{message.full}: traverse storage use disagrees with the struct")
    oneof_access = r"(self\.|_storage\._)([A-Za-z_][A-Za-z0-9_]*)"
    while True:
        at = source.line()
        if storage and source.match(r"\}"):
            storage = False
            source.literal("try unknownFields.traverse(visitor: &visitor)")
            break
        if not storage and source.match(r"try unknownFields\.traverse\(visitor: &visitor\)"):
            break
        handled = False
        for pattern, condition in CONDITIONS:
            found = source.match(pattern)
            if found:
                key = found.group(2)
                visit = parse_visit(source, re.escape(found.group(1)) + re.escape(key))
                source.literal("}")
                items.append(TraverseItem(int(visit.group(2)), at, visit.group(1), key=key, condition=condition))
                handled = True
                break
        if handled:
            continue
        found = source.match(r"if (self\.|_storage\.)(_?[A-Za-z_][A-Za-z0-9_]*) != \." + IDENT + r" \{")
        if found:
            key = found.group(2)
            visit = parse_visit(source, re.escape(found.group(1)) + re.escape(key))
            source.literal("}")
            items.append(TraverseItem(int(visit.group(2)), at, visit.group(1), key=key, condition="enum",
                                      condition_value=found.group(3)))
            continue
        found = source.match(r"(try \{ )?if let v = (self\.|_storage\.)(_[A-Za-z_][A-Za-z0-9_]*) \{")
        if found:
            visit = parse_visit(source, "v")
            source.literal("} }()" if found.group(1) else "}")
            items.append(TraverseItem(int(visit.group(2)), at, visit.group(1), key=found.group(3), condition="present"))
            continue
        found = source.match(r"try \{ if case \." + IDENT + r"\(let v\)\? = " + oneof_access + r" \{")
        if found:
            visit = parse_visit(source, "v")
            source.literal("} }()")
            key = ("_" if found.group(2) == "_storage._" else "") + found.group(3)
            items.append(TraverseItem(int(visit.group(2)), at, visit.group(1), oneof_key=key, case=found.group(1),
                                      condition="case"))
            continue
        found = source.match(r"switch " + oneof_access + r" \{")
        if found:
            key = ("_" if found.group(1) == "_storage._" else "") + found.group(2)
            group = []
            while True:
                case_at = source.line()
                if source.match(r"case nil: break") or source.match(r"default: break"):
                    break
                case = source.expect(r"case \." + IDENT + r"\?: try \{", "a oneof switch case").group(1)
                guard = source.expect(
                    r"guard case \." + IDENT + r"\(let v\)\? = " + oneof_access + r" else \{ preconditionFailure\(\) \}",
                    "the guard",
                )
                guard_key = ("_" if guard.group(2) == "_storage._" else "") + guard.group(3)
                if guard.group(1) != case or guard_key != key:
                    raise source.error("oneof switch case guard does not match the case")
                visit = parse_visit(source, "v")
                source.literal("}()")
                group.append(TraverseItem(int(visit.group(2)), case_at, visit.group(1), oneof_key=key, case=case,
                                          condition="case"))
            source.literal("}")
            if not group:
                raise source.error("empty oneof switch in traverse")
            items.extend(group)
            continue
        raise source.error(f"unsupported traverse statement in {message.full}: {source.peek()!r}")
    source.literal("}")
    return items


def parse_equality(source, message):
    source.literal(f"static func ==(lhs: {message.full}, rhs: {message.full}) -> Bool {{")
    names = []
    if message.uses_storage:
        source.literal("if lhs._storage !== rhs._storage {")
        source.literal(
            "let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { "
            "(_args: (_StorageClass, _StorageClass)) in"
        )
        source.literal("let _storage = _args.0")
        source.literal("let rhs_storage = _args.1")
        while True:
            found = source.match(r"if _storage\.(_[A-Za-z_][A-Za-z0-9_]*) != rhs_storage\.(_[A-Za-z_][A-Za-z0-9_]*) \{return false\}")
            if not found:
                break
            if found.group(1) != found.group(2):
                raise source.error("equality compares different storage variables")
            names.append(found.group(1))
        source.literal("return true")
        source.literal("}")
        source.literal("if !storagesAreEqual {return false}")
        source.literal("}")
    while True:
        found = source.expect(
            r"if lhs\.(_?[A-Za-z_][A-Za-z0-9_]*) != rhs\.(_?[A-Za-z_][A-Za-z0-9_]*) \{return false\}",
            "a property comparison",
        )
        if found.group(1) != found.group(2):
            raise source.error("equality compares different properties")
        if found.group(1) == "unknownFields":
            break
        names.append(found.group(1))
    source.literal("return true")
    source.literal("}")
    return names


def parse_message_extension(source, message, package, names, line):
    message.extension_line = line
    found = source.expect(
        r"static let protoMessageName: String = (_protobuf_package|" + TYPE + r"\.protoMessageName) \+ \"([^\"]+)\"",
        "protoMessageName",
    )
    if found.group(1) == "_protobuf_package":
        if package is None:
            raise source.error("_protobuf_package is used before it is declared")
        message.proto_name = package + found.group(3)
    else:
        parent = names.get(found.group(2))
        if not isinstance(parent, MessageDecl) or parent is not message.parent or parent.proto_name is None:
            raise source.error(f"protoMessageName of {message.full} refers to {found.group(2)}")
        message.proto_name = parent.proto_name + found.group(3)
    if source.match(r"static let _protobuf_nameMap = SwiftProtobuf\._NameMap\(\)"):
        message.name_map = []
    else:
        at = source.line()
        found = source.expect(r"static let _protobuf_nameMap = SwiftProtobuf\._NameMap\(bytecode: \"(.*)\"\)", "the name map")
        message.name_map = decode_name_map(source, found.group(1), at)
    if source.match(r"fileprivate class _StorageClass \{"):
        if not message.uses_storage:
            raise source.error(f"{message.full} has a _StorageClass but no _storage")
        message.storage = parse_storage_class(source, message)
    elif message.uses_storage:
        raise source.error(f"{message.full} uses _storage but has no _StorageClass")
    message.decode = parse_decode(source, message)
    message.traverse = parse_traverse(source, message)
    message.equality = parse_equality(source, message)
    source.literal("}")


def parse_file(path, display):
    source = Source(display, path.read_text(encoding="utf-8"))
    parse_header(source)
    top = []
    extensions = []
    package = None
    names = None
    while not source.at_end():
        at = source.line()
        enum = source.match(ENUM_DECLARATION)
        struct = None if enum else source.match(STRUCT_DECLARATION)
        if enum or struct:
            if names is not None:
                raise source.error("type declaration after the SwiftProtobuf runtime section", at)
            if enum:
                top.append(parse_enum(source, enum.group(1), None, at))
            else:
                top.append(parse_struct(source, struct.group(1), None, at))
            continue
        if names is None:
            names = {}
            for declaration in top:
                collect_names(declaration, names)
        found = source.match(r"fileprivate " + MODIFIERS + r"let _protobuf_package = \"([A-Za-z0-9_.]+)\"")
        if found:
            if package is not None:
                raise source.error("_protobuf_package is declared twice")
            package = found.group(1)
            continue
        found = source.match(MODIFIERS + r"extension " + TYPE + r": SwiftProtobuf\._ProtoNameProviding \{")
        if found:
            name_at = source.line()
            bytecode = source.expect(
                r"static let _protobuf_nameMap = SwiftProtobuf\._NameMap\(bytecode: \"(.*)\"\)", "the enum name map"
            )
            source.literal("}")
            extensions.append((found.group(1), at, decode_name_map(source, bytecode.group(1), name_at)))
            continue
        found = source.match(
            MODIFIERS + r"extension " + TYPE
            + r": SwiftProtobuf\.Message, SwiftProtobuf\._MessageImplementationBase, SwiftProtobuf\._ProtoNameProviding \{"
        )
        if found:
            message = names.get(found.group(1))
            if not isinstance(message, MessageDecl) or message.decode is not None:
                raise source.error(f"message extension for unknown or already extended type {found.group(1)}")
            parse_message_extension(source, message, package, names, at)
            continue
        raise source.error(f"unsupported top-level declaration: {source.peek()!r}")
    if names is None:
        names = {}
        for declaration in top:
            collect_names(declaration, names)
    for name, at, entries in extensions:
        declaration = names.get(name)
        if not isinstance(declaration, EnumDecl) or declaration.name_map_seen:
            raise source.error(f"name map extension for unknown or already named enum {name}", at)
        declaration.name_map_seen = True
        if sorted(number for number, _ in entries) != sorted(raw for _, raw in declaration.cases):
            raise source.error(f"name map of enum {name} does not list the same values as the enum", at)
    for declaration in names.values():
        if isinstance(declaration, EnumDecl) and not declaration.name_map_seen:
            raise source.error(f"enum {declaration.full} has no name map extension", declaration.line)
        if isinstance(declaration, MessageDecl) and declaration.decode is None:
            raise source.error(f"struct {declaration.full} has no SwiftProtobuf.Message extension", declaration.line)
    return source, top, names


def collect_names(declaration, names):
    if declaration.full in names:
        raise declaration.source.error(f"duplicate type {declaration.full}", declaration.line)
    names[declaration.full] = declaration
    if isinstance(declaration, MessageDecl):
        for child in declaration.body:
            if not isinstance(child, OneofDecl):
                collect_names(child, names)


def resolve_type(swift_type, scope, symbols):
    if swift_type == TIMESTAMP:
        return "timestamp"
    if swift_type in symbols:
        return symbols[swift_type]
    current = scope
    while current is not None:
        candidate = f"{current.full}.{swift_type}"
        if candidate in symbols:
            return symbols[candidate]
        current = current.parent
    return None


DEFAULT_LITERALS = {"0", "false", "String()", "Data()"}


def scalar_kind_for(function):
    name = function
    for prefix in ("Singular", "Repeated", "Packed"):
        if name.startswith(prefix) and name.endswith("Field"):
            return prefix, name[len(prefix):-len("Field")]
    return None, None


def check_default(message, prop, kind, type_ref, default, repeated=False):
    source = message.source
    if repeated:
        if default != "[]":
            raise source.error(f"{message.full}.{prop.name}: repeated default must be []", prop.line)
        return
    if kind == "Message":
        expected = {f"{type_ref.full}()" if type_ref != "timestamp" else f"{TIMESTAMP}()"}
    elif kind == "Enum":
        expected = {"." + type_ref.default}
    else:
        expected = {SCALARS[kind].swift_default}
    if default not in expected:
        raise source.error(f"{message.full}.{prop.name}: unexpected default {default!r}", prop.line)


def resolve_message(message, symbols):
    source = message.source
    props_by_key = {}
    member_names = set()
    for prop in message.props:
        if prop.name.startswith("_"):
            raise source.error(f"property {prop.name} starts with an underscore", prop.line)
        if prop.name in RESERVED_MEMBERS:
            raise source.error(f"property {prop.name} collides with a generated Kotlin member", prop.line)
        if prop.name in member_names:
            raise source.error(f"duplicate property {prop.name}", prop.line)
        member_names.add(prop.name)
        if prop.kind == "case":
            continue
        if prop.key in props_by_key:
            raise source.error(f"two properties use {prop.key}", prop.line)
        props_by_key[prop.key] = prop
    for key, (has_name, line) in message.has_names.items():
        prop = props_by_key.get(key)
        if prop is None or prop.kind != "presence":
            raise source.error(f"{has_name} refers to {key}, which has no presence", line)
        prop.has_name = has_name
    for key, (clear_name, line) in message.clear_names.items():
        prop = props_by_key.get(key)
        if prop is None or prop.kind != "presence":
            raise source.error(f"{clear_name} refers to {key}, which has no presence", line)
        prop.clear_name = clear_name
    for prop in message.props:
        for extra in (prop.has_name, prop.clear_name):
            if extra is None:
                continue
            if extra in member_names or extra in RESERVED_MEMBERS:
                raise source.error(f"member {extra} collides with another member", prop.line)
            member_names.add(extra)
    for child in message.body:
        child_name = child.enum_name if isinstance(child, OneofDecl) else child.name
        if child_name in member_names or child_name in KOTLIN_TYPE_NAMES:
            raise source.error(f"nested type {child_name} collides with a member or a Kotlin type", child.line)
        member_names.add(child_name)
    if message.uses_storage:
        for prop in message.props:
            if prop.kind == "plain":
                raise source.error(f"{message.full}.{prop.name} is stored outside _storage", prop.line)
            if prop.kind in ("presence", "storage", "oneof"):
                variable = message.storage.get(prop.key)
                if variable is None:
                    raise source.error(f"{message.full}.{prop.name} has no storage variable", prop.line)
                storage_type, storage_default, _ = variable
                if prop.kind == "presence":
                    if storage_type != prop.swift_type + "?":
                        raise source.error(f"{message.full}.{prop.name}: storage type {storage_type} differs", prop.line)
                elif prop.kind == "storage":
                    if storage_type != prop.swift_type:
                        raise source.error(f"{message.full}.{prop.name}: storage type {storage_type} differs", prop.line)
                    prop.default = storage_default
                else:
                    if resolve_oneof_name(storage_type[:-1], message) != resolve_oneof_name(prop.swift_type[:-1], message):
                        raise source.error(f"{message.full}.{prop.name}: storage type {storage_type} differs", prop.line)
        used = {prop.key for prop in message.props if prop.kind != "case"}
        if set(message.storage) != used:
            raise source.error(f"{message.full}: storage variables and properties differ", message.extension_line)
    else:
        for key, (backing_type, line) in message.backings.items():
            prop = props_by_key.get(key)
            if prop is None or prop.kind != "presence" or prop.swift_type != backing_type:
                raise source.error(f"backing field {key} does not belong to a property", line)
        for prop in message.props:
            if prop.kind == "presence" and prop.key not in message.backings:
                raise source.error(f"{message.full}.{prop.name} has no backing field", prop.line)
    oneof_props = {}
    for prop in message.props:
        if prop.kind == "oneof":
            enum_name = resolve_oneof_name(prop.swift_type[:-1], message)
            oneof = message.oneofs.get(enum_name)
            if oneof is None or oneof.prop is not None:
                raise source.error(f"{message.full}.{prop.name} has type {prop.swift_type} without a oneof", prop.line)
            oneof.prop = prop
            oneof_props[prop.key] = prop
    for oneof in message.oneofs.values():
        if oneof.prop is None:
            raise source.error(f"oneof {oneof.enum_name} has no property", oneof.line)
    case_props = {}
    for prop in message.props:
        if prop.kind == "case":
            oneof_prop = oneof_props.get(prop.oneof_key)
            if oneof_prop is None:
                raise source.error(f"{prop.name} reads unknown oneof {prop.oneof_key}", prop.line)
            oneof = message.oneofs[resolve_oneof_name(oneof_prop.swift_type[:-1], message)]
            case_types = dict(oneof.cases)
            if prop.case not in case_types or case_types[prop.case] != prop.swift_type:
                raise source.error(f"{prop.name} does not match a case of {oneof.enum_name}", prop.line)
            if (prop.oneof_key, prop.case) in case_props:
                raise source.error(f"duplicate accessor for case {prop.case}", prop.line)
            case_props[(prop.oneof_key, prop.case)] = prop
    for oneof in message.oneofs.values():
        for case, _ in oneof.cases:
            if (oneof.prop.key, case) not in case_props:
                raise source.error(f"case {case} of {oneof.enum_name} has no accessor", oneof.line)
            if case in KOTLIN_TYPE_NAMES or case == oneof.enum_name:
                raise source.error(f"case {case} of {oneof.enum_name} collides with a Kotlin type", oneof.line)

    fields = []
    numbers = set()
    decoded_keys = set()
    for case in message.decode:
        if case.number in numbers or case.number <= 0 or case.number >= 1 << 29:
            raise source.error(f"field number {case.number} is duplicated or out of range", case.line)
        numbers.add(case.number)
        label, kind = scalar_kind_for(case.function)
        if label not in ("Singular", "Repeated") or (kind not in SCALARS and kind not in ("Enum", "Message")):
            raise source.error(f"unsupported decoder function decode{case.function}", case.line)
        if case.oneof_key is not None:
            oneof_prop = oneof_props.get(case.oneof_key)
            if oneof_prop is None or label != "Singular":
                raise source.error(f"decode case {case.number} writes unknown oneof {case.oneof_key}", case.line)
            prop = case_props.get((case.oneof_key, case.case))
            if prop is None:
                raise source.error(f"decode case {case.number} writes unknown case {case.case}", case.line)
            if (prop.oneof_key, prop.case) in decoded_keys:
                raise source.error(f"case {case.case} is decoded twice", case.line)
            decoded_keys.add((prop.oneof_key, prop.case))
            if case.value_type != prop.swift_type:
                raise source.error(f"decode case {case.number} declares v as {case.value_type}", case.line)
            if (kind == "Message") != case.merges:
                raise source.error(f"decode case {case.number}: only message cases merge", case.line)
            type_ref = check_field_type(message, prop, kind, prop.swift_type, symbols, case.line)
            check_default(message, prop, kind, type_ref, prop.default)
            fields.append(Field(case.number, prop, kind, False, False, type_ref, oneof=oneof_prop))
            continue
        prop = props_by_key.get(case.key)
        if prop is None or prop.kind in ("oneof", "case"):
            raise source.error(f"decode case {case.number} writes unknown property {case.key}", case.line)
        if prop.key in decoded_keys:
            raise source.error(f"property {prop.name} is decoded twice", case.line)
        decoded_keys.add(prop.key)
        repeated = label == "Repeated"
        swift_type = prop.swift_type
        if repeated:
            if not (swift_type.startswith("[") and swift_type.endswith("]")) or prop.kind == "presence":
                raise source.error(f"repeated field {prop.name} has type {swift_type}", case.line)
            swift_type = swift_type[1:-1]
        elif swift_type.startswith("["):
            raise source.error(f"singular field {prop.name} has type {swift_type}", case.line)
        if kind == "Message" and not repeated and prop.kind != "presence":
            raise source.error(f"singular message field {prop.name} has no presence", case.line)
        type_ref = check_field_type(message, prop, kind, swift_type, symbols, case.line)
        check_default(message, prop, kind, type_ref, prop.default, repeated)
        fields.append(Field(case.number, prop, kind, repeated, prop.kind == "presence", type_ref))
    for prop in message.props:
        if prop.kind == "oneof":
            continue
        if prop.kind == "case":
            if (prop.oneof_key, prop.case) not in decoded_keys:
                raise source.error(f"oneof case {prop.name} is never decoded", prop.line)
        elif prop.key not in decoded_keys:
            raise source.error(f"property {prop.name} is never decoded", prop.line)
    fields.sort(key=lambda item: item.number)

    if message.name_map is not None:
        mapped = sorted(number for number, _ in message.name_map)
        if mapped != sorted(numbers):
            raise source.error(f"{message.full}: name map numbers {mapped} differ from decoded numbers",
                               message.extension_line)

    by_number = {item.number: item for item in fields}
    previous = 0
    if len(message.traverse) != len(fields):
        raise source.error(f"{message.full}: traverse visits {len(message.traverse)} fields, decode reads {len(fields)}",
                           message.extension_line)
    for item in message.traverse:
        if item.number <= previous:
            raise source.error("traverse is not in field number order", item.line)
        previous = item.number
        target = by_number.get(item.number)
        if target is None:
            raise source.error(f"traverse visits field {item.number}, which is never decoded", item.line)
        check_traverse_item(message, item, target)

    equality = message.equality
    expected = [prop.key for prop in message.props if prop.kind != "case"]
    if sorted(equality) != sorted(expected) or len(set(equality)) != len(equality):
        raise source.error(f"{message.full}: == compares {equality}, expected {expected}", message.extension_line)
    message.fields = fields


def resolve_oneof_name(swift_type, message):
    if swift_type.startswith(message.full + "."):
        swift_type = swift_type[len(message.full) + 1:]
    return swift_type


def check_field_type(message, prop, kind, swift_type, symbols, line):
    source = message.source
    if kind in SCALARS:
        if swift_type != SCALARS[kind].swift:
            raise source.error(f"{prop.name}: decoder {kind} does not fit Swift type {swift_type}", line)
        return None
    type_ref = resolve_type(swift_type, message, symbols)
    if kind == "Enum" and not isinstance(type_ref, EnumDecl):
        raise source.error(f"{prop.name}: {swift_type} is not an enum", line)
    if kind == "Message" and not (isinstance(type_ref, MessageDecl) or type_ref == "timestamp"):
        raise source.error(f"{prop.name}: {swift_type} is not a supported message", line)
    return type_ref


def check_traverse_item(message, item, target):
    source = message.source
    label, kind = scalar_kind_for(item.function)
    if kind != target.kind:
        raise source.error(f"traverse visits field {item.number} as {kind}, decode reads {target.kind}", item.line)
    prop = target.prop
    if target.oneof is not None:
        if label != "Singular" or item.condition != "case" or item.oneof_key != target.oneof.key or item.case != prop.case:
            raise source.error(f"traverse of oneof field {item.number} does not match its case", item.line)
        return
    if item.key != prop.key:
        raise source.error(f"traverse of field {item.number} reads {item.key}, expected {prop.key}", item.line)
    if target.repeated:
        if label == "Singular" or item.condition != "isEmpty":
            raise source.error(f"traverse of repeated field {item.number} is not a non-empty check", item.line)
        if label == "Packed":
            if kind not in PACKABLE:
                raise source.error(f"field {item.number} of kind {kind} cannot be packed", item.line)
            target.packed = True
        return
    if label != "Singular":
        raise source.error(f"traverse visits singular field {item.number} with {item.function}", item.line)
    if target.presence:
        if item.condition != "present":
            raise source.error(f"traverse of field {item.number} does not check presence", item.line)
        return
    if kind == "Enum":
        if item.condition != "enum" or item.condition_value != target.type_ref.default:
            raise source.error(f"traverse of enum field {item.number} does not compare with the default", item.line)
        return
    if item.condition != SCALARS[kind].swift_condition:
        raise source.error(f"traverse of field {item.number} uses condition {item.condition}", item.line)


def kotlin_name(name):
    return f"`{name}`" if name in KOTLIN_KEYWORDS else name


class Output:
    def __init__(self):
        self.lines = []
        self.depth = 0

    def line(self, text=""):
        self.lines.append(("    " * self.depth + text) if text else "")

    def blank(self):
        if self.lines and self.lines[-1] != "" and not self.lines[-1].endswith("{"):
            self.lines.append("")

    def indent(self):
        self.depth += 1

    def dedent(self):
        self.depth -= 1


class Emitter:
    def __init__(self):
        self.imports = set()

    def element_type(self, target):
        if target.kind in SCALARS:
            return SCALARS[target.kind].kotlin
        if target.type_ref == "timestamp":
            self.imports.add("Google_Protobuf_Timestamp")
            return "Google_Protobuf_Timestamp"
        return target.type_ref.full

    def default_value(self, target):
        if target.kind in SCALARS:
            return SCALARS[target.kind].default
        if target.kind == "Enum":
            return f"{target.type_ref.full}.{kotlin_name(target.type_ref.default)}"
        return f"{self.element_type(target)}()"

    def oneof_class(self, message, oneof_prop):
        return f"{message.full}.{resolve_oneof_name(oneof_prop.swift_type[:-1], message)}"

    def emit_file(self, top):
        out = Output()
        for declaration in top:
            out.blank()
            if isinstance(declaration, EnumDecl):
                self.emit_enum(out, declaration)
            else:
                self.emit_message(out, declaration)
        header = [f"package {PACKAGE}", ""]
        if self.imports:
            header += [f"import {SHIM_PACKAGE}.{name}" for name in sorted(self.imports)] + [""]
        body = out.lines
        while body and body[0] == "":
            body = body[1:]
        return "\n".join(header + body) + "\n"

    def emit_enum(self, out, enum):
        out.line(f"enum class {enum.name}(val rawValue: Int) {{")
        out.indent()
        for name, raw in enum.cases:
            out.line(f"{kotlin_name(name)}({raw}),")
        out.line("UNRECOGNIZED(-1),")
        out.line(";")
        out.line()
        out.line("companion object {")
        out.indent()
        out.line(f"val allCases: List<{enum.full}> = listOf(")
        for name, _ in enum.cases:
            out.line(f"    {kotlin_name(name)},")
        out.line(")")
        out.line()
        out.line(f"fun fromRawValue(rawValue: Int): {enum.full} =")
        out.line("    when (rawValue) {")
        for name, raw in enum.cases:
            out.line(f"        {raw} -> {kotlin_name(name)}")
        out.line("        else -> UNRECOGNIZED")
        out.line("    }")
        out.dedent()
        out.line("}")
        out.dedent()
        out.line("}")

    def message_cases(self, message, oneof_prop):
        return [item for item in message.fields if item.oneof is oneof_prop and item.kind == "Message"]

    def enum_cases(self, message, oneof_prop):
        return [item for item in message.fields if item.oneof is oneof_prop and item.kind == "Enum"]

    def needs_store(self, message, oneof_prop):
        return bool(self.message_cases(message, oneof_prop) or self.enum_cases(message, oneof_prop))

    def store_oneof(self, message, oneof_prop, value):
        if self.needs_store(message, oneof_prop):
            return f"this._protobufStore_{oneof_prop.name}({value})"
        return f"this._{oneof_prop.name} = {value}"

    def emit_message(self, out, message):
        self.imports.update(["BinaryDecoder", "BinaryEncodingVisitor", "GeneratedMessage", "merge"])
        full = message.full
        by_prop = {id(item.prop): item for item in message.fields}
        out.line(f"class {message.name}() : GeneratedMessage() {{")
        out.indent()
        out.line("constructor(serializedBytes: ByteArray) : this() {")
        out.line("    merge(serializedBytes)")
        out.line("}")
        for prop in message.props:
            out.blank()
            if prop.kind == "oneof":
                self.emit_oneof_property(out, message, prop)
            else:
                self.emit_property(out, message, by_prop[id(prop)])
        for child in message.body:
            out.blank()
            if isinstance(child, OneofDecl):
                self.emit_oneof_class(out, message, child)
            elif isinstance(child, EnumDecl):
                self.emit_enum(out, child)
            else:
                self.emit_message(out, child)
        private = []
        for prop in message.props:
            if prop.kind == "oneof":
                private.append(f"private var _{prop.name}: {self.oneof_class(message, prop)}? = null")
                continue
            if prop.kind == "case":
                continue
            target = by_prop[id(prop)]
            if target.repeated:
                self.imports.add("ProtobufList")
                private.append(f"private var _{prop.name}: ProtobufList<{self.element_type(target)}> = {self.new_list(target)}")
            elif target.presence:
                private.append(f"private var _{prop.name}: {self.element_type(target)}? = null")
        if private:
            out.blank()
            for text in private:
                out.line(text)
        for prop in message.props:
            if prop.kind == "oneof" and self.needs_store(message, prop):
                self.emit_oneof_helpers(out, message, prop)
        out.blank()
        self.emit_decode(out, message)
        out.blank()
        self.emit_traverse(out, message)
        out.blank()
        self.emit_equals(out, message, by_prop)
        out.blank()
        self.emit_hash(out, message, by_prop)
        out.blank()
        self.emit_copy(out, message, by_prop)
        out.blank()
        out.line("companion object {")
        out.indent()
        out.line(f'const val protoMessageName: String = "{message.proto_name}"')
        out.line()
        out.line(f"fun with(block: {full}.() -> Unit): {full} =")
        out.line(f"    {full}().apply(block)")
        out.dedent()
        out.line("}")
        out.dedent()
        out.line("}")

    def emit_property(self, out, message, target):
        prop = target.prop
        name = kotlin_name(prop.name)
        element = self.element_type(target)
        if target.oneof is not None:
            oneof_prop = target.oneof
            case_class = f"{self.oneof_class(message, oneof_prop)}.{kotlin_name(prop.case)}"
            read = f"(this._{oneof_prop.name} as? {case_class})?.value"
            out.line(f"var {name}: {element}")
            if target.kind == "Message":
                store = self.store_oneof(message, oneof_prop, f"{case_class}(it)")
                out.line(f"    get() = {read}")
                out.line(f"        ?: _protobufPending({target.number}, {{ {element}() }}) {{ {store} }}")
            else:
                out.line(f"    get() = {read} ?: {self.default_value(target)}")
            out.line("    set(value) {")
            out.line(f"        this.{kotlin_name(oneof_prop.name)} = {case_class}(value)")
            out.line("    }")
            return
        if target.repeated:
            out.line(f"var {name}: MutableList<{element}>")
            out.line(f"    get() = this._{prop.name}")
            out.line("    set(value) {")
            out.line(f"        this._{prop.name} = {self.new_list(target)}.apply {{ addAll(value) }}")
            out.line("        _protobufMutated()")
            out.line("    }")
            return
        if target.presence:
            out.line(f"var {name}: {element}")
            if target.kind == "Message":
                out.line(f"    get() = this._{prop.name} ?: _protobufPending({target.number}, {{ {element}() }}) {{ this._{prop.name} = it }}")
                out.line("    set(value) {")
                out.line(f"        _protobufDropPending({target.number})")
                out.line(f"        this._{prop.name} = value.copy()")
            else:
                out.line(f"    get() = this._{prop.name} ?: {self.default_value(target)}")
                out.line("    set(value) {")
                out.line(f"        this._{prop.name} = value")
                if target.kind == "Enum":
                    out.line(f"        _protobufForgetUnrecognized({target.number})")
            out.line("        _protobufMutated()")
            out.line("    }")
            if prop.has_name:
                out.line()
                out.line(f"val {kotlin_name(prop.has_name)}: Boolean")
                out.line(f"    get() = this._{prop.name} != null")
            if prop.clear_name:
                out.line()
                out.line(f"fun {kotlin_name(prop.clear_name)}() {{")
                if target.kind == "Message":
                    out.line(f"    _protobufDropPending({target.number})")
                out.line(f"    this._{prop.name} = null")
                if target.kind == "Enum":
                    out.line(f"    _protobufForgetUnrecognized({target.number})")
                out.line("    _protobufMutated()")
                out.line("}")
            return
        out.line(f"var {name}: {element} = {self.default_value(target)}")
        out.line("    set(value) {")
        out.line("        field = value")
        if target.kind == "Enum":
            out.line(f"        _protobufForgetUnrecognized({target.number})")
        out.line("        _protobufMutated()")
        out.line("    }")

    def new_list(self, target):
        element = self.element_type(target)
        if target.kind == "Message":
            return f"ProtobufList<{element}>(this) {{ it.copy() }}"
        if target.kind == "Enum":
            return f"ProtobufList<{element}>(this, {element}.UNRECOGNIZED, {target.number}) {{ it }}"
        return f"ProtobufList<{element}>(this) {{ it }}"

    def emit_oneof_property(self, out, message, prop):
        oneof_class = self.oneof_class(message, prop)
        out.line(f"var {kotlin_name(prop.name)}: {oneof_class}?")
        out.line(f"    get() = this._{prop.name}")
        out.line("    set(value) {")
        if self.message_cases(message, prop):
            out.line(f"        this._protobufStore_{prop.name}(this._protobufCopy_{prop.name}(value))")
        elif self.needs_store(message, prop):
            out.line(f"        this._protobufStore_{prop.name}(value)")
        else:
            out.line(f"        this._{prop.name} = value")
        out.line("        _protobufMutated()")
        out.line("    }")

    def emit_oneof_class(self, out, message, oneof):
        self.imports.add("ProtobufOneofCase")
        targets = {item.prop.case: item for item in message.fields if item.oneof is oneof.prop}
        out.line(f"sealed class {oneof.enum_name}(value: Any) : ProtobufOneofCase(value) {{")
        out.indent()
        for case, _ in oneof.cases:
            element = self.element_type(targets[case])
            out.line(f"class {kotlin_name(case)}(val value: {element}) : {oneof.enum_name}(value)")
        out.dedent()
        out.line("}")

    def emit_oneof_helpers(self, out, message, prop):
        oneof_class = self.oneof_class(message, prop)
        cases = self.message_cases(message, prop)
        enums = self.enum_cases(message, prop)
        out.blank()
        out.line(f"private fun _protobufStore_{prop.name}(value: {oneof_class}?) {{")
        for item in cases:
            out.line(f"    _protobufDropPending({item.number})")
        if enums:
            out.line(f"    _protobufForgetUnrecognized({', '.join(str(item.number) for item in enums)})")
        out.line(f"    this._{prop.name} = value")
        out.line("}")
        if not cases:
            return
        out.line()
        out.line(f"private fun _protobufCopy_{prop.name}(value: {oneof_class}?): {oneof_class}? =")
        out.line("    when (value) {")
        for item in cases:
            case_class = f"{oneof_class}.{kotlin_name(item.prop.case)}"
            out.line(f"        is {case_class} -> {case_class}(value.value.copy())")
        out.line("        else -> value")
        out.line("    }")

    def enum_decoder(self, target):
        enum = target.type_ref.full
        return f"{enum}.UNRECOGNIZED) {{ {enum}.fromRawValue(it) }}"

    def emit_decode(self, out, message):
        out.line("override fun decodeMessage(decoder: BinaryDecoder) {")
        out.indent()
        if not message.fields:
            out.line("while (decoder.nextFieldNumber() != null) {")
            out.line("}")
            out.dedent()
            out.line("}")
            return
        out.line("while (true) {")
        out.line("    when (decoder.nextFieldNumber() ?: return) {")
        out.depth += 2
        for target in message.fields:
            number = target.number
            prop = target.prop
            element = self.element_type(target)
            if target.oneof is not None:
                case_class = f"{self.oneof_class(message, target.oneof)}.{kotlin_name(prop.case)}"
                store = self.store_oneof(message, target.oneof, f"{case_class}(it)")
                others = [item.number for item in self.enum_cases(message, target.oneof) if item is not target]
                if others:
                    store = f"decoder.forgetUnrecognized({', '.join(str(number) for number in others)}); {store}"
                if target.kind == "Message":
                    current = f"(this._{target.oneof.name} as? {case_class})?.value"
                    out.line(f"{number} -> decoder.decodeSingularMessageField({current}) {{ {element}() }}")
                    out.line(f"    ?.let {{ {store} }}")
                elif target.kind == "Enum":
                    out.line(f"{number} -> decoder.decodeSingularOpenEnumField({self.enum_decoder(target)}")
                    out.line(f"    ?.let {{ {store} }}")
                else:
                    out.line(f"{number} -> decoder.decodeSingular{target.kind}Field()?.let {{ {store} }}")
                continue
            if target.repeated:
                if target.kind == "Message":
                    out.line(f"{number} -> decoder.decodeRepeatedMessageField(this._{prop.name}) {{ {element}() }}")
                elif target.kind == "Enum":
                    out.line(f"{number} -> decoder.decodeRepeatedOpenEnumField(this._{prop.name}, {self.enum_decoder(target)}")
                else:
                    out.line(f"{number} -> decoder.decodeRepeated{target.kind}Field(this._{prop.name})")
                continue
            destination = f"this._{prop.name}" if target.presence and target.kind != "Enum" else f"this.{kotlin_name(prop.name)}"
            if target.kind == "Message":
                out.line(f"{number} -> decoder.decodeSingularMessageField(this._{prop.name}) {{ {element}() }}?.let {{")
                out.line(f"    _protobufDropPending({number})")
                out.line(f"    this._{prop.name} = it")
                out.line("}")
            elif target.kind == "Enum":
                out.line(f"{number} -> decoder.decodeSingularOpenEnumField({self.enum_decoder(target)}")
                out.line(f"    ?.let {{ {destination} = it }}")
            else:
                out.line(f"{number} -> decoder.decodeSingular{target.kind}Field()?.let {{ {destination} = it }}")
        out.depth -= 2
        out.line("    }")
        out.line("}")
        out.dedent()
        out.line("}")

    def emit_traverse(self, out, message):
        out.line("override fun traverse(visitor: BinaryEncodingVisitor) {")
        out.indent()
        for target in message.fields:
            number = target.number
            prop = target.prop
            if target.oneof is not None:
                case_class = f"{self.oneof_class(message, target.oneof)}.{kotlin_name(prop.case)}"
                out.line(f"(this._{target.oneof.name} as? {case_class})?.let {{")
                self.emit_visit(out, target, "it.value", number)
                out.line("}")
                continue
            if target.repeated:
                out.line(f"if (this._{prop.name}.isNotEmpty()) {{")
                out.indent()
                mode = "Packed" if target.packed else "Repeated"
                if target.kind == "Enum":
                    unrecognized = f"{target.type_ref.full}.UNRECOGNIZED"
                    out.line(f"visitor.visit{mode}EnumField(this._{prop.name}.filter {{ it != {unrecognized} }}.map {{ it.rawValue }}, {number})")
                else:
                    out.line(f"visitor.visit{mode}{target.kind}Field(this._{prop.name}, {number})")
                out.dedent()
                out.line("}")
                continue
            if target.presence:
                out.line(f"this._{prop.name}?.let {{")
                self.emit_visit(out, target, "it", number)
                out.line("}")
                continue
            value = f"this.{kotlin_name(prop.name)}"
            if target.kind == "Enum":
                enum = target.type_ref.full
                condition = f"{value} != {enum}.{kotlin_name(target.type_ref.default)} && {value} != {enum}.UNRECOGNIZED"
                out.line(f"if ({condition}) {{")
                out.line(f"    visitor.visitSingularEnumField({value}.rawValue, {number})")
            else:
                out.line(f"if ({SCALARS[target.kind].nonzero.format(value)}) {{")
                out.line(f"    visitor.visitSingular{target.kind}Field({value}, {number})")
            out.line("}")
        out.line("visitor.visitUnknown(unknownFields)")
        out.dedent()
        out.line("}")

    def emit_visit(self, out, target, value, number):
        out.indent()
        if target.kind == "Enum":
            out.line(f"if ({value} != {target.type_ref.full}.UNRECOGNIZED) {{")
            out.line(f"    visitor.visitSingularEnumField({value}.rawValue, {number})")
            out.line("}")
        else:
            out.line(f"visitor.visitSingular{target.kind}Field({value}, {number})")
        out.dedent()

    def stored(self, message, key, by_prop):
        for prop in message.props:
            if prop.key == key and prop.kind != "case":
                if prop.kind == "oneof":
                    return prop, None
                return prop, by_prop[id(prop)]
        raise message.source.error(f"unknown key {key}", message.extension_line)

    def emit_equals(self, out, message, by_prop):
        out.line("override fun equals(other: Any?): Boolean {")
        out.indent()
        out.line("if (this === other) return true")
        out.line(f"if (other !is {message.full}) return false")
        for key in message.equality:
            prop, target = self.stored(message, key, by_prop)
            if target is None or target.repeated or target.presence and target.kind != "Bytes":
                left = f"this._{prop.name}"
                out.line(f"if ({left} != other._{prop.name}) return false")
            elif target.presence:
                out.line(f"if (!this._{prop.name}.contentEquals(other._{prop.name})) return false")
            elif target.kind == "Bytes":
                name = kotlin_name(prop.name)
                out.line(f"if (!this.{name}.contentEquals(other.{name})) return false")
            else:
                name = kotlin_name(prop.name)
                out.line(f"if (this.{name} != other.{name}) return false")
        out.line("return this.unknownFields.contentEquals(other.unknownFields)")
        out.dedent()
        out.line("}")

    def emit_hash(self, out, message, by_prop):
        out.line("override fun hashCode(): Int {")
        out.indent()
        out.line("var hash = 0")
        for key in message.equality:
            prop, target = self.stored(message, key, by_prop)
            if target is None:
                expression = f"(this._{prop.name}?.hashCode() ?: 0)"
            elif target.repeated:
                expression = f"this._{prop.name}.hashCode()"
            else:
                value = f"this._{prop.name}" if target.presence else f"this.{kotlin_name(prop.name)}"
                if target.kind in ("Float", "Double"):
                    self.imports.add("protobufHash")
                    expression = f"protobufHash({value})"
                elif target.kind == "Bytes":
                    expression = f"{value}.contentHashCode()"
                elif target.presence:
                    expression = f"({value}?.hashCode() ?: 0)"
                else:
                    expression = f"{value}.hashCode()"
            out.line(f"hash = 31 * hash + {expression}")
        out.line("hash = 31 * hash + this.unknownFields.contentHashCode()")
        out.line("return hash")
        out.dedent()
        out.line("}")

    def emit_copy(self, out, message, by_prop):
        out.line(f"override fun copy(): {message.full} {{")
        out.indent()
        out.line(f"val result = {message.full}()")
        for key in message.equality:
            prop, target = self.stored(message, key, by_prop)
            if target is None:
                if self.message_cases(message, prop):
                    out.line(f"result._{prop.name} = this._protobufCopy_{prop.name}(this._{prop.name})")
                else:
                    out.line(f"result._{prop.name} = this._{prop.name}")
            elif target.repeated:
                out.line(f"result._{prop.name}.addAll(this._{prop.name})")
            elif target.presence:
                suffix = "?.copy()" if target.kind == "Message" else ""
                out.line(f"result._{prop.name} = this._{prop.name}{suffix}")
            else:
                name = kotlin_name(prop.name)
                out.line(f"result.{name} = this.{name}")
        out.line("result.unknownFields = this.unknownFields")
        out.line("return result")
        out.dedent()
        out.line("}")


def jvm_names(declaration, prefix, names, source):
    if isinstance(declaration, OneofDecl):
        own = f"{prefix}${declaration.enum_name}"
        children = [f"{own}${case}" for case, _ in declaration.cases]
    else:
        own = f"{prefix}${declaration.name}" if prefix else declaration.name
        children = []
    for name in [own] + children:
        lowered = name.lower()
        if lowered in names:
            raise source.error(f"classes {names[lowered]} and {name} differ only in case, which breaks on "
                               "case-insensitive file systems", getattr(declaration, "line", None))
        names[lowered] = name
    if not isinstance(declaration, OneofDecl):
        names[f"{own}$Companion".lower()] = f"{own}$Companion"
    if isinstance(declaration, MessageDecl):
        for child in declaration.body:
            jvm_names(child, own, names, source)


def default_upstream():
    candidates = [ROOT / ".upstream"]
    try:
        common = subprocess.run(
            ["git", "-C", str(ROOT), "rev-parse", "--git-common-dir"], capture_output=True, text=True, check=True,
        ).stdout.strip()
        common_path = Path(common)
        if not common_path.is_absolute():
            common_path = (ROOT / common_path).resolve()
        candidates.append(common_path.parent / ".upstream")
    except (OSError, subprocess.CalledProcessError):
        pass
    for candidate in candidates:
        if (candidate / SWIFT_DIR).is_dir():
            return candidate
    return candidates[0]


def generate(moblin):
    swift_dir = moblin / SWIFT_DIR
    if not swift_dir.is_dir():
        raise GeneratorError(f"{swift_dir.as_posix()}: directory not found (pass --moblin <upstream checkout>)")
    inputs = []
    for path in sorted(swift_dir.iterdir(), key=lambda item: item.name):
        if path.name == "README-TESLA.md":
            continue
        if not path.name.endswith(".pb.swift") or not path.is_file():
            raise GeneratorError(f"{SWIFT_DIR}/{path.name}: unexpected file, only *.pb.swift files are translated")
        inputs.append(path)
    if not inputs:
        raise GeneratorError(f"{SWIFT_DIR}: no *.pb.swift files")
    parsed = []
    symbols = {}
    for path in inputs:
        source, top, names = parse_file(path, f"{SWIFT_DIR}/{path.name}")
        for name, declaration in names.items():
            if name in symbols:
                raise source.error(f"type {name} is also declared in {symbols[name].source.display}", declaration.line)
            symbols[name] = declaration
        parsed.append((path, source, top, names))
    class_names = {}
    for path, source, top, names in parsed:
        for declaration in top:
            jvm_names(declaration, "", class_names, source)
        for declaration in names.values():
            if isinstance(declaration, MessageDecl):
                resolve_message(declaration, symbols)
    outputs = {}
    for path, source, top, names in parsed:
        emitter = Emitter()
        outputs[path.name[:-len(".swift")] + ".kt"] = emitter.emit_file(top)
    counts = {
        "files": len(parsed),
        "messages": sum(isinstance(item, MessageDecl) for item in symbols.values()),
        "enums": sum(isinstance(item, EnumDecl) for item in symbols.values()),
    }
    return outputs, counts


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Translate the SwiftProtobuf-generated Tesla protobufs in the upstream checkout to Kotlin "
        f"({OUTPUT_DIR.relative_to(ROOT).as_posix()}). Fails on any Swift construct it does not understand."
    )
    parser.add_argument("--moblin", type=Path, default=None, help="upstream checkout (default: .upstream)")
    parser.add_argument("--output", type=Path, default=OUTPUT_DIR)
    parser.add_argument("--check", action="store_true",
                        help="write nothing and exit 1 when the Kotlin files differ from what would be generated")
    args = parser.parse_args()
    moblin = (args.moblin or default_upstream()).resolve()
    try:
        outputs, counts = generate(moblin)
    except GeneratorError as error:
        print(f"pbswift: error: {error}", file=sys.stderr)
        sys.exit(1)
    output = args.output.resolve()
    existing = {path.name: path for path in output.glob("*.kt")} if output.is_dir() else {}
    changed = [name for name, text in sorted(outputs.items())
               if name not in existing or existing[name].read_bytes().replace(b"\r\n", b"\n") != text.encode("utf-8")]
    stale = sorted(set(existing) - set(outputs))
    summary = f"{counts['files']} files, {counts['messages']} messages, {counts['enums']} enums"
    if args.check:
        for name in changed:
            print(f"pbswift: {name} differs from the generator output")
        for name in stale:
            print(f"pbswift: {name} is not generated from any Swift file")
        if changed or stale:
            print("pbswift: run python tools/pbswift.py to regenerate")
            sys.exit(1)
        print(f"pbswift: up to date ({summary})")
        return
    output.mkdir(parents=True, exist_ok=True)
    for name in changed:
        (output / name).write_bytes(outputs[name].encode("utf-8"))
    for name in stale:
        existing[name].unlink()
    print(f"pbswift: {summary}, {len(changed)} written, {len(stale)} removed")


if __name__ == "__main__":
    main()
