package com.moblin.android.platform.live2d

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val DEPTH_FUDGE = 0.001f
private const val PARAM_FUDGE = 0.001f

internal fun rustMin(a: Float, b: Float): Float {
    if (a.isNaN()) {
        return b
    }
    if (b.isNaN()) {
        return a
    }
    return if (a < b) a else b
}

internal fun rustMax(a: Float, b: Float): Float {
    if (a.isNaN()) {
        return b
    }
    if (b.isNaN()) {
        return a
    }
    return if (a > b) a else b
}

internal fun rustClamp(value: Float, min: Float, max: Float): Float {
    var result = value
    if (result < min) {
        result = min
    }
    if (result > max) {
        result = max
    }
    return result
}

internal fun saturate(value: Float): Float {
    return rustClamp(value, 0f, 1f)
}

internal fun toRadians(degrees: Float): Float {
    return degrees * (Math.PI.toFloat() / 180f)
}

private fun floatToUsize(value: Float): Int {
    if (value.isNaN() || value <= 0f) {
        return 0
    }
    if (value >= Int.MAX_VALUE.toFloat()) {
        return Int.MAX_VALUE
    }
    return value.toInt()
}

private fun depthToInt(depth: Float): Int {
    return (rustClamp(depth, 0f, 1000f) + DEPTH_FUDGE).toInt()
}

internal class Live2DVisual(
    var visible: Boolean = false,
    var opacity: Float = 0f,
    var multiplyR: Float = 0f,
    var multiplyG: Float = 0f,
    var multiplyB: Float = 0f,
    var screenR: Float = 0f,
    var screenG: Float = 0f,
    var screenB: Float = 0f,
) {
    fun copy(): Live2DVisual {
        return Live2DVisual(visible, opacity, multiplyR, multiplyG, multiplyB, screenR, screenG, screenB)
    }

    fun apply(visual: Live2DVisual) {
        visual.opacity *= opacity
        visual.visible = visual.visible && visible
        visual.multiplyR *= multiplyR
        visual.multiplyG *= multiplyG
        visual.multiplyB *= multiplyB
        visual.screenR = saturate(visual.screenR + screenR)
        visual.screenG = saturate(visual.screenG + screenG)
        visual.screenB = saturate(visual.screenB + screenB)
    }

    companion object {
        fun fromValues(values: FloatArray): Live2DVisual {
            return Live2DVisual(
                visible = true,
                opacity = saturate(values[0]),
                multiplyR = saturate(values[1]),
                multiplyG = saturate(values[2]),
                multiplyB = saturate(values[3]),
                screenR = saturate(values[4]),
                screenG = saturate(values[5]),
                screenB = saturate(values[6]),
            )
        }
    }
}

internal class Affine2(
    var m00: Float = 1f,
    var m10: Float = 0f,
    var m01: Float = 0f,
    var m11: Float = 1f,
    var tx: Float = 0f,
    var ty: Float = 0f,
) {
    fun transformX(x: Float, y: Float): Float {
        return m00 * x + m01 * y + tx
    }

    fun transformY(x: Float, y: Float): Float {
        return m10 * x + m11 * y + ty
    }

    fun times(other: Affine2): Affine2 {
        return Affine2(
            m00 = m00 * other.m00 + m01 * other.m10,
            m10 = m10 * other.m00 + m11 * other.m10,
            m01 = m00 * other.m01 + m01 * other.m11,
            m11 = m10 * other.m01 + m11 * other.m11,
            tx = m00 * other.tx + m01 * other.ty + tx,
            ty = m10 * other.tx + m11 * other.ty + ty,
        )
    }

    companion object {
        fun fromScaleAngleTranslation(scaleX: Float, scaleY: Float, angle: Float, tx: Float, ty: Float): Affine2 {
            val sin = sin(angle)
            val cos = cos(angle)
            return Affine2(
                m00 = cos * scaleX,
                m10 = sin * scaleX,
                m01 = -sin * scaleY,
                m11 = cos * scaleY,
                tx = tx,
                ty = ty,
            )
        }
    }
}

internal abstract class DeformerSub {
    abstract val visual: Live2DVisual
    abstract val scale: Float

    abstract fun apply(coords: FloatArray, count: Int, visual: Live2DVisual)
}

internal class RotState(
    override val visual: Live2DVisual = Live2DVisual(),
    var affine: Affine2 = Affine2(),
    override var scale: Float = 0f,
) : DeformerSub() {
    override fun apply(coords: FloatArray, count: Int, visual: Live2DVisual) {
        for (i in 0 until count) {
            val x = coords[2 * i]
            val y = coords[2 * i + 1]
            coords[2 * i] = affine.transformX(x, y)
            coords[2 * i + 1] = affine.transformY(x, y)
        }
        this.visual.apply(visual)
    }
}

