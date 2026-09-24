package com.moblin.android.platform.live2d

internal class Live2DRange(val start: Int, val count: Int)

internal class Live2DModelData(file: Moc3File) {
    val canvas: Moc3Canvas = file.canvas

    val keypoints: FloatArray = file.table("Keypoint").primitiveFloats
    val vertexCoords: FloatArray = file.table("VertexCoord").primitiveFloats
    val texCoords: FloatArray = file.table("TexCoord").primitiveFloats
    val vertexIndices: ShortArray = file.table("VertexIndex").primitiveShorts

    val paramCount: Int
    val paramIds: Array<String>
    val paramMin: FloatArray
    val paramMax: FloatArray
    val paramDefault: FloatArray
    val paramMaps: Array<IntArray>
    val paramBlendMaps: Array<IntArray>

    val paramMapCount: Int
    val paramMapKeypoints: Array<Live2DRange>

    val blendParamMapCount: Int
    val blendParamMapKeypoints: Array<Live2DRange>
    val blendParamMapNeutral: IntArray

    val paramMapSetMaps: Array<IntArray>

    val partCount: Int
    val partIds: Array<String>
    val partParamMapSet: IntArray
    val partForms: Array<Live2DRange>
    val partVisibleArtmeshes: BooleanArray
    val partVisibleDeformers: BooleanArray
    val partParent: IntArray
    val partOffscreen: BooleanArray
    val partBlendFormMaps: Array<IntArray?>

    val partFormCount: Int
    val partFormDepth: FloatArray
    val partFormOffscreen: IntArray
    val offscreenFormOpacity: FloatArray
    val offscreenFormMultiply: IntArray
    val offscreenFormScreen: IntArray

    val deformerCount: Int
    val deformerIds: Array<String>
    val deformerParamMapSet: IntArray
    val deformerVisible: BooleanArray
    val deformerPart: IntArray
    val deformerParent: IntArray
    val deformerIsWarp: BooleanArray
    val deformerTyped: IntArray

    val warpCount: Int
    val warpParamMapSet: IntArray
    val warpForms: Array<Live2DRange>
    val warpFileVertexCount: IntArray
    val warpXDivs: IntArray
    val warpYDivs: IntArray
    val warpBilinear: BooleanArray
    val warpBlendFormMaps: Array<IntArray?>

    val warpFormCount: Int
    val warpFormOpacity: FloatArray
    val warpFormStartVertex: IntArray
    val warpFormMultiply: IntArray
    val warpFormScreen: IntArray

    val rotCount: Int
    val rotParamMapSet: IntArray
    val rotForms: Array<Live2DRange>
    val rotAngleOffset: FloatArray
    val rotBlendFormMaps: Array<IntArray?>

    val rotFormCount: Int
    val rotFormOpacity: FloatArray
    val rotFormAngle: FloatArray
    val rotFormPosX: FloatArray
    val rotFormPosY: FloatArray
    val rotFormScale: FloatArray
    val rotFormFlipX: BooleanArray
    val rotFormFlipY: BooleanArray
    val rotFormMultiply: IntArray
    val rotFormScreen: IntArray

    val artMeshCount: Int
    val artMeshIds: Array<String>
    val artMeshParamMapSet: IntArray
    val artMeshForms: Array<Live2DRange>
    val artMeshVisible: BooleanArray
    val artMeshPart: IntArray
    val artMeshDeformer: IntArray
    val artMeshTexture: IntArray
    val artMeshRenderConfig: IntArray
    val artMeshVertexCount: IntArray
    val artMeshTexcoordStart: IntArray
    val artMeshIndices: Array<Live2DRange>
    val artMeshClips: Array<IntArray>
    val artMeshBlendColor: IntArray
    val artMeshBlendAlpha: IntArray
    val artMeshBlendFormMaps: Array<IntArray?>

    val artMeshFormCount: Int
    val artMeshFormOpacity: FloatArray
    val artMeshFormDepth: FloatArray
    val artMeshFormStartVertex: IntArray
    val artMeshFormMultiply: IntArray
    val artMeshFormScreen: IntArray

