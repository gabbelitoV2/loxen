package com.moblin.android.platform.live2d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal class Moc3Exception(message: String) : Exception(message)

internal enum class Moc3Pass {
    Base,
    V3_0,
    V3_3,
    V4_0,
    V4_2A,
    V4_2B,
    V4_2,
    V5_0,
    V5_3A,
    V5_3,
    Internal,
}

internal enum class Moc3Version(val raw: Int, val pass: Moc3Pass, val label: String) {
    V3_0(1, Moc3Pass.V3_0, "3.0"),
    V3_3(2, Moc3Pass.V3_3, "3.3"),
    V4_0(3, Moc3Pass.V4_0, "4.0"),
    V4_2(4, Moc3Pass.V4_2, "4.2"),
    V5_0(5, Moc3Pass.V5_0, "5.0"),
    V5_3(6, Moc3Pass.V5_3, "5.3"),
}

private enum class FieldKind {
    U32_PAIR,
    IDENTIFIER,
    BOOL32,
    U32,
    F32,
    U8,
    U16,
    REF,
    OPT_REF,
    ARRAY_REF,
    DEFORMER_TYPE,
    SNAP_TYPE,
    DRAW_ITEM_TYPE,
    BLEND_CONFIG,
}

private class FieldSpec(val pass: Moc3Pass, val name: String, val kind: FieldKind, val target: String? = null)

private class PrimitiveSpec(val isU16: Boolean, val stride: Int)

private class ClassSpec(
    val name: String,
    val groupPass: Moc3Pass,
    val fields: List<FieldSpec> = emptyList(),
    val primitive: PrimitiveSpec? = null,
)

private fun f(pass: Moc3Pass, name: String, kind: FieldKind, target: String? = null): FieldSpec {
    return FieldSpec(pass, name, kind, target)
}