internal class WarpState(
    override val visual: Live2DVisual = Live2DVisual(),
    val sizeX: Int = 0,
    val sizeY: Int = 0,
    val bilinear: Boolean = false,
    val vertices: FloatArray = FloatArray(0),
    override var scale: Float = 0f,
) : DeformerSub() {
    private var cachedAffine: Affine2? = null

    private fun pointX(x: Int, y: Int): Float {
        return vertices[2 * ((sizeX + 1) * y + x)]
    }

    private fun pointY(x: Int, y: Int): Float {
        return vertices[2 * ((sizeX + 1) * y + x) + 1]
    }

    fun affine(): Affine2 {
        cachedAffine?.let { return it }
        val p00x = pointX(0, 0)
        val p00y = pointY(0, 0)
        val p01x = pointX(sizeX, 0)
        val p01y = pointY(sizeX, 0)
        val p10x = pointX(0, sizeY)
        val p10y = pointY(0, sizeY)
        val p11x = pointX(sizeX, sizeY)
        val p11y = pointY(sizeX, sizeY)
        val pcx = (p00x + p01x + p10x + p11x) / 4f
        val pcy = (p00y + p01y + p10y + p11y) / 4f
        val dxx = (p01x - p00x + p11x - p10x) / 2f
        val dxy = (p01y - p00y + p11y - p10y) / 2f
        val dyx = (p10x - p00x + p11x - p01x) / 2f
        val dyy = (p10y - p00y + p11y - p01y) / 2f
        val base = Affine2(m00 = dxx, m10 = dxy, m01 = dyx, m11 = dyy, tx = pcx, ty = pcy)
        val result = base.times(Affine2(tx = -0.5f, ty = -0.5f))
        cachedAffine = result
        return result
    }

    private fun extrapolatedPoint(x: Int, y: Int, out: FloatArray, offset: Int) {
        val lowX = x < 1
        val lowY = y < 1
        val highX = x > sizeX + 1
        val highY = y > sizeY + 1
        if (!lowX && !lowY && !highX && !highY) {
            out[offset] = pointX(x - 1, y - 1)
            out[offset + 1] = pointY(x - 1, y - 1)
            return
        }
        val px = if (lowX) -2f else if (highX) 3f else (x.toFloat() - 1f) / sizeX.toFloat()
        val py = if (lowY) -2f else if (highY) 3f else (y.toFloat() - 1f) / sizeY.toFloat()
        val affine = affine()
        out[offset] = affine.transformX(px, py)
        out[offset + 1] = affine.transformY(px, py)
    }

    private val corners = FloatArray(8)

    override fun apply(coords: FloatArray, count: Int, visual: Live2DVisual) {
        val fsizeX = sizeX.toFloat()
        val fsizeY = sizeY.toFloat()
        for (i in 0 until count) {
            val cx = coords[2 * i]
            val cy = coords[2 * i + 1]
            val minElement = if (cx < cy) cx else cy
            val maxElement = if (cx > cy) cx else cy
            if (minElement < 0f || maxElement > 1f) {
                if (minElement <= -2f || maxElement >= 3f) {
                    val affine = affine()
                    coords[2 * i] = affine.transformX(cx, cy)
                    coords[2 * i + 1] = affine.transformY(cx, cy)
                    continue
                }
                val rx = (if (cx < 0f) cx / 2f else if (cx > 1f) (cx - 1f) / 2f + fsizeX else cx * fsizeX) + 1f
                val ry = (if (cy < 0f) cy / 2f else if (cy > 1f) (cy - 1f) / 2f + fsizeY else cy * fsizeY) + 1f
                val ix = floatToUsize(rx).coerceIn(0, sizeX + 1)
                val iy = floatToUsize(ry).coerceIn(0, sizeY + 1)
                val fx = rx - ix.toFloat()
                val fy = ry - iy.toFloat()
                extrapolatedPoint(ix, iy, corners, 0)
                extrapolatedPoint(ix + 1, iy, corners, 2)
                extrapolatedPoint(ix, iy + 1, corners, 4)
                extrapolatedPoint(ix + 1, iy + 1, corners, 6)
                triangle(coords, i, fx, fy)
            } else {
                val rx = cx * fsizeX
                val ry = cy * fsizeY
                val ix = floatToUsize(rx).coerceIn(0, sizeX - 1)
                val iy = floatToUsize(ry).coerceIn(0, sizeY - 1)
                val fx = rx - ix.toFloat()
                val fy = ry - iy.toFloat()
                corners[0] = pointX(ix, iy)
                corners[1] = pointY(ix, iy)
                corners[2] = pointX(ix + 1, iy)
                corners[3] = pointY(ix + 1, iy)
                corners[4] = pointX(ix, iy + 1)
                corners[5] = pointY(ix, iy + 1)
                corners[6] = pointX(ix + 1, iy + 1)
                corners[7] = pointY(ix + 1, iy + 1)
                if (bilinear) {
                    val p0x = corners[0] * (1f - fx) + corners[2] * fx
                    val p0y = corners[1] * (1f - fx) + corners[3] * fx
                    val p1x = corners[4] * (1f - fx) + corners[6] * fx
                    val p1y = corners[5] * (1f - fx) + corners[7] * fx
                    coords[2 * i] = p0x * (1f - fy) + p1x * fy
                    coords[2 * i + 1] = p0y * (1f - fy) + p1y * fy
                } else {
                    triangle(coords, i, fx, fy)
                }
            }
        }
        this.visual.apply(visual)
    }

    private fun triangle(coords: FloatArray, i: Int, fx: Float, fy: Float) {
        if ((fx + fy) < 1f) {
            val w = 1f - (fx + fy)
            coords[2 * i] = w * corners[0] + fx * corners[2] + fy * corners[4]
            coords[2 * i + 1] = w * corners[1] + fx * corners[3] + fy * corners[5]
        } else {
            val gx = 1f - fx
            val gy = 1f - fy
            val w = 1f - (gx + gy)
            coords[2 * i] = w * corners[6] + gx * corners[4] + gy * corners[2]
            coords[2 * i + 1] = w * corners[7] + gx * corners[5] + gy * corners[3]
        }
    }
}