    val multiplyR: FloatArray
    val multiplyG: FloatArray
    val multiplyB: FloatArray
    val screenR: FloatArray
    val screenG: FloatArray
    val screenB: FloatArray

    val drawGroupCount: Int
    val drawGroupItems: Array<Live2DRange>
    val drawItemIsPart: BooleanArray
    val drawItemChild: IntArray
    val drawItemGroup: IntArray
    val rootDrawGroup: Int

    val glueCount: Int
    val glueParamMapSet: IntArray
    val glueForms: Array<Live2DRange>
    val glueArtMesh1: IntArray
    val glueArtMesh2: IntArray
    val glueCoords: Array<Live2DRange>
    val glueBlendFormMaps: Array<IntArray?>
    val glueFormCompatibility: FloatArray
    val glueFormCount: Int
    val glueCoordWeight: FloatArray
    val glueCoordVertexIndex: IntArray

    val blendFormMapCount: Int
    val blendFormMapParamMap: IntArray
    val blendFormMapFormsStart: IntArray
    val blendFormMapFormsCount: IntArray
    val blendFormMapLimits: Array<IntArray>

    val blendWeightLimitCount: Int
    val blendWeightLimitParam: IntArray
    val blendWeightLimitPoints: Array<Live2DRange>
    val blendWeightLimitPointValue: FloatArray
    val blendWeightLimitPointWeight: FloatArray

