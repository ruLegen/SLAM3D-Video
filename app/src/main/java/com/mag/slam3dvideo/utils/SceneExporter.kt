package com.mag.slam3dvideo.utils

import android.util.Log
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
import de.javagl.jgltf.model.AccessorDatas
import de.javagl.jgltf.model.ElementType
import de.javagl.jgltf.model.GltfConstants
import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.creation.AccessorModels
import de.javagl.jgltf.model.creation.GltfModelBuilder
import de.javagl.jgltf.model.creation.MeshPrimitiveBuilder
import de.javagl.jgltf.model.impl.DefaultAccessorModel
import de.javagl.jgltf.model.impl.DefaultMeshModel
import de.javagl.jgltf.model.impl.DefaultNodeModel
import de.javagl.jgltf.model.impl.DefaultSceneModel
import de.javagl.jgltf.model.io.GltfModelWriter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder




object SceneExporter {

    fun export(sceneContext: SceneContext) {
        val scene = DefaultSceneModel()
        val nodes: List<DefaultNodeModel> = getNodesFromObjects(sceneContext)
        nodes.forEach {
            scene.addNode(it)
        }

        val gltfModelBuilder = GltfModelBuilder.create()
        gltfModelBuilder.addSceneModel(scene)
        val gltfModel: GltfModel? = gltfModelBuilder.build()

        val outFile = File.createTempFile("gltf_", ".gltf")
        Log.d("DEBUG", outFile.absolutePath);
        val gltfModelWriter = GltfModelWriter()
        gltfModelWriter.writeEmbedded(gltfModel, outFile)
    }

    private fun getNodesFromObjects(context: SceneContext): List<DefaultNodeModel> {
        val sceneObjects = context.sceneObjectContainer.objects
        val nodes = sceneObjects.map { obj -> getNode(context, null, obj) }
        return nodes
    }

    private fun getNode(context: SceneContext,parentNode: DefaultNodeModel?,obj: SceneObject): DefaultNodeModel {
        val node = DefaultNodeModel()
        node.setParent(parentNode)
        node.name = obj.name
        node.matrix = obj.transformComponent.getTransform(context)

        // what if object has multiple mesh renderers?
        val meshRenderer = obj.getComponent<MeshRendererComponent>()
        val objMesh = meshRenderer?.getMesh()
        if (objMesh != null) {
            val vertexCount  = objMesh.vertexCount
            val vertexData = objMesh.getVertexDataBuffer()
//            val indexBuffer = objMesh.getIndexDataBuffer()
            val indexBuffer = ByteBuffer.allocateDirect(vertexCount*Short.SIZE_BYTES)
            (0 until vertexCount).map {
                indexBuffer.putShort(it.toShort())
            }
            val attributes = objMesh.attributes
            val gltfMesh = DefaultMeshModel()
            val meshPrimitiveBuilder = MeshPrimitiveBuilder.create()
            meshPrimitiveBuilder.setPoints()
//            when (meshRenderer.primitiveType) {
//                RenderableManager.PrimitiveType.POINTS -> meshPrimitiveBuilder.setPoints()
//                RenderableManager.PrimitiveType.LINES -> meshPrimitiveBuilder.setLines()
//                RenderableManager.PrimitiveType.TRIANGLES -> meshPrimitiveBuilder.setTriangles()
//                else -> {}
//            }
            val indicesAccessor = AccessorModels.create(GltfConstants.GL_UNSIGNED_SHORT,ElementType.SCALAR.name,false,indexBuffer as ByteBuffer)
            meshPrimitiveBuilder.setIndices(indicesAccessor)
            attributes.forEach {
                val name = it.attribute.name
                val componentType = it.type.toGlTFComponentType()
                val elementType = it.type.toGlTFElementType()
                if(name == "COLOR")
                    return@forEach
                val accessorModel = DefaultAccessorModel(componentType, vertexCount, elementType)
                accessorModel.byteStride = it.stride
                accessorModel.byteOffset = it.offset
                accessorModel.isNormalized = false
                accessorModel.accessorData = AccessorDatas.create(accessorModel, vertexData as ByteBuffer)
                meshPrimitiveBuilder.addAttribute(name, accessorModel)
            }

            val primitives = meshPrimitiveBuilder.build()
            gltfMesh.addMeshPrimitiveModel(primitives)
            node.addMeshModel(gltfMesh)
        }


        val children = obj.children.map {
            getNode(context, node, it)
        }
        children.forEach {
            node.addChild(it)
        }
        return node
    }
}