private class DeformerState {
    var clean = false
    var updated = false
    var sub: DeformerSub? = null

    fun visible(): Boolean {
        return sub?.visual?.visible ?: false
    }
}

private class ParamState(var exists: Boolean = false, var clean: Boolean = false, var value: Float = 0f)

private class ParamMapState {
    var clean = false
    var index = -1
    var t = 0f

    val hasValue: Boolean
        get() = index >= 0
}

private class BlendLimitState {
    var updated = false
    var weight = 0f
}

private class PartState(
    var exists: Boolean = false,
    var visibleArtmeshes: Boolean = false,
    var visibleDeformers: Boolean = false,
    var depth: Int = 0,
    var visual: Live2DVisual? = null,
    var userOpacity: Float = 0f,
    var opacity: Float = 0f,
    var modified: Boolean = false,
    var clean: Boolean = false,
    var updated: Boolean = false,
) {
    fun apply(other: PartState) {
        if (!visibleArtmeshes) {
            other.visibleArtmeshes = false
        }
        if (!visibleDeformers) {
            other.visibleDeformers = false
        }
        other.opacity *= opacity
    }
}

private class ArtMeshState(
    var initialized: Boolean = false,
    var updated: Boolean = false,
    var visual: Live2DVisual = Live2DVisual(),
    var depth: Int = 0,
    var vertices: FloatArray = FloatArray(0),
    var gluedVertices: FloatArray? = null,
)

internal class DrivenArtMesh(val visual: Live2DVisual, val vertices: FloatArray)

internal class FormSet(val forms: IntArray, val weights: FloatArray)

internal sealed class DrawNode {
    data class ArtMesh(val uid: Int) : DrawNode()

    data class OffscreenPart(val uid: Int) : DrawNode()
}

internal class Live2DDriverException(message: String) : Exception(message)

internal class Live2DDriver(private val model: Live2DModelData) {
    private val paramUids = HashMap<String, Int>()
    private val partUids = HashMap<String, Int>()
    private val params = Array(model.paramCount) { ParamState() }
    private val paramMaps = Array(model.paramMapCount) { ParamMapState() }
    private val blendParamMaps = Array(model.blendParamMapCount) { ParamMapState() }
    private val blendLimits = Array(model.blendWeightLimitCount) { BlendLimitState() }
    private val deformers = Array(model.deformerCount) { DeformerState() }
    private val artMeshes = Array(model.artMeshCount) { ArtMeshState() }
    private val parts = Array(model.partCount) { PartState() }
    private var partDrawNodes: Map<Int, List<DrawNode>> = emptyMap()
    private var rootDrawNodes: List<DrawNode> = emptyList()
    private var orderChanged = true

    init {
        for (i in 0 until model.paramCount) {
            paramUids[model.paramIds[i]] = i
            params[i] = ParamState(exists = true, clean = false, value = model.paramDefault[i])
        }
        for (i in 0 until model.partCount) {
            partUids[model.partIds[i]] = i
            parts[i] = PartState(exists = true, modified = true, depth = Int.MIN_VALUE, userOpacity = 1f)
        }
    }

    fun setParamById(id: String, value: Float) {
        val uid = paramUids[id] ?: return
        val state = params[uid]
        if (!state.exists) {
            return
        }
        state.clean = state.clean && state.value == value
        state.value = value
    }

    fun setPartOpacityById(id: String, opacity: Float) {
        val uid = partUids[id] ?: return
        val state = parts[uid]
        if (!state.exists) {
            return
        }
        state.modified = state.modified || state.userOpacity != opacity
        state.userOpacity = opacity
    }

    fun setPose(pose: Live2DPose) {
        val map = pose.map
        for (index in 0 until map.descriptors.size) {
            val descriptor = map.descriptors[index]
            val value = pose.values[index]?.flatten(descriptor.default) ?: descriptor.default
            if (descriptor.isParam) {
                setParamById(descriptor.id, value)
            } else {
                setPartOpacityById(descriptor.id, value)
            }
        }
    }

    private fun formIndex(start: Int, count: Int, tableCount: Int, index: Int): Int {
        if (index < 0 || index >= count) {
            throw Live2DDriverException("Form index $index out of range ($count forms)")
        }
        val absolute = start + index
        if (absolute >= tableCount) {
            throw Live2DDriverException("Form $absolute out of range ($tableCount forms)")
        }
        return absolute
    }