    init {
        val param = file.table("Param")
        paramCount = param.rawCount
        paramIds = param.strings.getValue("id")
        paramMin = param.float("min")
        paramMax = param.float("max")
        paramDefault = param.float("default")
        paramMaps = ranges(param, "maps").map { rangeIndices(it) }.toTypedArray()
        paramBlendMaps = ranges(param, "blend_maps").map { rangeIndices(it) }.toTypedArray()

        val paramMap = file.table("ParamMap")
        paramMapCount = paramMap.rawCount
        paramMapKeypoints = ranges(paramMap, "keypoints")

        val blendParamMap = file.table("BlendParamMap")
        blendParamMapCount = blendParamMap.rawCount
        blendParamMapKeypoints = ranges(blendParamMap, "keypoints")
        blendParamMapNeutral = blendParamMap.int("neutral_index")

        val paramMapSet = file.table("ParamMapSet")
        val paramMapRefMaps = file.table("ParamMapRef").int("map")
        paramMapSetMaps = ranges(paramMapSet, "refs").map { range ->
            IntArray(range.count) { paramMapRefMaps[range.start + it] }
        }.toTypedArray()

        val multiply = file.table("MultiplyColor")
        multiplyR = multiply.float("r")
        multiplyG = multiply.float("g")
        multiplyB = multiply.float("b")
        val screen = file.table("ScreenColor")
        screenR = screen.float("r")
        screenG = screen.float("g")
        screenB = screen.float("b")

        val blendFormMap = file.table("BlendFormMap")
        blendFormMapCount = blendFormMap.rawCount
        blendFormMapParamMap = blendFormMap.int("param_map")
        blendFormMapFormsStart = blendFormMap.int("i_forms")
        blendFormMapFormsCount = blendFormMap.int("cnt_forms")
        val limitRefs = file.table("BlendWeightLimitRef").int("limit")
        blendFormMapLimits = ranges(blendFormMap, "blendweight_limits").map { range ->
            IntArray(range.count) { limitRefs[range.start + it] }
        }.toTypedArray()

        val blendWeightLimit = file.table("BlendWeightLimit")
        blendWeightLimitCount = blendWeightLimit.rawCount
        blendWeightLimitParam = blendWeightLimit.int("param")
        blendWeightLimitPoints = ranges(blendWeightLimit, "points")
        val point = file.table("BlendWeightLimitPoint")
        blendWeightLimitPointValue = point.float("value")
        blendWeightLimitPointWeight = point.float("weight")

        val part = file.table("Part")
        partCount = part.rawCount
        partIds = part.strings.getValue("id")
        partParamMapSet = part.int("param_map_set")
        partForms = ranges(part, "forms")
        partVisibleArtmeshes = booleans(part.int("visible_artmeshes"))
        partVisibleDeformers = booleans(part.int("visible_deformers"))
        partParent = part.int("parent")
        partOffscreen = BooleanArray(partCount) { part.int("offscreen_part")[it] != -1 }
        partBlendFormMaps = blendFormMaps(file, part, "PartBlendFormMaps")

        val partForm = file.table("PartForm")
        partFormCount = partForm.rawCount
        partFormDepth = partForm.float("depth")
        partFormOffscreen = partForm.int("offscreen")
        val offscreenForm = file.table("OffscreenPartForm")
        offscreenFormOpacity = offscreenForm.float("opacity")
        offscreenFormMultiply = offscreenForm.int("multiply_color")
        offscreenFormScreen = offscreenForm.int("screen_color")

        val deformer = file.table("Deformer")
        deformerCount = deformer.rawCount
        deformerIds = deformer.strings.getValue("id")
        deformerParamMapSet = deformer.int("param_map_set")
        deformerVisible = booleans(deformer.int("visible"))
        deformerPart = deformer.int("part")
        deformerParent = deformer.int("parent")
        deformerIsWarp = BooleanArray(deformerCount) { deformer.int("deformer_type")[it] == 0 }
        deformerTyped = deformer.int("i_typed")

        val warp = file.table("WarpDeformer")
        warpCount = warp.rawCount
        warpParamMapSet = warp.int("param_map_set")
        warpForms = ranges(warp, "forms")
        warpFileVertexCount = warp.int("vertex_count")
        warpXDivs = warp.int("x_divs")
        warpYDivs = warp.int("y_divs")
        warpBilinear = booleans(warp.int("bilinear_interpolation"))
        warpBlendFormMaps = blendFormMaps(file, warp, "WarpBlendFormMaps")

        val warpForm = file.table("WarpForm")
        warpFormCount = warpForm.rawCount
        warpFormOpacity = warpForm.float("opacity")
        warpFormStartVertex = warpForm.int("start_vertex")
        warpFormMultiply = warpForm.int("multiply_color")
        warpFormScreen = warpForm.int("screen_color")

        val rot = file.table("RotDeformer")
        rotCount = rot.rawCount
        rotParamMapSet = rot.int("param_map_set")
        rotForms = ranges(rot, "forms")
        rotAngleOffset = rot.float("angle_offset")
        rotBlendFormMaps = blendFormMaps(file, rot, "RotBlendFormMaps")

        val rotForm = file.table("RotForm")
        rotFormCount = rotForm.rawCount
        rotFormOpacity = rotForm.float("opacity")
        rotFormAngle = rotForm.float("angle")
        rotFormPosX = rotForm.float("pos_x")
        rotFormPosY = rotForm.float("pos_y")
        rotFormScale = rotForm.float("scale")
        rotFormFlipX = booleans(rotForm.int("flip_x"))
        rotFormFlipY = booleans(rotForm.int("flip_y"))
        rotFormMultiply = rotForm.int("multiply_color")
        rotFormScreen = rotForm.int("screen_color")

        val artMesh = file.table("ArtMesh")
        artMeshCount = artMesh.rawCount
        artMeshIds = artMesh.strings.getValue("id")
        artMeshParamMapSet = artMesh.int("param_map_set")
        artMeshForms = ranges(artMesh, "forms")
        artMeshVisible = booleans(artMesh.int("visible"))
        artMeshPart = artMesh.int("part")
        artMeshDeformer = artMesh.int("deformer")
        artMeshTexture = artMesh.int("texture")
        artMeshRenderConfig = artMesh.int("render_config")
        artMeshVertexCount = artMesh.int("vertex_count")
        artMeshTexcoordStart = artMesh.int("texcoord_start")
        artMeshIndices = ranges(artMesh, "indices")
        val artMeshRefs = file.table("ArtMeshRef").int("artmesh")
        artMeshClips = ranges(artMesh, "clips").map { range ->
            (0 until range.count).map { artMeshRefs[range.start + it] }.filter { it != -1 }.toIntArray()
        }.toTypedArray()
        artMeshBlendColor = artMesh.int("blend_config_color")
        artMeshBlendAlpha = artMesh.int("blend_config_alpha")
        artMeshBlendFormMaps = blendFormMaps(file, artMesh, "ArtMeshBlendFormMaps")

        val artMeshForm = file.table("ArtMeshForm")
        artMeshFormCount = artMeshForm.rawCount
        artMeshFormOpacity = artMeshForm.float("opacity")
        artMeshFormDepth = artMeshForm.float("depth")
        artMeshFormStartVertex = artMeshForm.int("start_vertex")
        artMeshFormMultiply = artMeshForm.int("multiply_color")
        artMeshFormScreen = artMeshForm.int("screen_color")

        val drawGroup = file.table("DrawGroup")
        drawGroupCount = drawGroup.rawCount
        drawGroupItems = ranges(drawGroup, "items")
        val drawItem = file.table("DrawItem")
        drawItemIsPart = BooleanArray(drawItem.rawCount) { drawItem.int("item_type")[it] == 1 }
        drawItemChild = drawItem.int("i_child")
        drawItemGroup = drawItem.int("draw_group")
        rootDrawGroup = 0

        val glue = file.table("Glue")
        glueCount = glue.rawCount
        glueParamMapSet = glue.int("param_map_set")
        glueForms = ranges(glue, "forms")
        glueArtMesh1 = glue.int("artmesh_1")
        glueArtMesh2 = glue.int("artmesh_2")
        glueCoords = ranges(glue, "coords")
        glueBlendFormMaps = blendFormMaps(file, glue, "GlueBlendFormMaps")
        val glueForm = file.table("GlueForm")
        glueFormCount = glueForm.rawCount
        glueFormCompatibility = glueForm.float("compatibility")
        val glueCoord = file.table("GlueCoord")
        glueCoordWeight = glueCoord.float("weight")
        glueCoordVertexIndex = glueCoord.int("vertex_index")
    }

