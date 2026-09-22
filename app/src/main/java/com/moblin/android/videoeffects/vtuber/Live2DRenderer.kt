package com.moblin.android.videoeffects.vtuber

import android.media.Image

private val shaderSource = """
#include <metal_stdlib>

using namespace metal;

constant bool live2DUseMask [[function_constant(0)]];

struct Live2DVertexUniforms {
    float2 scale;
    float2 offset;
};

struct Live2DFragmentUniforms {
    float3 multiplyColor;
    float opacity;
    float3 screenColor;
    uint invertMask;
};

struct Live2DVaryings {
    float4 position [[position]];
    float2 textureCoordinate;
};

vertex Live2DVaryings live2DVertex(uint vertexId [[vertex_id]],
                                   const device float2 *positions [[buffer(0)]],
                                   const device float2 *textureCoordinates [[buffer(1)]],
                                   constant Live2DVertexUniforms &uniforms [[buffer(2)]])
{
    Live2DVaryings out;
    out.position = float4(positions[vertexId] * uniforms.scale + uniforms.offset, 0, 1);
    out.textureCoordinate = textureCoordinates[vertexId];
    return out;
}

constexpr sampler live2DSampler(filter::linear, mip_filter::linear, address::clamp_to_edge);

fragment float4 live2DFragment(Live2DVaryings in [[stage_in]],
                               texture2d<float> texture [[texture(0)]],
                               texture2d<float> mask [[texture(1), function_constant(live2DUseMask)]],
                               constant Live2DFragmentUniforms &uniforms [[buffer(0)]])
{
    float4 color = texture.sample(live2DSampler, in.textureCoordinate);
    color.rgb *= color.a;
    color.rgb *= uniforms.multiplyColor;
    color.rgb = color.a - (color.a - color.rgb) * (1 - uniforms.screenColor);
    float maskValue = 1;
    if (live2DUseMask) {
        float2 maskCoordinate = in.position.xy / float2(mask.get_width(), mask.get_height());
        maskValue = mask.sample(live2DSampler, maskCoordinate).r;
        if (uniforms.invertMask != 0) {
            maskValue = 1 - maskValue;
        }
    }
    return saturate(color) * uniforms.opacity * maskValue;
}

fragment float4 live2DMaskFragment(Live2DVaryings in [[stage_in]],
                                   texture2d<float> texture [[texture(0)]])
{
    return float4(texture.sample(live2DSampler, in.textureCoordinate).a);
}
"""

private val library: Any? = TODO("OpenGL ES port: Metal shader library")

private data class Live2DVertexUniforms(
    val scale: FloatArray,
    val offset: FloatArray,
)

private data class Live2DFragmentUniforms(
    val multiplyColor: FloatArray,
    val opacity: Float,
    val screenColor: FloatArray,
    val invertMask: Int,
)

private data class Live2DPipelineKey(
    val blendMode: Any,
    val masked: Boolean,
)

private data class Live2DArtMesh(
    val info: Any,
    val clipSet: Int?,
)

private class Live2DClipSet(
    val targets: List<Int>,
    var texture: Any?,
)

class Live2DRenderer private constructor(
    private val device: Any,
    private val commandQueue: Any,
    private val textures: List<Any>,
    private val indexBuffer: Any,
    private val texcoordBuffer: Any,
    private val vertexBuffer: Any,
    private val artMeshes: List<Live2DArtMesh>,
    private var clipSets: MutableList<Live2DClipSet>,
    private val maskPipeline: Any,
    private var vertexUniforms: Live2DVertexUniforms,
) {
    private var pipelines: MutableMap<Live2DPipelineKey, Any> = mutableMapOf()
    private var textureCache: Any? = null

    companion object {
        fun create(model: Any): Live2DRenderer? =
            TODO("OpenGL ES port: Ayagami model access, Metal device, command queue, textures and render pipelines")

        private fun makePipeline(
            device: Any,
            library: Any,
            blendMode: Any,
            masked: Boolean,
        ): Any = TODO("OpenGL ES port: Metal render pipeline state")

        private fun makeMaskPipeline(
            device: Any,
            library: Any,
        ): Any = TODO("OpenGL ES port: Metal mask render pipeline state")
    }

    private fun makeClipTexture(width: Int, height: Int): Any? =
        TODO("OpenGL ES port: Metal clip texture")

    private fun makeOutputTexture(pixelBuffer: Image): Any? =
        TODO("OpenGL ES port: CVMetalTextureCache output texture")

    fun render(model: Any, pixelBuffer: Image) {
        TODO("OpenGL ES port")
    }

    private fun renderClipSet(
        index: Int,
        states: List<Any?>,
        commandBuffer: Any,
        width: Int,
        height: Int,
    ) {
        TODO("OpenGL ES port")
    }

    private fun draw(encoder: Any, artMesh: Any) {
        TODO("OpenGL ES port")
    }
}