    private fun getFormSet(
        maps: IntArray,
        forms: Live2DRange,
        formTableCount: Int,
        blends: IntArray?,
    ): FormSet? {
        val strides = IntArray(maps.size)
        val indices = IntArray(maps.size)
        val ts = FloatArray(maps.size)
        var stateCount = 0
        for (map in maps) {
            val state = paramMaps[map]
            if (!state.hasValue) {
                return null
            }
            val range = model.paramMapKeypoints[map]
            val stride = range.count
            if (stride > 1) {
                strides[stateCount] = stride
                indices[stateCount] = state.index
                ts[stateCount] = state.t
                stateCount += 1
            }
        }
        if (stateCount >= 32) {
            throw Live2DDriverException("Too many parameter maps: $stateCount")
        }
        val formCount = 1 shl stateCount
        val capacity = formCount + 2 * (blends?.size ?: 0)
        val formList = IntArray(capacity)
        val weightList = FloatArray(capacity)
        var size = 0
        for (i in 0 until formCount) {
            var stride = 1
            var index = 0
            var weight = 1f
            for (j in 0 until stateCount) {
                if (i and (1 shl j) != 0) {
                    index += stride * (indices[j] + 1)
                    weight *= ts[j]
                } else {
                    index += stride * indices[j]
                    weight *= 1f - ts[j]
                }
                stride *= strides[j]
            }
            if (weight > 0f) {
                formList[size] = formIndex(forms.start, forms.count, formTableCount, index)
                weightList[size] = weight
                size += 1
            }
        }
        if (blends != null) {
            for (blend in blends) {
                var weight = 1f
                for (limit in model.blendFormMapLimits[blend]) {
                    weight = rustMin(weight, blendLimits[limit].weight)
                }
                val paramMap = model.blendFormMapParamMap[blend]
                val neutral = model.blendParamMapNeutral[paramMap]
                val state = blendParamMaps[paramMap]
                if (!state.hasValue) {
                    return null
                }
                val index = state.index
                val t = state.t
                val start = model.blendFormMapFormsStart[blend]
                val count = model.blendFormMapFormsCount[blend]
                if (index != neutral) {
                    val w = weight * (1f - t)
                    if (w > 0f) {
                        formList[size] = formIndex(start, count, formTableCount, index)
                        weightList[size] = w
                        size += 1
                    }
                }
                if (index + 1 != neutral) {
                    val w = weight * t
                    if (w > 0f) {
                        formList[size] = formIndex(start, count, formTableCount, index + 1)
                        weightList[size] = w
                        size += 1
                    }
                }
            }
        }
        return FormSet(formList.copyOf(size), weightList.copyOf(size))
    }

    private fun blendVisual(
        set: FormSet,
        opacity: FloatArray,
        multiply: IntArray,
        screen: IntArray,
        extra: Int,
        extraValues: (Int, Int) -> Float,
    ): FloatArray {
        val result = FloatArray(7 + extra)
        for (k in 0 until 7 + extra) {
            var sum = 0f
            for (i in set.forms.indices) {
                val form = set.forms[i]
                val value = when (k) {
                    0 -> opacity[form]
                    1 -> model.multiplyR[multiply[form]]
                    2 -> model.multiplyG[multiply[form]]
                    3 -> model.multiplyB[multiply[form]]
                    4 -> model.screenR[screen[form]]
                    5 -> model.screenG[screen[form]]
                    6 -> model.screenB[screen[form]]
                    else -> extraValues(k - 7, form)
                }
                sum += value * set.weights[i]
            }
            result[k] = sum
        }
        return result
    }

    private fun blendScalar(set: FormSet, values: FloatArray): Float {
        var sum = 0f
        for (i in set.forms.indices) {
            sum += values[set.forms[i]] * set.weights[i]
        }
        return sum
    }

    private fun blendVertices(set: FormSet, starts: IntArray, fileCount: Int, count: Int): FloatArray {
        val result = FloatArray(2 * count)
        val sources = IntArray(set.forms.size)
        for (i in set.forms.indices) {
            val start = starts[set.forms[i]]
            if (start % 2 != 0) {
                throw Live2DDriverException("Unaligned vertex index $start")
            }
            if (fileCount < count || start + 2 * fileCount > model.vertexCoords.size) {
                throw Live2DDriverException("Vertex range out of bounds")
            }
            sources[i] = start
        }
        val coords = model.vertexCoords
        for (k in 0 until 2 * count) {
            var sum = 0f
            for (i in sources.indices) {
                sum += coords[sources[i] + k] * set.weights[i]
            }
            result[k] = sum
        }
        return result
    }

