package com.mag.slam3dvideo.render.components

import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import com.mag.slam3dvideo.render.Mesh
import com.mag.slam3dvideo.render.SceneContext

data class MeshBuffers(val vertexBuffer: VertexBuffer,val indexBuffer: IndexBuffer){
    fun destroy(context: SceneContext){
        val engine = context.engine
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
    }
}
class MeshRendererComponent : ObjectComponent() {
    var primitiveType:PrimitiveType = PrimitiveType.TRIANGLES
    private var buffers: MeshBuffers? = null
    private var shouldUpdateMaterialInstance: Boolean = false
    private var shouldUpdateMesh: Boolean = false
    var renderable: Int = -1
        private set
    private var mesh:Mesh? = null
    var materialInstance:MaterialInstance? = null
        private set
    override fun start(context: SceneContext) {
        renderable = parent!!.entity
        RenderableManager.Builder(1)
            .culling(false)
            .build(context.engine, renderable)

        // in order to be able renderable, enitity must be added to a fillament scene
        context.scene.addEntity(renderable)
    }

    override fun update(context: SceneContext) {
        val needUpdateMesh = shouldUpdateMesh
        val needUpdateMatInst = shouldUpdateMaterialInstance
        if(!needUpdateMesh && !needUpdateMatInst)
            return
        shouldUpdateMesh =false
        shouldUpdateMaterialInstance = false

        val rcm = context.engine.renderableManager
        val entity = rcm.getInstance(renderable)

        if(needUpdateMesh && mesh != null){
            updateMesh(context,mesh!!)
        }

        if(needUpdateMatInst && materialInstance != null){
           rcm.setMaterialInstanceAt(entity,0,materialInstance!!)
        }
    }

    private fun updateMesh(context: SceneContext, mesh: Mesh) {
        val rcm = context.engine.renderableManager
        val entity = rcm.getInstance(renderable)

        buffers?.destroy(context)
        buffers = createBuffersForMesh(mesh!!,context)
        buffers!!.vertexBuffer.setBufferAt(context.engine,0,mesh.getVertexDataBuffer())

        buffers!!.indexBuffer.setBuffer(context.engine,mesh.getIndexDataBuffer())
        rcm.setGeometryAt(entity,0,primitiveType,buffers!!.vertexBuffer,buffers!!.indexBuffer)
    }

    private fun createBuffersForMesh(mesh: Mesh, context: SceneContext): MeshBuffers {
        val vertexBufferBuilder = VertexBuffer.Builder()
            .bufferCount(mesh.bufferCount)
            .vertexCount(mesh.vertexCount)
            .normalized(VertexBuffer.VertexAttribute.COLOR)
        mesh.attributes.forEach{
            vertexBufferBuilder.attribute(it.attribute,it.bufferIndex,it.type,it.offset,it.stride)
        }
        val vertexBuffer = vertexBufferBuilder.build(context.engine)

        val indexBuffer = IndexBuffer.Builder()
            .indexCount(mesh.indicesCount)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(context.engine)
        return MeshBuffers(vertexBuffer,indexBuffer)
    }

    fun setMesh(newMesh:Mesh){
        mesh = newMesh
        shouldUpdateMesh = true
    }
    fun setMaterialInstance(newMaterialInstance: MaterialInstance){
        materialInstance = newMaterialInstance
        shouldUpdateMaterialInstance = true
    }
}