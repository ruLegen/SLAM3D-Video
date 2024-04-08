package com.mag.slam3dvideo.resources

import android.os.Handler
import android.os.Looper
import com.google.android.filament.Material
import com.google.android.filament.filamat.MaterialBuilder
import com.mag.slam3dvideo.render.SceneContext
import java.nio.ByteBuffer

object StaticMaterials {
    // todo add SceneContext as one more dimension
    private val compiledMaterials  = HashMap<String,Material>()

    fun getMeshMetal(context: SceneContext): Material {
        val name = "mesh_material"
        if(compiledMaterials.containsKey(name))
            return compiledMaterials[name]!!

        val mat = MaterialBuilder()
            .name(name)
            .require(MaterialBuilder.VertexAttribute.COLOR)
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .culling(MaterialBuilder.CullingMode.NONE)
            .material(
                """
               void material(inout MaterialInputs material) {
                   prepareMaterial(material);
                   material.baseColor =  getColor();
               }
            """
            )
            .build()
        val materialBuffer =  mat.buffer
        val material = Material.Builder().payload(materialBuffer, materialBuffer.remaining()).build(context.engine)
        material.compile(
            Material.CompilerPriorityQueue.HIGH,
            Material.UserVariantFilterBit.ALL,
            Handler(Looper.getMainLooper())
        ) {
            android.util.Log.i(
                "MATERIAL_COMPILER",
                "Material " + material.name + " compiled."
            )
        }
        compiledMaterials[name] = material
        context.engine.flush()
        return material
    }
}