    private fun calcRot(deformer: Int, rot: Int, visible: Boolean): RotState {
        val set = getFormSet(
            model.paramMapSetMaps[model.rotParamMapSet[rot]],
            model.rotForms[rot],
            model.rotFormCount,
            model.rotBlendFormMaps[rot],
        ) ?: return RotState()
        val first = set.forms.firstOrNull() ?: throw Live2DDriverException("No forms")
        val flipX = if (model.rotFormFlipX[first]) -1f else 1f
        val flipY = if (model.rotFormFlipY[first]) -1f else 1f
        val values = blendVisual(set, model.rotFormOpacity, model.rotFormMultiply, model.rotFormScreen, 4) { k, form ->
            when (k) {
                0 -> model.rotFormScale[form]
                1 -> model.rotFormAngle[form]
                2 -> model.rotFormPosX[form]
                else -> model.rotFormPosY[form]
            }
        }
        val scale = values[7]
        val angle = values[8]
        val state = RotState(
            visual = Live2DVisual.fromValues(values),
            affine = Affine2.fromScaleAngleTranslation(
                flipX * scale,
                flipY * scale,
                toRadians(angle + model.rotAngleOffset[rot]),
                values[9],
                values[10],
            ),
            scale = scale,
        )
        state.visual.visible = model.deformerVisible[deformer] && visible
        val parent = model.deformerParent[deformer]
        if (parent != -1) {
            val parentState = deformers[parent]
            val parentSub = parentState.sub ?: throw Live2DDriverException("Parent deformer has no state")
            state.scale *= parentSub.scale
            parentSub.apply(FloatArray(0), 0, state.visual)
            when (parentSub) {
                is RotState -> state.affine = parentSub.affine.times(state.affine)
                is WarpState -> {
                    val points = floatArrayOf(
                        state.affine.tx,
                        state.affine.ty,
                        state.affine.tx + 0f,
                        state.affine.ty + -0.1f,
                    )
                    parentSub.apply(points, 2, state.visual)
                    val newAngle = if (points[2] != points[0] || points[3] != points[1]) {
                        val dx = points[2] - points[0]
                        val dy = points[3] - points[1]
                        atan2(dx, -dy)
                    } else {
                        0f
                    }
                    val scaleRotation = Affine2.fromScaleAngleTranslation(
                        parentSub.scale,
                        parentSub.scale,
                        newAngle,
                        0f,
                        0f,
                    )
                    val m = state.affine
                    state.affine = Affine2(
                        m00 = scaleRotation.m00 * m.m00 + scaleRotation.m01 * m.m10,
                        m10 = scaleRotation.m10 * m.m00 + scaleRotation.m11 * m.m10,
                        m01 = scaleRotation.m00 * m.m01 + scaleRotation.m01 * m.m11,
                        m11 = scaleRotation.m10 * m.m01 + scaleRotation.m11 * m.m11,
                        tx = points[0],
                        ty = points[1],
                    )
                }
            }
        }
        return state
    }

    private fun calcWarp(deformer: Int, warp: Int, visible: Boolean): WarpState {
        val set = getFormSet(
            model.paramMapSetMaps[model.warpParamMapSet[warp]],
            model.warpForms[warp],
            model.warpFormCount,
            model.warpBlendFormMaps[warp],
        ) ?: return WarpState()
        val values = blendVisual(set, model.warpFormOpacity, model.warpFormMultiply, model.warpFormScreen, 0) { _, _ -> 0f }
        val sizeX = model.warpXDivs[warp]
        val sizeY = model.warpYDivs[warp]
        val vertexCount = (sizeX + 1) * (sizeY + 1)
        val vertices = blendVertices(set, model.warpFormStartVertex, model.warpFileVertexCount[warp], vertexCount)
        val state = WarpState(
            visual = Live2DVisual.fromValues(values),
            sizeX = sizeX,
            sizeY = sizeY,
            bilinear = model.warpBilinear[warp],
            vertices = vertices,
            scale = 1f,
        )
        state.visual.visible = model.deformerVisible[deformer] && visible
        val parent = model.deformerParent[deformer]
        if (parent != -1) {
            val parentSub = deformers[parent].sub ?: throw Live2DDriverException("Parent deformer has no state")
            state.scale *= parentSub.scale
            parentSub.apply(state.vertices, vertexCount, state.visual)
        }
        return state
    }

    private fun paramMapsChanged(maps: IntArray): Boolean {
        for (map in maps) {
            if (!paramMaps[map].clean) {
                return true
            }
        }
        return false
    }

    private fun blendFormMapsChanged(blends: IntArray?): Boolean {
        if (blends == null) {
            return false
        }
        for (blend in blends) {
            if (!blendParamMaps[model.blendFormMapParamMap[blend]].clean) {
                return true
            }
            for (limit in model.blendFormMapLimits[blend]) {
                if (blendLimits[limit].updated) {
                    return true
                }
            }
        }
        return false
    }

    private fun calcDeformer(deformer: Int): Boolean {
        val state = deformers[deformer]
        if (state.clean) {
            return state.updated
        }
        state.clean = true
        var changed = state.sub == null
        if (paramMapsChanged(model.paramMapSetMaps[model.deformerParamMapSet[deformer]])) {
            changed = true
        }
        val typed = model.deformerTyped[deformer]
        val isWarp = model.deformerIsWarp[deformer]
        val blends = if (isWarp) model.warpBlendFormMaps[typed] else model.rotBlendFormMaps[typed]
        if (blendFormMapsChanged(blends)) {
            changed = true
        }
        var visible = true
        val parent = model.deformerParent[deformer]
        if (parent != -1) {
            if (calcDeformer(parent)) {
                changed = true
            }
            if (!deformers[parent].visible()) {
                visible = false
            }
        }
        val part = model.deformerPart[deformer]
        if (part != -1) {
            calcPart(part)
            visible = visible && parts[part].visibleDeformers
        }
        if (!changed) {
            return false
        }
        if (!visible) {
            state.clean = true
            state.updated = true
            state.sub = null
            return true
        }
        val sub = if (isWarp) calcWarp(deformer, typed, visible) else calcRot(deformer, typed, visible)
        state.clean = true
        state.updated = true
        state.sub = sub
        return true
    }