    fun culling(artMesh: Int): Boolean {
        return artMeshRenderConfig[artMesh] and RENDER_DOUBLE_SIDED == 0
    }

    fun invertMask(artMesh: Int): Boolean {
        return artMeshRenderConfig[artMesh] and RENDER_INVERT_MASK != 0
    }

    fun simpleBlendMode(artMesh: Int): Int? {
        val color = artMeshBlendColor[artMesh]
        val alpha = artMeshBlendAlpha[artMesh]
        val advanced = when (color) {
            0 -> alpha != 0
            1, 2 -> false
            else -> true
        }
        return if (advanced) null else color
    }

    fun texcoordOffset(artMesh: Int): Int {
        return artMeshTexcoordStart[artMesh] / 2
    }

    companion object {
        const val RENDER_INVERT_MASK = 0x8
        const val RENDER_DOUBLE_SIDED = 0x4
    }
}

private fun ranges(table: Moc3Table, field: String): Array<Live2DRange> {
    val starts = table.int("i_$field")
    val counts = table.int("cnt_$field")
    return Array(table.rawCount) { Live2DRange(starts[it], counts[it]) }
}

private fun rangeIndices(range: Live2DRange): IntArray {
    return IntArray(range.count) { range.start + it }
}

private fun booleans(values: IntArray): BooleanArray {
    return BooleanArray(values.size) { values[it] != 0 }
}

private fun blendFormMaps(file: Moc3File, owner: Moc3Table, mapsName: String): Array<IntArray?> {
    val back = owner.int("blend_form_maps")
    val maps = file.table(mapsName)
    val starts = maps.int("i_maps")
    val counts = maps.int("cnt_maps")
    return Array(owner.rawCount) { index ->
        val mapsIndex = back[index]
        if (mapsIndex == -1) {
            null
        } else {
            IntArray(counts[mapsIndex]) { starts[mapsIndex] + it }
        }
    }
}