private val classSpecs: List<ClassSpec> = run {
    val base = Moc3Pass.Base
    val v30 = Moc3Pass.V3_0
    val v33 = Moc3Pass.V3_3
    val v42a = Moc3Pass.V4_2A
    val v42b = Moc3Pass.V4_2B
    val v42 = Moc3Pass.V4_2
    val v50 = Moc3Pass.V5_0
    val v53a = Moc3Pass.V5_3A
    val v53 = Moc3Pass.V5_3
    listOf(
        ClassSpec(
            "Part",
            base,
            listOf(
                f(base, "hdr", FieldKind.U32_PAIR),
                f(base, "id", FieldKind.IDENTIFIER),
                f(base, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(base, "forms", FieldKind.ARRAY_REF, "PartForm"),
                f(base, "visible_artmeshes", FieldKind.BOOL32),
                f(base, "visible_deformers", FieldKind.BOOL32),
                f(base, "parent", FieldKind.OPT_REF, "Part"),
                f(v53a, "offscreen_part", FieldKind.OPT_REF, "OffscreenPart"),
            ),
        ),
        ClassSpec(
            "Deformer",
            base,
            listOf(
                f(base, "hdr", FieldKind.U32_PAIR),
                f(base, "id", FieldKind.IDENTIFIER),
                f(base, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(base, "unk_flag1", FieldKind.BOOL32),
                f(base, "visible", FieldKind.BOOL32),
                f(base, "part", FieldKind.OPT_REF, "Part"),
                f(base, "parent", FieldKind.OPT_REF, "Deformer"),
                f(base, "deformer_type", FieldKind.DEFORMER_TYPE),
                f(base, "i_typed", FieldKind.U32),
            ),
        ),
        ClassSpec(
            "WarpDeformer",
            base,
            listOf(
                f(base, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(base, "forms", FieldKind.ARRAY_REF, "WarpForm"),
                f(base, "vertex_count", FieldKind.U32),
                f(base, "y_divs", FieldKind.U32),
                f(base, "x_divs", FieldKind.U32),
                f(v33, "bilinear_interpolation", FieldKind.BOOL32),
                f(v42b, "i_color_forms", FieldKind.U32),
            ),
        ),
        ClassSpec(
            "RotDeformer",
            base,
            listOf(
                f(base, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(base, "forms", FieldKind.ARRAY_REF, "RotForm"),
                f(base, "angle_offset", FieldKind.F32),
                f(v42b, "i_color_forms", FieldKind.U32),
            ),
        ),
        ClassSpec(
            "ArtMesh",
            base,
            listOf(
                f(base, "hdr", FieldKind.U32_PAIR),
                f(base, "unk_a", FieldKind.U32_PAIR),
                f(base, "unk_b", FieldKind.U32_PAIR),
                f(base, "unk_c", FieldKind.U32_PAIR),
                f(base, "id", FieldKind.IDENTIFIER),
                f(base, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(base, "forms", FieldKind.ARRAY_REF, "ArtMeshForm"),
                f(base, "unk_flag1", FieldKind.BOOL32),
                f(base, "visible", FieldKind.BOOL32),
                f(base, "part", FieldKind.OPT_REF, "Part"),
                f(base, "deformer", FieldKind.OPT_REF, "Deformer"),
                f(base, "texture", FieldKind.U32),
                f(base, "render_config", FieldKind.U8),
                f(base, "vertex_count", FieldKind.U32),
                f(base, "texcoord_start", FieldKind.REF, "TexCoord"),
                f(base, "indices", FieldKind.ARRAY_REF, "VertexIndex"),
                f(base, "clips", FieldKind.ARRAY_REF, "ArtMeshRef"),
                f(v42b, "i_color_forms", FieldKind.U32),
                f(v53a, "blend_config", FieldKind.BLEND_CONFIG),
            ),
        ),
        ClassSpec(
            "Param",
            base,
            listOf(
                f(base, "hdr", FieldKind.U32_PAIR),
                f(base, "id", FieldKind.IDENTIFIER),
                f(base, "max", FieldKind.F32),
                f(base, "min", FieldKind.F32),
                f(base, "default", FieldKind.F32),
                f(base, "repeat", FieldKind.BOOL32),
                f(base, "snap_type", FieldKind.SNAP_TYPE),
                f(base, "maps", FieldKind.ARRAY_REF, "ParamMap"),
                f(v42a, "unk_zero_2", FieldKind.U32_PAIR),
                f(v42a, "keypoints", FieldKind.ARRAY_REF, "Keypoint"),
                f(v42, "blendshape", FieldKind.BOOL32),
                f(v42, "blend_maps", FieldKind.ARRAY_REF, "BlendParamMap"),
            ),
        ),
        ClassSpec(
            "PartForm",
            base,
            listOf(
                f(base, "depth", FieldKind.F32),
                f(v53, "offscreen", FieldKind.OPT_REF, "OffscreenPartForm"),
            ),
        ),
        ClassSpec(
            "WarpForm",
            base,
            listOf(
                f(base, "opacity", FieldKind.F32),
                f(base, "start_vertex", FieldKind.REF, "VertexCoord"),
                f(v50, "multiply_color", FieldKind.REF, "MultiplyColor"),
                f(v50, "screen_color", FieldKind.REF, "ScreenColor"),
            ),
        ),
        ClassSpec(
            "RotForm",
            base,
            listOf(
                f(base, "opacity", FieldKind.F32),
                f(base, "angle", FieldKind.F32),
                f(base, "pos_x", FieldKind.F32),
                f(base, "pos_y", FieldKind.F32),
                f(base, "scale", FieldKind.F32),
                f(base, "flip_x", FieldKind.BOOL32),
                f(base, "flip_y", FieldKind.BOOL32),
                f(v50, "multiply_color", FieldKind.REF, "MultiplyColor"),
                f(v50, "screen_color", FieldKind.REF, "ScreenColor"),
            ),
        ),
        ClassSpec(
            "ArtMeshForm",
            base,
            listOf(
                f(base, "opacity", FieldKind.F32),
                f(base, "depth", FieldKind.F32),
                f(base, "start_vertex", FieldKind.REF, "VertexCoord"),
                f(v50, "multiply_color", FieldKind.REF, "MultiplyColor"),
                f(v50, "screen_color", FieldKind.REF, "ScreenColor"),
            ),
        ),
        ClassSpec("VertexCoord", base, primitive = PrimitiveSpec(isU16 = false, stride = 2)),
        ClassSpec("ParamMapRef", base, listOf(f(base, "map", FieldKind.REF, "ParamMap"))),
        ClassSpec("ParamMapSet", base, listOf(f(base, "refs", FieldKind.ARRAY_REF, "ParamMapRef"))),
        ClassSpec("ParamMap", base, listOf(f(base, "keypoints", FieldKind.ARRAY_REF, "Keypoint"))),
        ClassSpec("Keypoint", base, primitive = PrimitiveSpec(isU16 = false, stride = 1)),
        ClassSpec("TexCoord", base, primitive = PrimitiveSpec(isU16 = false, stride = 2)),
        ClassSpec("VertexIndex", base, primitive = PrimitiveSpec(isU16 = true, stride = 1)),
        ClassSpec("ArtMeshRef", base, listOf(f(base, "artmesh", FieldKind.OPT_REF, "ArtMesh"))),
        ClassSpec(
            "DrawGroup",
            base,
            listOf(
                f(base, "items", FieldKind.ARRAY_REF, "DrawItem"),
                f(base, "total_artmesh_count", FieldKind.U32),
                f(base, "max_depth", FieldKind.F32),
                f(base, "min_depth", FieldKind.F32),
            ),
        ),
        ClassSpec(
            "DrawItem",
            base,
            listOf(
                f(base, "item_type", FieldKind.DRAW_ITEM_TYPE),
                f(base, "i_child", FieldKind.U32),
                f(base, "draw_group", FieldKind.OPT_REF, "DrawGroup"),
            ),
        ),
        ClassSpec(
            "Glue",
            v30,
            listOf(
                f(v30, "hdr", FieldKind.U32_PAIR),
                f(v30, "id", FieldKind.IDENTIFIER),
                f(v30, "param_map_set", FieldKind.REF, "ParamMapSet"),
                f(v30, "forms", FieldKind.ARRAY_REF, "GlueForm"),
                f(v30, "artmesh_1", FieldKind.REF, "ArtMesh"),
                f(v30, "artmesh_2", FieldKind.REF, "ArtMesh"),
                f(v30, "coords", FieldKind.ARRAY_REF, "GlueCoord"),
            ),
        ),
        ClassSpec(
            "GlueCoord",
            v30,
            listOf(f(v30, "weight", FieldKind.F32), f(v30, "vertex_index", FieldKind.U16)),
        ),
        ClassSpec("GlueForm", v30, listOf(f(v30, "compatibility", FieldKind.F32))),
        ClassSpec(
            "MultiplyColor",
            v42b,
            listOf(f(v42b, "r", FieldKind.F32), f(v42b, "g", FieldKind.F32), f(v42b, "b", FieldKind.F32)),
        ),
        ClassSpec(
            "ScreenColor",
            v42b,
            listOf(f(v42b, "r", FieldKind.F32), f(v42b, "g", FieldKind.F32), f(v42b, "b", FieldKind.F32)),
        ),
        ClassSpec(
            "BlendParamMap",
            v42,
            listOf(f(v42, "keypoints", FieldKind.ARRAY_REF, "Keypoint"), f(v42, "neutral_index", FieldKind.U32)),
        ),
        ClassSpec(
            "BlendFormMap",
            v42,
            listOf(
                f(v42, "param_map", FieldKind.REF, "BlendParamMap"),
                f(v42, "i_forms", FieldKind.U32),
                f(v42, "cnt_forms", FieldKind.U32),
                f(v42, "blendweight_limits", FieldKind.ARRAY_REF, "BlendWeightLimitRef"),
            ),
        ),
        ClassSpec(
            "WarpBlendFormMaps",
            v42,
            listOf(f(v42, "warp", FieldKind.REF, "WarpDeformer"), f(v42, "maps", FieldKind.ARRAY_REF, "BlendFormMap")),
        ),
        ClassSpec(
            "ArtMeshBlendFormMaps",
            v42,
            listOf(f(v42, "artmesh", FieldKind.REF, "ArtMesh"), f(v42, "maps", FieldKind.ARRAY_REF, "BlendFormMap")),
        ),
        ClassSpec("BlendWeightLimitRef", v42, listOf(f(v42, "limit", FieldKind.REF, "BlendWeightLimit"))),
        ClassSpec(
            "BlendWeightLimit",
            v42,
            listOf(
                f(v42, "param", FieldKind.REF, "Param"),
                f(v42, "points", FieldKind.ARRAY_REF, "BlendWeightLimitPoint"),
            ),
        ),
        ClassSpec(
            "BlendWeightLimitPoint",
            v42,
            listOf(f(v42, "value", FieldKind.F32), f(v42, "weight", FieldKind.F32)),
        ),
        ClassSpec(
            "PartBlendFormMaps",
            v50,
            listOf(f(v50, "part", FieldKind.REF, "Part"), f(v50, "maps", FieldKind.ARRAY_REF, "BlendFormMap")),
        ),
        ClassSpec(
            "RotBlendFormMaps",
            v50,
            listOf(f(v50, "rot", FieldKind.REF, "RotDeformer"), f(v50, "maps", FieldKind.ARRAY_REF, "BlendFormMap")),
        ),
        ClassSpec(
            "GlueBlendFormMaps",
            v50,
            listOf(f(v50, "glue", FieldKind.REF, "Glue"), f(v50, "maps", FieldKind.ARRAY_REF, "BlendFormMap")),
        ),
        ClassSpec(
            "OffscreenPart",
            v53a,
            listOf(
                f(v53a, "unk_zeros", FieldKind.U32),
                f(v53a, "part", FieldKind.REF, "Part"),
                f(v53a, "render_config", FieldKind.U8),
                f(v53a, "blend_config", FieldKind.BLEND_CONFIG),
                f(v53a, "clips", FieldKind.ARRAY_REF, "ArtMeshRef"),
            ),
        ),
        ClassSpec(
            "OffscreenPartForm",
            v53,
            listOf(
                f(v53, "opacity", FieldKind.F32),
                f(v53, "multiply_color", FieldKind.REF, "MultiplyColor"),
                f(v53, "screen_color", FieldKind.REF, "ScreenColor"),
            ),
        ),
        ClassSpec(
            "OffscreenPartBlendFormMaps",
            v53,
            listOf(
                f(v53, "offscreen_part", FieldKind.REF, "OffscreenPart"),
                f(v53, "maps", FieldKind.ARRAY_REF, "BlendFormMap"),
            ),
        ),
    )
}

internal fun moc3ClassCount(version: Moc3Version): Int {
    return classSpecs.count { it.groupPass <= version.pass }
}

private fun fieldSectionCount(field: FieldSpec): Int {
    return if (field.kind == FieldKind.ARRAY_REF) 2 else 1
}

internal fun moc3SectionCount(version: Moc3Version): Int {
    var count = 2
    for (pass in Moc3Pass.entries) {
        if (pass > version.pass) {
            break
        }
        for (spec in classSpecs) {
            if (spec.groupPass > pass) {
                continue
            }
            if (spec.primitive != null) {
                if (spec.groupPass == pass) {
                    count += 1
                }
            } else {
                for (field in spec.fields) {
                    if (field.pass == pass) {
                        count += fieldSectionCount(field)
                    }
                }
            }
        }
    }
    return count
}

internal class Moc3Table(val name: String) {
    var rawCount = 0
    val ints = HashMap<String, IntArray>()
    val floats = HashMap<String, FloatArray>()
    val strings = HashMap<String, Array<String>>()
    var primitiveFloats = FloatArray(0)
    var primitiveShorts = ShortArray(0)

    fun int(field: String): IntArray {
        return ints[field] ?: IntArray(0)
    }

    fun float(field: String): FloatArray {
        return floats[field] ?: FloatArray(0)
    }
}

internal class Moc3Canvas(
    val scale: Float,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
)

private class SectionReader(private val data: ByteArray) {
    var p = 0
    var section = 0
    var offsets = LongArray(0)

    fun readExact(count: Int): ByteArray {
        if (count < 0 || count > data.size - p) {
            throw Moc3Exception("I/O error: failed to fill whole buffer")
        }
        val result = data.copyOfRange(p, p + count)
        p += count
        return result
    }

    private fun buffer(count: Int): ByteBuffer {
        if (count < 0 || count > data.size - p) {
            throw Moc3Exception("I/O error: failed to fill whole buffer")
        }
        val buffer = ByteBuffer.wrap(data, p, count).order(ByteOrder.LITTLE_ENDIAN)
        p += count
        return buffer
    }

    fun readU32(): Long {
        return buffer(4).int.toLong() and 0xffffffffL
    }

    fun readF32(): Float {
        return buffer(4).float
    }

    fun readU32Array(count: Int): IntArray {
        val buffer = buffer(checkedSize(count, 4))
        return IntArray(count) { buffer.int }
    }

    fun readF32Array(count: Int): FloatArray {
        val buffer = buffer(checkedSize(count, 4))
        return FloatArray(count) { buffer.float }
    }

    fun readU16Array(count: Int): ShortArray {
        val buffer = buffer(checkedSize(count, 2))
        return ShortArray(count) { buffer.short }
    }

    fun readU8Array(count: Int): IntArray {
        val bytes = readExact(checkedSize(count, 1))
        return IntArray(count) { bytes[it].toInt() and 0xff }
    }

    private fun checkedSize(count: Int, size: Int): Int {
        val total = count.toLong() * size
        if (total > Int.MAX_VALUE) {
            throw Moc3Exception("I/O error: failed to fill whole buffer")
        }
        return total.toInt()
    }

    fun nextSection() {
        if (section >= offsets.size) {
            throw Moc3Exception("Invalid offset: missing section $section")
        }
        advanceTo(offsets[section])
        section += 1
    }

    fun advanceTo(to: Long) {
        if (to < p) {
            throw Moc3Exception("Invalid offset: Tried to seek from 0x${p.toString(16)} to 0x${to.toString(16)}")
        }
        if (to == p.toLong()) {
            return
        }
        val skip = to - p
        if (skip > 0x2000) {
            throw Moc3Exception(
                "Invalid offset: Tried to seek from 0x${p.toString(16)} to 0x${to.toString(16)} " +
                    "(0x${skip.toString(16)} bytes > 0x2000)"
            )
        }
        if (skip > data.size - p) {
            throw Moc3Exception("I/O error: failed to fill whole buffer")
        }
        for (i in 0 until skip.toInt()) {
            val value = data[p + i].toInt() and 0xff
            if (value != 0) {
                throw Moc3Exception("Invalid padding: Non-zero 0x${value.toString(16)} at offset 0x${(p + i).toString(16)}")
            }
        }
        p = to.toInt()
    }
}

internal class Moc3File(
    val version: Moc3Version,
    val canvas: Moc3Canvas,
    val tables: Map<String, Moc3Table>,
) {
    fun table(name: String): Moc3Table {
        return tables.getValue(name)
    }
}

private fun unsignedLess(value: Int, bound: Int): Boolean {
    return (value.toLong() and 0xffffffffL) < bound.toLong()
}

private fun identifierToString(bytes: ByteArray, offset: Int): String {
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = try {
        decoder.decode(ByteBuffer.wrap(bytes, offset, 64)).toString()
    } catch (error: CharacterCodingException) {
        throw Moc3Exception("Invalid value: invalid utf-8 sequence")
    }
    val end = text.indexOf('\u0000')
    return if (end >= 0) text.substring(0, end) else text
}

private fun isAdvanced(color: Int, alpha: Int): Boolean {
    return when (color) {
        0 -> alpha != 0
        1, 2 -> false
        else -> true
    }
}

internal fun parseMoc3(data: ByteArray): Moc3File {
    val reader = SectionReader(data)
    val magic = reader.readExact(4)
    if (!(magic[0] == 'M'.code.toByte() && magic[1] == 'O'.code.toByte() &&
            magic[2] == 'C'.code.toByte() && magic[3] == '3'.code.toByte())
    ) {
        throw Moc3Exception("Invalid magic value ${magic.joinToString(", ", "[", "]") { (it.toInt() and 0xff).toString() }}")
    }
    val rawVersion = reader.readU32()
    val version = Moc3Version.entries.firstOrNull { it.raw.toLong() == rawVersion }
        ?: throw Moc3Exception("Unknown/unsupported version $rawVersion")
    reader.advanceTo(0x40)
    val sectionCount = moc3SectionCount(version)
    reader.offsets = LongArray(sectionCount) { reader.readU32() }
    reader.nextSection()
    val counts = LongArray(moc3ClassCount(version)) { reader.readU32() }
    reader.nextSection()
    val canvas = Moc3Canvas(
        scale = reader.readF32(),
        centerX = reader.readF32(),
        centerY = reader.readF32(),
        width = reader.readF32(),
        height = reader.readF32(),
    )
    val tables = LinkedHashMap<String, Moc3Table>()
    for (spec in classSpecs) {
        tables[spec.name] = Moc3Table(spec.name)
    }
    var countIndex = 0
    for (spec in classSpecs) {
        if (spec.groupPass <= version.pass) {
            val count = counts[countIndex]
            if (count > Int.MAX_VALUE) {
                throw Moc3Exception("I/O error: failed to fill whole buffer")
            }
            tables.getValue(spec.name).rawCount = count.toInt()
            countIndex += 1
        }
    }
    for (pass in Moc3Pass.entries) {
        if (pass > version.pass) {
            break
        }
        for (spec in classSpecs) {
            if (spec.groupPass > pass) {
                continue
            }
            parseClass(spec, tables.getValue(spec.name), pass, reader)
        }
    }
    val file = Moc3File(version, canvas, tables)
    upgrade(file)
    validate(file)
    findRefs(file)
    postValidate(file)
    return file
}

private fun parseClass(spec: ClassSpec, table: Moc3Table, pass: Moc3Pass, reader: SectionReader) {
    val primitive = spec.primitive
    if (primitive != null) {
        if (spec.groupPass != pass) {
            return
        }
        if (table.rawCount % primitive.stride != 0) {
            throw Moc3Exception(
                "Unaligned item count: ${spec.name} count ${table.rawCount} not a multiple of ${primitive.stride}"
            )
        }
        reader.nextSection()
        if (primitive.isU16) {
            table.primitiveShorts = reader.readU16Array(table.rawCount)
        } else {
            table.primitiveFloats = reader.readF32Array(table.rawCount)
        }
        return
    }
    val count = table.rawCount
    for (field in spec.fields) {
        if (field.pass != pass) {
            continue
        }
        when (field.kind) {
            FieldKind.U32_PAIR -> {
                reader.nextSection()
                table.ints[field.name] = reader.readU32Array(count * 2)
            }
            FieldKind.IDENTIFIER -> {
                reader.nextSection()
                val bytes = reader.readExact(count * 64)
                table.strings[field.name] = Array(count) { identifierToString(bytes, it * 64) }
            }
            FieldKind.BOOL32 -> {
                reader.nextSection()
                val values = reader.readU32Array(count)
                for (value in values) {
                    if (value != 0 && value != 1) {
                        throw Moc3Exception("Invalid value: Unexpected Bool32 value ${value.toLong() and 0xffffffffL}")
                    }
                }
                table.ints[field.name] = values
            }
            FieldKind.U32, FieldKind.REF -> {
                reader.nextSection()
                table.ints[field.name] = reader.readU32Array(count)
            }
            FieldKind.OPT_REF -> {
                reader.nextSection()
                table.ints[field.name] = reader.readU32Array(count)
            }
            FieldKind.F32 -> {
                reader.nextSection()
                table.floats[field.name] = reader.readF32Array(count)
            }
            FieldKind.U8 -> {
                reader.nextSection()
                table.ints[field.name] = reader.readU8Array(count)
            }
            FieldKind.U16 -> {
                reader.nextSection()
                val shorts = reader.readU16Array(count)
                table.ints[field.name] = IntArray(count) { shorts[it].toInt() and 0xffff }
            }
            FieldKind.ARRAY_REF -> {
                reader.nextSection()
                table.ints["i_${field.name}"] = reader.readU32Array(count)
                reader.nextSection()
                table.ints["cnt_${field.name}"] = reader.readU32Array(count)
            }
            FieldKind.DEFORMER_TYPE -> {
                reader.nextSection()
                val values = reader.readU32Array(count)
                for (value in values) {
                    if (value != 0 && value != 1) {
                        throw Moc3Exception("Invalid value: ${value.toLong() and 0xffffffffL}")
                    }
                }
                table.ints[field.name] = values
            }
            FieldKind.SNAP_TYPE -> {
                reader.nextSection()
                val values = reader.readU32Array(count)
                for (value in values) {
                    if (value != 0 && value != 1 && value != 3) {
                        throw Moc3Exception("Invalid value: ${value.toLong() and 0xffffffffL}")
                    }
                }
                table.ints[field.name] = values
            }
            FieldKind.DRAW_ITEM_TYPE -> {
                reader.nextSection()
                val values = reader.readU32Array(count)
                for (value in values) {
                    if (value != 0 && value != 1) {
                        throw Moc3Exception("Invalid value: ${value.toLong() and 0xffffffffL}")
                    }
                }
                table.ints[field.name] = values
            }
            FieldKind.BLEND_CONFIG -> {
                reader.nextSection()
                val values = reader.readU32Array(count)
                val colors = IntArray(count)
                val alphas = IntArray(count)
                for (i in 0 until count) {
                    val value = values[i]
                    if ((value ushr 16) != 0) {
                        throw Moc3Exception("Invalid value: BlendConfig = 0x${(value.toLong() and 0xffffffffL).toString(16)}")
                    }
                    val color = value and 0xff
                    var alpha = (value ushr 8) and 0xff
                    if (color > 17) {
                        throw Moc3Exception("Invalid value: $color")
                    }
                    if (alpha > 4) {
                        throw Moc3Exception("Invalid value: $alpha")
                    }
                    if (!isAdvanced(color, alpha)) {
                        alpha = 0
                    }
                    colors[i] = color
                    alphas[i] = alpha
                }
                table.ints["${field.name}_color"] = colors
                table.ints["${field.name}_alpha"] = alphas
            }
        }
    }
}

private fun defaultColors(file: Moc3File, count: Int): Int {
    val multiply = file.table("MultiplyColor")
    val screen = file.table("ScreenColor")
    val result = multiply.rawCount
    val newCount = multiply.rawCount + count
    for (component in listOf("r", "g", "b")) {
        multiply.floats[component] = multiply.float(component).copyOf(newCount).also {
            it.fill(1f, multiply.rawCount, newCount)
        }
        screen.floats[component] = screen.float(component).copyOf(newCount).also {
            it.fill(0f, screen.rawCount, newCount)
        }
    }
    multiply.rawCount = newCount
    screen.rawCount = newCount
    return result
}

private fun validateArrayRef(file: Moc3File, owner: Moc3Table, field: String, target: String) {
    val starts = owner.int("i_$field")
    val counts = owner.int("cnt_$field")
    val bound = file.table(target).rawCount
    for (i in starts.indices) {
        val count = counts[i].toLong() and 0xffffffffL
        if (count == 0L) {
            continue
        }
        val start = starts[i].toLong() and 0xffffffffL
        val end = start + count - 1
        if (end > 0xffffffffL) {
            throw Moc3Exception("Invalid reference: ${owner.name}.$field[$i] = base $start, count $count (overflow)")
        }
        if (end >= bound) {
            throw Moc3Exception("Invalid reference: ${owner.name}.$field[$i] = [$start..=$end] (bound=$bound)")
        }
    }
}

private fun upgrade(file: Moc3File) {
    val version = file.version
    for (name in listOf("Part", "ArtMesh", "RotDeformer", "WarpDeformer", "Glue", "OffscreenPart")) {
        val table = file.table(name)
        table.ints["blend_form_maps"] = IntArray(table.rawCount) { -1 }
    }
    for (name in listOf("RotDeformer", "WarpDeformer")) {
        val table = file.table(name)
        table.ints["deformer"] = IntArray(table.rawCount) { -1 }
    }
    val warp = file.table("WarpDeformer")
    val rot = file.table("RotDeformer")
    val artMesh = file.table("ArtMesh")
    val param = file.table("Param")
    val artMeshForm = file.table("ArtMeshForm")
    val warpForm = file.table("WarpForm")
    val rotForm = file.table("RotForm")
    if (version < Moc3Version.V3_3) {
        warp.ints["bilinear_interpolation"] = IntArray(warp.rawCount)
    }
    if (version < Moc3Version.V4_2) {
        param.ints["unk_zero_2"] = IntArray(param.rawCount * 2)
        param.ints["i_keypoints"] = IntArray(param.rawCount)
        param.ints["cnt_keypoints"] = IntArray(param.rawCount)
        param.ints["blendshape"] = IntArray(param.rawCount)
        param.ints["i_blend_maps"] = IntArray(param.rawCount)
        param.ints["cnt_blend_maps"] = IntArray(param.rawCount)
        val index = defaultColors(file, 1)
        artMesh.ints["i_color_forms"] = IntArray(artMesh.rawCount) { index }
        warp.ints["i_color_forms"] = IntArray(warp.rawCount) { index }
        rot.ints["i_color_forms"] = IntArray(rot.rawCount) { index }
        for (form in listOf(artMeshForm, warpForm, rotForm)) {
            form.ints["multiply_color"] = IntArray(form.rawCount) { index }
            form.ints["screen_color"] = IntArray(form.rawCount) { index }
        }
    } else if (version < Moc3Version.V5_0) {
        for (form in listOf(artMeshForm, warpForm, rotForm)) {
            form.ints["multiply_color"] = IntArray(form.rawCount)
            form.ints["screen_color"] = IntArray(form.rawCount)
        }
        val owners = listOf(
            Triple(artMesh, artMeshForm, "ArtMeshForm"),
            Triple(rot, rotForm, "RotForm"),
            Triple(warp, warpForm, "WarpForm"),
        )
        for ((owner, form, formName) in owners) {
            validateArrayRef(file, owner, "forms", formName)
            val starts = owner.int("i_forms")
            val counts = owner.int("cnt_forms")
            val colors = owner.int("i_color_forms")
            val multiply = form.int("multiply_color")
            val screen = form.int("screen_color")
            for (i in 0 until owner.rawCount) {
                for (j in 0 until counts[i]) {
                    multiply[starts[i] + j] = colors[i] + j
                    screen[starts[i] + j] = colors[i] + j
                }
            }
        }
    }
    if (version < Moc3Version.V5_3) {
        val part = file.table("Part")
        val partForm = file.table("PartForm")
        part.ints["offscreen_part"] = IntArray(part.rawCount) { -1 }
        partForm.ints["offscreen"] = IntArray(partForm.rawCount) { -1 }
        val renderConfig = artMesh.int("render_config")
        artMesh.ints["blend_config_color"] = IntArray(artMesh.rawCount) { renderConfig[it] and 3 }
        artMesh.ints["blend_config_alpha"] = IntArray(artMesh.rawCount)
    }
}

private fun validate(file: Moc3File) {
    for (spec in classSpecs) {
        val table = file.table(spec.name)
        if (spec.primitive != null) {
            continue
        }
        for (field in spec.fields) {
            val target = field.target ?: continue
            val bound = file.table(target).rawCount
            when (field.kind) {
                FieldKind.REF -> {
                    val values = table.int(field.name)
                    for (i in values.indices) {
                        if (!unsignedLess(values[i], bound)) {
                            throw Moc3Exception(
                                "Invalid reference: ${spec.name}.${field.name}[$i] = " +
                                    "${values[i].toLong() and 0xffffffffL} (bound=$bound)"
                            )
                        }
                    }
                }
                FieldKind.OPT_REF -> {
                    val values = table.int(field.name)
                    for (i in values.indices) {
                        if (values[i] == -1) {
                            continue
                        }
                        if (!unsignedLess(values[i], bound)) {
                            throw Moc3Exception(
                                "Invalid reference: ${spec.name}.${field.name}[$i] = " +
                                    "${values[i].toLong() and 0xffffffffL} (bound=$bound)"
                            )
                        }
                    }
                }
                FieldKind.ARRAY_REF -> validateArrayRef(file, table, field.name, target)
                else -> {}
            }
        }
    }
    validateObjects(file)
}

private fun check(condition: Boolean, className: String, index: Int, test: String) {
    if (!condition) {
        throw Moc3Exception("Validation error: $className[$index]: $test")
    }
}

private fun validateObjects(file: Moc3File) {
    for (name in listOf("Part", "Deformer", "ArtMesh", "Param", "Glue")) {
        val table = file.table(name)
        val header = table.int("hdr")
        for (i in 0 until table.rawCount) {
            check(header[2 * i] == 0 && header[2 * i + 1] == 0, name, i, "*self.f_hdr() == U32Pair(0, 0)")
        }
    }
    val deformer = file.table("Deformer")
    val warpCount = file.table("WarpDeformer").rawCount.toLong()
    val rotCount = file.table("RotDeformer").rawCount.toLong()
    val types = deformer.int("deformer_type")
    val typed = deformer.int("i_typed")
    for (i in 0 until deformer.rawCount) {
        val index = typed[i].toLong() and 0xffffffffL
        if (types[i] == 0) {
            check(index <= warpCount, "Deformer", i, "*self.f_i_typed() as usize <= self.model.warp_deformer.count")
        } else {
            check(index <= rotCount, "Deformer", i, "*self.f_i_typed() as usize <= self.model.rot_deformer.count")
        }
    }
    val artMesh = file.table("ArtMesh")
    val renderConfig = artMesh.int("render_config")
    for (i in 0 until artMesh.rawCount) {
        check((renderConfig[i] shr 4) == 0, "ArtMesh", i, "(self.f_render_config() >> 4) == 0")
        check((renderConfig[i] and 3) <= 2, "ArtMesh", i, "BlendMode::from_repr(self.f_render_config() & 3).is_some()")
    }
    val drawItem = file.table("DrawItem")
    val itemTypes = drawItem.int("item_type")
    val children = drawItem.int("i_child")
    val drawGroups = drawItem.int("draw_group")
    val artMeshCount = artMesh.rawCount.toLong()
    val partCount = file.table("Part").rawCount.toLong()
    for (i in 0 until drawItem.rawCount) {
        val child = children[i].toLong() and 0xffffffffL
        if (itemTypes[i] == 0) {
            check(child < artMeshCount, "DrawItem", i, "(*self.f_i_child() as usize) < self.model.art_mesh.count")
            check(drawGroups[i] == -1, "DrawItem", i, "self.i_draw_group().get().is_none()")
        } else {
            check(child < partCount, "DrawItem", i, "(*self.f_i_child() as usize) < self.model.part.count")
            check(drawGroups[i] != -1, "DrawItem", i, "self.i_draw_group().get().is_some()")
        }
    }
    val glue = file.table("Glue")
    val coordCounts = glue.int("cnt_coords")
    for (i in 0 until glue.rawCount) {
        check((coordCounts[i].toLong() and 0xffffffffL) % 2 == 0L, "Glue", i, "self.cnt_coords().is_multiple_of(2)")
    }
    val offscreenPart = file.table("OffscreenPart")
    val part = file.table("Part")
    val partOffscreen = part.int("offscreen_part")
    val offscreenParts = offscreenPart.int("part")
    val zeros = offscreenPart.int("unk_zeros")
    val offscreenRender = offscreenPart.int("render_config")
    val offscreenColors = offscreenPart.int("blend_config_color")
    for (i in 0 until offscreenPart.rawCount) {
        check(partOffscreen[offscreenParts[i]] == i, "OffscreenPart", i, "self.part_view().i_offscreen_part().0 == self.idx as i32")
        check(zeros[i] == 0, "OffscreenPart", i, "*self.f_unk_zeros() == 0")
        check((offscreenRender[i] shr 4) == 0, "OffscreenPart", i, "(self.f_render_config() >> 4) == 0")
        check((offscreenRender[i] and 3) <= 2, "OffscreenPart", i, "BlendMode::from_repr(self.f_render_config() & 3).is_some()")
        if (offscreenColors[i] <= 2) {
            check((offscreenRender[i] and 3) == offscreenColors[i], "OffscreenPart", i, "self.f_render_config() & 3 == self.f_blend_config().color as u8")
        } else {
            check((offscreenRender[i] and 3) == 0, "OffscreenPart", i, "self.f_render_config() & 3 == BlendMode::Normal as u8")
        }
    }
}

private fun findRefs(file: Moc3File) {
    val deformer = file.table("Deformer")
    val warp = file.table("WarpDeformer")
    val rot = file.table("RotDeformer")
    val types = deformer.int("deformer_type")
    val typed = deformer.int("i_typed")
    val warpDeformers = warp.int("deformer")
    val rotDeformers = rot.int("deformer")
    for (i in 0 until deformer.rawCount) {
        val index = typed[i]
        if (types[i] == 0) {
            if (!unsignedLess(index, warp.rawCount)) {
                throw Moc3Exception("Deformer[$i]: missing warp deformer $index")
            }
            warpDeformers[index] = i
        } else {
            if (!unsignedLess(index, rot.rawCount)) {
                throw Moc3Exception("Deformer[$i]: missing rotation deformer $index")
            }
            rotDeformers[index] = i
        }
    }
    val maps = listOf(
        Triple("ArtMeshBlendFormMaps", "artmesh", "ArtMesh"),
        Triple("RotBlendFormMaps", "rot", "RotDeformer"),
        Triple("WarpBlendFormMaps", "warp", "WarpDeformer"),
        Triple("PartBlendFormMaps", "part", "Part"),
        Triple("GlueBlendFormMaps", "glue", "Glue"),
        Triple("OffscreenPartBlendFormMaps", "offscreen_part", "OffscreenPart"),
    )
    for ((mapsName, field, ownerName) in maps) {
        val table = file.table(mapsName)
        val owners = table.int(field)
        val back = file.table(ownerName).int("blend_form_maps")
        for (i in 0 until table.rawCount) {
            val owner = owners[i]
            val existing = back[owner]
            if (existing != -1) {
                throw Moc3Exception(
                    "Duplicate reference: ${ownerName.removeSuffix("Deformer")} referenced by two blend form maps: " +
                        "$existing and $i"
                )
            }
            back[owner] = i
        }
    }
    val drawGroup = file.table("DrawGroup")
    val totals = drawGroup.int("total_artmesh_count")
    var root = -1
    var best = -1L
    for (i in 0 until drawGroup.rawCount) {
        val total = totals[i].toLong() and 0xffffffffL
        if (root == -1 || total >= best) {
            root = i
            best = total
        }
    }
    if (root != 0) {
        throw Moc3Exception("Invalid value: Root draw group is ${if (root == -1) "None" else "Some($root)"}, expected 0")
    }
}

private fun postValidate(file: Moc3File) {
    val maps = file.table("OffscreenPartBlendFormMaps")
    if (maps.rawCount == 0) {
        return
    }
    val offscreenPart = file.table("OffscreenPart")
    val part = file.table("Part")
    val partBlendFormMaps = file.table("PartBlendFormMaps")
    val blendFormMap = file.table("BlendFormMap")
    val partForm = file.table("PartForm")
    val offscreenParts = maps.int("offscreen_part")
    val mapStarts = maps.int("i_maps")
    val mapCounts = maps.int("cnt_maps")
    val partOfOffscreen = offscreenPart.int("part")
    val partMaps = part.int("blend_form_maps")
    val partMapStarts = partBlendFormMaps.int("i_maps")
    val partMapCounts = partBlendFormMaps.int("cnt_maps")
    val paramMaps = blendFormMap.int("param_map")
    val formStarts = blendFormMap.int("i_forms")
    val formCounts = blendFormMap.int("cnt_forms")
    val limitStarts = blendFormMap.int("i_blendweight_limits")
    val limitCounts = blendFormMap.int("cnt_blendweight_limits")
    val offscreenForms = partForm.int("offscreen")
    val name = "OffscreenPartBlendFormMaps"
    for (i in 0 until maps.rawCount) {
        val partIndex = partOfOffscreen[offscreenParts[i]]
        val partMapIndex = partMaps[partIndex]
        check(partMapIndex != -1, name, i, "part.blend_form_maps_view().is_some()")
        check(mapCounts[i] == partMapCounts[partMapIndex], name, i, "self.cnt_maps() == part_blend_form_maps.cnt_maps()")
        val count = minOf(mapCounts[i], partMapCounts[partMapIndex])
        for (j in 0 until count) {
            val map = mapStarts[i] + j
            val partMap = partMapStarts[partMapIndex] + j
            if (map >= blendFormMap.rawCount || partMap >= blendFormMap.rawCount) {
                break
            }
            check(paramMaps[map] == paramMaps[partMap], name, i, "map.i_param_map() == partmap.i_param_map()")
            check(formCounts[map] == formCounts[partMap], name, i, "map.f_cnt_forms() == partmap.f_cnt_forms()")
            check(
                limitStarts[map] == limitStarts[partMap] && limitCounts[map] == limitCounts[partMap],
                name,
                i,
                "map.range_blendweight_limits() == partmap.range_blendweight_limits()"
            )
            val formCount = minOf(formCounts[map], formCounts[partMap])
            for (k in 0 until formCount) {
                val form = formStarts[map] + k
                val partFormIndex = formStarts[partMap] + k
                if (partFormIndex >= partForm.rawCount || form >= file.table("OffscreenPartForm").rawCount) {
                    break
                }
                check(offscreenForms[partFormIndex] == form, name, i, "partform.i_offscreen().get() == Some(form.idx)")
            }
        }
    }
}