    private fun calcPart(part: Int): Boolean {
        val state = parts[part]
        if (state.clean) {
            return state.updated
        }
        state.clean = true
        val userOpacity = state.userOpacity
        var changed = state.modified
        val parent = model.partParent[part]
        if (parent != -1) {
            val parentChanged = calcPart(parent)
            changed = changed || parentChanged
        }
        if (!changed) {
            changed = paramMapsChanged(model.paramMapSetMaps[model.partParamMapSet[part]])
        }
        if (!changed) {
            changed = blendFormMapsChanged(model.partBlendFormMaps[part])
        }
        if (!changed) {
            return false
        }
        val newState = PartState(
            exists = true,
            visibleArtmeshes = model.partVisibleArtmeshes[part],
            visibleDeformers = model.partVisibleDeformers[part],
            depth = 0,
            visual = null,
            userOpacity = userOpacity,
            opacity = userOpacity,
            modified = false,
            clean = true,
            updated = false,
        )
        val set = getFormSet(
            model.paramMapSetMaps[model.partParamMapSet[part]],
            model.partForms[part],
            model.partFormCount,
            model.partBlendFormMaps[part],
        )
        if (set != null) {
            if (model.partOffscreen[part]) {
                val values = FloatArray(8)
                for (k in 0 until 8) {
                    var sum = 0f
                    for (i in set.forms.indices) {
                        val form = set.forms[i]
                        val offscreen = model.partFormOffscreen[form]
                        if (offscreen == -1) {
                            throw Live2DDriverException("Offscreen part form without offscreen data")
                        }
                        val value = when (k) {
                            0 -> model.offscreenFormOpacity[offscreen]
                            1 -> model.multiplyR[model.offscreenFormMultiply[offscreen]]
                            2 -> model.multiplyG[model.offscreenFormMultiply[offscreen]]
                            3 -> model.multiplyB[model.offscreenFormMultiply[offscreen]]
                            4 -> model.screenR[model.offscreenFormScreen[offscreen]]
                            5 -> model.screenG[model.offscreenFormScreen[offscreen]]
                            6 -> model.screenB[model.offscreenFormScreen[offscreen]]
                            else -> model.partFormDepth[form]
                        }
                        sum += value * set.weights[i]
                    }
                    values[k] = sum
                }
                newState.depth = depthToInt(values[7])
                val visual = Live2DVisual.fromValues(values)
                visual.visible = model.partVisibleArtmeshes[part]
                newState.visual = visual
            } else {
                newState.depth = depthToInt(blendScalar(set, model.partFormDepth))
            }
        } else {
            newState.depth = 0
            if (model.partOffscreen[part]) {
                newState.visual = Live2DVisual()
            }
        }
        if (parent != -1) {
            parts[parent].apply(newState)
        }
        val current = parts[part]
        if (current.depth != newState.depth) {
            newState.updated = true
            orderChanged = true
        }
        if (current.opacity != newState.opacity) {
            newState.updated = true
        }
        parts[part] = newState
        return newState.updated
    }

    private fun calcParamMap(value: Float, keypoints: Live2DRange, state: ParamMapState) {
        state.clean = false
        state.index = -1
        state.t = 0f
        val kp = model.keypoints
        val count = keypoints.count
        val start = keypoints.start
        if (count == 0) {
            return
        }
        if (count == 1) {
            if (Math.abs(value - kp[start]) <= PARAM_FUDGE) {
                state.index = 0
                state.t = 0f
            }
            return
        }
        val first = kp[start]
        val last = kp[start + count - 1]
        if (value < (first - PARAM_FUDGE) || value > (last + PARAM_FUDGE)) {
            return
        }
        if (value <= first) {
            state.index = 0
            state.t = 0f
            return
        }
        if (value >= last) {
            state.index = count - 1
            state.t = 0f
            return
        }
        for (i in 0 until count - 1) {
            val a = kp[start + i]
            val b = kp[start + i + 1]
            if (value == b) {
                state.index = i + 1
                state.t = 0f
                return
            } else if (value >= a && value < b) {
                if ((value - PARAM_FUDGE) <= a) {
                    state.index = i
                    state.t = 0f
                } else if ((value + PARAM_FUDGE) >= b) {
                    state.index = i + 1
                    state.t = 0f
                } else {
                    state.index = i
                    state.t = (value - a) / (b - a)
                }
                return
            }
        }
    }

