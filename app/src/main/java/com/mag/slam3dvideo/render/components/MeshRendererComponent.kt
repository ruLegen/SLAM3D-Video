package com.mag.slam3dvideo.render.components

import androidx.core.content.contentValuesOf
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import com.google.android.filament.utils.conjugate
import com.mag.slam3dvideo.render.mesh.DynamicMesh
import com.mag.slam3dvideo.render.mesh.Mesh
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.mesh.DynamicMeshOf
import com.mag.slam3dvideo.render.mesh.DynamicMeshState

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
    private var shouldUpdateVisibility:Boolean = false
    private var isDynamic: Boolean = false
    var renderable: Int = -1
        private set
    private var mesh: Mesh? = null
    var materialInstance:MaterialInstance? = null
        private set

    var isVisible:Boolean = true
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
        if(shouldUpdateVisibility){
            shouldUpdateVisibility = false
            if(isVisible)
                context.scene.addEntity(renderable)
            else
                context.scene.removeEntity(renderable)
        }
        var needUpdateMesh = shouldUpdateMesh
        val needUpdateMatInst = shouldUpdateMaterialInstance
        if(isDynamic && mesh != null){
            val meshState = (mesh as DynamicMesh)?.checkState()
            needUpdateMesh = needUpdateMesh.or(meshState != DynamicMeshState.Unchanged)
        }

        if(!needUpdateMesh && !needUpdateMatInst)
            return

        val rcm = context.engine.renderableManager
        val entity = rcm.getInstance(renderable)
        if(entity == 0)
            return

        shouldUpdateMesh =false
        shouldUpdateMaterialInstance = false

        if(needUpdateMesh && mesh != null){
            if(mesh is DynamicMesh)
                updateDynamicMesh(context,mesh as DynamicMesh)
            else
                updateMesh(context,mesh!!)
        }

        if(needUpdateMatInst && materialInstance != null){
           rcm.setMaterialInstanceAt(entity,0,materialInstance!!)
        }
    }

    fun  setVisibility(visible:Boolean){
        isVisible = visible
        shouldUpdateVisibility =true
    }

    private fun updateDynamicMesh(context: SceneContext, dynamicMesh: DynamicMesh) {
        val state = dynamicMesh.checkState()
        if(state == DynamicMeshState.Unchanged)
            return
        dynamicMesh.resetState()
        if(buffers == null){
            updateMesh(context,dynamicMesh)
            return
        }

        when(state){
            DynamicMeshState.CapacityChanged -> {
                updateMesh(context,dynamicMesh)
            }
            DynamicMeshState.Changed -> {
                buffers!!.vertexBuffer.setBufferAt(context.engine,0,dynamicMesh.getVertexDataBuffer())
                buffers!!.indexBuffer.setBuffer(context.engine,dynamicMesh.getIndexDataBuffer())
            }
            DynamicMeshState.Advanced -> {
                val visibleIndices = dynamicMesh.getVisibleIndices()
                buffers!!.indexBuffer.setBuffer(context.engine,dynamicMesh.getIndexDataBuffer(),visibleIndices.offsetInBytes,visibleIndices.count)
            }
            else -> {}
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

    fun setMesh(newMesh: Mesh){
        isDynamic = newMesh is DynamicMesh
        mesh = newMesh
        shouldUpdateMesh = true
    }
    fun setMaterialInstance(newMaterialInstance: MaterialInstance){
        materialInstance = newMaterialInstance
        shouldUpdateMaterialInstance = true
    }
}