private fun VertexBuffer.AttributeType.toGlTFElementType(): ElementType {
    return when (this) {
        VertexBuffer.AttributeType.BYTE,
        VertexBuffer.AttributeType.UBYTE,
        VertexBuffer.AttributeType.SHORT,
        VertexBuffer.AttributeType.USHORT,
        VertexBuffer.AttributeType.INT,
        VertexBuffer.AttributeType.UINT,
        VertexBuffer.AttributeType.FLOAT,
        VertexBuffer.AttributeType.HALF -> ElementType.SCALAR

        VertexBuffer.AttributeType.BYTE2,
        VertexBuffer.AttributeType.SHORT2,
        VertexBuffer.AttributeType.UBYTE2,
        VertexBuffer.AttributeType.USHORT2,
        VertexBuffer.AttributeType.FLOAT2,
        VertexBuffer.AttributeType.HALF2 -> ElementType.VEC2

        VertexBuffer.AttributeType.BYTE3,
        VertexBuffer.AttributeType.UBYTE3,
        VertexBuffer.AttributeType.SHORT3,
        VertexBuffer.AttributeType.USHORT3,
        VertexBuffer.AttributeType.FLOAT3,
        VertexBuffer.AttributeType.HALF3 -> ElementType.VEC3

        VertexBuffer.AttributeType.BYTE4,
        VertexBuffer.AttributeType.UBYTE4,
        VertexBuffer.AttributeType.SHORT4,
        VertexBuffer.AttributeType.USHORT4,
        VertexBuffer.AttributeType.FLOAT4,
        VertexBuffer.AttributeType.HALF4 -> ElementType.VEC4
    }
}

private fun VertexBuffer.AttributeType.toGlTFComponentType(): Int {
    return when (this) {
        VertexBuffer.AttributeType.BYTE,
        VertexBuffer.AttributeType.BYTE2,
        VertexBuffer.AttributeType.BYTE3,
        VertexBuffer.AttributeType.BYTE4 -> GltfConstants.GL_BYTE

        VertexBuffer.AttributeType.UBYTE,
        VertexBuffer.AttributeType.UBYTE2,
        VertexBuffer.AttributeType.UBYTE3,
        VertexBuffer.AttributeType.UBYTE4 -> GltfConstants.GL_UNSIGNED_BYTE

        VertexBuffer.AttributeType.SHORT,
        VertexBuffer.AttributeType.SHORT2,
        VertexBuffer.AttributeType.SHORT3,
        VertexBuffer.AttributeType.SHORT4 -> GltfConstants.GL_SHORT

        VertexBuffer.AttributeType.USHORT,
        VertexBuffer.AttributeType.USHORT2,
        VertexBuffer.AttributeType.USHORT3,
        VertexBuffer.AttributeType.USHORT4 -> GltfConstants.GL_UNSIGNED_SHORT

        VertexBuffer.AttributeType.INT -> GltfConstants.GL_INT
        VertexBuffer.AttributeType.UINT -> GltfConstants.GL_UNSIGNED_INT
        VertexBuffer.AttributeType.FLOAT,
        VertexBuffer.AttributeType.FLOAT2,
        VertexBuffer.AttributeType.FLOAT3,
        VertexBuffer.AttributeType.FLOAT4 -> GltfConstants.GL_FLOAT

        VertexBuffer.AttributeType.HALF -> TODO()
        VertexBuffer.AttributeType.HALF2 -> TODO()
        VertexBuffer.AttributeType.HALF3 -> TODO()
        VertexBuffer.AttributeType.HALF4 -> TODO()
    }
}