    fun drive() {
        orderChanged = false
        for (param in 0 until model.paramCount) {
            val state = params[param]
            if (!state.clean) {
                val value = rustMax(rustMin(state.value, model.paramMax[param]), model.paramMin[param])
                state.value = value
                for (map in model.paramMaps[param]) {
                    calcParamMap(value, model.paramMapKeypoints[map], paramMaps[map])
                }
                for (map in model.paramBlendMaps[param]) {
                    calcParamMap(value, model.blendParamMapKeypoints[map], blendParamMaps[map])
                }
            } else {
                for (map in model.paramMaps[param]) {
                    paramMaps[map].clean = true
                }
                for (map in model.paramBlendMaps[param]) {
                    blendParamMaps[map].clean = true
                }
            }
        }
        for (limit in 0 until model.blendWeightLimitCount) {
            val param = params[model.blendWeightLimitParam[limit]]
            val state = blendLimits[limit]
            if (!param.clean) {
                val old = state.weight
                val points = model.blendWeightLimitPoints[limit]
                if (points.count == 0) {
                    throw Live2DDriverException("Blend weight limit without points")
                }
                state.weight = model.blendWeightLimitPointWeight[points.start + points.count - 1]
                for (i in 0 until points.count - 1) {
                    val aValue = model.blendWeightLimitPointValue[points.start + i]
                    val aWeight = model.blendWeightLimitPointWeight[points.start + i]
                    val bValue = model.blendWeightLimitPointValue[points.start + i + 1]
                    val bWeight = model.blendWeightLimitPointWeight[points.start + i + 1]
                    if (param.value < aValue) {
                        state.weight = aWeight
                        break
                    } else if (param.value >= aValue && param.value < bValue) {
                        val t = (param.value - aValue) / (bValue - aValue)
                        state.weight = aWeight + (bWeight - aWeight) * t
                        break
                    }
                }
                state.updated = old != state.weight
            } else {
                state.updated = false
            }
        }
        for (state in deformers) {
            state.clean = false
            state.updated = false
        }
        for (state in parts) {
            state.clean = false
            state.updated = false
        }
        for (artMesh in 0 until model.artMeshCount) {
            driveArtMesh(artMesh)
        }
        applyGlue()
        if (orderChanged) {
            val partNodes = HashMap<Int, List<DrawNode>>()
            val rootNodes = ArrayList<DrawNode>()
            if (model.drawGroupCount > 0) {
                collectDrawGroup(model.rootDrawGroup, partNodes, rootNodes)
            }
            orderChanged = rootDrawNodes != rootNodes || partDrawNodes != partNodes
            partDrawNodes = partNodes
            rootDrawNodes = rootNodes
        }
        for (param in 0 until model.paramCount) {
            params[param].clean = true
            for (map in model.paramMaps[param]) {
                paramMaps[map].clean = true
            }
        }
    }

    private fun driveArtMesh(artMesh: Int) {
        val state = artMeshes[artMesh]
        var changed = !state.initialized
        state.updated = false
        state.initialized = true
        val oldDepth = state.depth
        var partOpacity = 1f
        var visible = model.artMeshVertexCount[artMesh] != 0 && model.artMeshIndices[artMesh].count != 0
        val deformer = model.artMeshDeformer[artMesh]
        if (deformer != -1) {
            val deformerChanged = calcDeformer(deformer)
            changed = changed || deformerChanged
            visible = visible && deformers[deformer].visible()
        }
        val part = model.artMeshPart[artMesh]
        if (part != -1) {
            val partChanged = calcPart(part)
            changed = changed || partChanged
            visible = visible && parts[part].visibleArtmeshes
            partOpacity = parts[part].opacity
        }
        val maps = model.paramMapSetMaps[model.artMeshParamMapSet[artMesh]]
        if (visible && !changed) {
            changed = paramMapsChanged(maps)
        }
        if (visible && !changed) {
            changed = blendFormMapsChanged(model.artMeshBlendFormMaps[artMesh])
        }
        if (!changed) {
            return
        }
        if (!visible) {
            artMeshes[artMesh] = ArtMeshState(initialized = true, updated = true)
            return
        }
        val set = getFormSet(maps, model.artMeshForms[artMesh], model.artMeshFormCount, model.artMeshBlendFormMaps[artMesh])
        if (set == null) {
            artMeshes[artMesh] = ArtMeshState(initialized = true, updated = true, depth = oldDepth)
            return
        }
        val values = blendVisual(
            set,
            model.artMeshFormOpacity,
            model.artMeshFormMultiply,
            model.artMeshFormScreen,
            1,
        ) { _, form -> model.artMeshFormDepth[form] }
        val vertexCount = model.artMeshVertexCount[artMesh]
        val vertices = blendVertices(set, model.artMeshFormStartVertex, vertexCount, vertexCount)
        val visual = Live2DVisual.fromValues(values)
        visual.visible = model.artMeshVisible[artMesh]
        if (deformer != -1) {
            deformers[deformer].sub?.apply(vertices, vertexCount, visual)
                ?: throw Live2DDriverException("Deformer has no state")
        }
        val depth = depthToInt(values[7])
        if (depth != oldDepth) {
            orderChanged = true
        }
        visual.opacity *= partOpacity
        artMeshes[artMesh] = ArtMeshState(
            initialized = true,
            updated = true,
            visual = visual,
            depth = depth,
            vertices = vertices,
            gluedVertices = null,
        )
    }

    private fun applyGlue() {
        val count = model.glueCount
        if (count == 0) {
            return
        }
        val order = IntArray(2 * count) { if (it < count) it else 2 * count - 1 - it }
        while (true) {
            var progress = false
            for (glue in order) {
                val first = model.glueArtMesh1[glue]
                val second = model.glueArtMesh2[glue]
                if (artMeshes[first].updated) {
                    val state = artMeshes[second]
                    if (!state.updated) {
                        progress = true
                        state.updated = true
                    }
                    state.gluedVertices = null
                }
                if (artMeshes[second].updated) {
                    val state = artMeshes[first]
                    if (!state.updated) {
                        progress = true
                        state.updated = true
                    }
                    state.gluedVertices = null
                }
            }
            if (!progress) {
                break
            }
        }
        glue@ for (glue in 0 until count) {
            val first = model.glueArtMesh1[glue]
            val second = model.glueArtMesh2[glue]
            var updated = false
            for (uid in intArrayOf(first, second)) {
                val state = artMeshes[uid]
                if (state.vertices.isEmpty()) {
                    continue@glue
                }
                if (state.gluedVertices == null) {
                    state.gluedVertices = state.vertices.copyOf()
                }
                updated = updated || state.updated
            }
            if (!updated) {
                continue
            }
            val vertices1 = artMeshes[first].gluedVertices ?: throw Live2DDriverException("Missing glued vertices")
            artMeshes[first].gluedVertices = null
            val vertices2 = artMeshes[second].gluedVertices ?: throw Live2DDriverException("Missing glued vertices")
            artMeshes[second].gluedVertices = null
            val set = getFormSet(
                model.paramMapSetMaps[model.glueParamMapSet[glue]],
                model.glueForms[glue],
                model.glueFormCount,
                model.glueBlendFormMaps[glue],
            ) ?: continue
            val compatibility = blendScalar(set, model.glueFormCompatibility)
            val coords = model.glueCoords[glue]
            var i = 0
            while (i + 1 < coords.count) {
                val weight1 = model.glueCoordWeight[coords.start + i]
                val index1 = model.glueCoordVertexIndex[coords.start + i]
                val weight2 = model.glueCoordWeight[coords.start + i + 1]
                val index2 = model.glueCoordVertexIndex[coords.start + i + 1]
                if (2 * index1 + 1 >= vertices1.size || 2 * index2 + 1 >= vertices2.size) {
                    throw Live2DDriverException("Glue vertex index out of range")
                }
                val v1x = vertices1[2 * index1]
                val v1y = vertices1[2 * index1 + 1]
                val v2x = vertices2[2 * index2]
                val v2y = vertices2[2 * index2 + 1]
                val vg1x = v1x * (1f - weight1) + v2x * weight1
                val vg1y = v1y * (1f - weight1) + v2y * weight1
                val vg2x = v2x * (1f - weight2) + v1x * weight2
                val vg2y = v2y * (1f - weight2) + v1y * weight2
                vertices1[2 * index1] = v1x * (1f - compatibility) + vg1x * compatibility
                vertices1[2 * index1 + 1] = v1y * (1f - compatibility) + vg1y * compatibility
                vertices2[2 * index2] = v2x * (1f - compatibility) + vg2x * compatibility
                vertices2[2 * index2 + 1] = v2y * (1f - compatibility) + vg2y * compatibility
                i += 2
            }
            artMeshes[first].gluedVertices = vertices1
            artMeshes[second].gluedVertices = vertices2
        }
    }

    private fun collectDrawGroup(group: Int, partNodes: HashMap<Int, List<DrawNode>>, nodes: MutableList<DrawNode>) {
        val range = model.drawGroupItems[group]
        val items = (0 until range.count).map { range.start + it }
        val sorted = items.sortedBy { item ->
            val child = model.drawItemChild[item]
            val depth = if (model.drawItemIsPart[item]) parts[child].depth else artMeshes[child].depth
            depth.toLong() and 0xffffffffL
        }
        for (item in sorted) {
            val child = model.drawItemChild[item]
            if (!model.drawItemIsPart[item]) {
                nodes.add(DrawNode.ArtMesh(child))
            } else {
                val subGroup = model.drawItemGroup[item]
                if (model.partOffscreen[child]) {
                    val branch = ArrayList<DrawNode>()
                    collectDrawGroup(subGroup, partNodes, branch)
                    partNodes[child] = branch
                    nodes.add(DrawNode.OffscreenPart(child))
                } else {
                    collectDrawGroup(subGroup, partNodes, nodes)
                }
            }
        }
    }

    fun artMeshState(uid: Int): DrivenArtMesh? {
        val state = artMeshes.getOrNull(uid) ?: return null
        if (!state.initialized) {
            return null
        }
        return DrivenArtMesh(state.visual, state.gluedVertices ?: state.vertices)
    }

    fun drawNodes(part: Int?): List<DrawNode>? {
        return if (part == null) rootDrawNodes else partDrawNodes[part]
    }
}
