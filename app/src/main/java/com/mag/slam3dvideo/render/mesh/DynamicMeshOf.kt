package com.mag.slam3dvideo.render.mesh

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DynamicMeshOf<T>(bufferCount: Int,
                       vertexSizeInBytes: Int,
                       attributes: List<VertexAttribute>,
                       var verticies:MutableList<T>,
                       var indicies:MutableList<Short>,
                       val onByteBufferPut:(buffer: ByteBuffer, vertex:T)->Unit)
    : DynamicMesh(bufferCount, vertexSizeInBytes,closestPowerOf2(verticies.size), closestPowerOf2(indicies.size), attributes)
{
        init {
            currentVertexSize = verticies.size
            currentIndicesSize = indicies.size
        }
        companion object
        {
            fun grow(oldCapacity:Int,newCapacity: Int): Int {
                if(newCapacity<oldCapacity)
                    return oldCapacity
                return closestPowerOf2(newCapacity)
            }
            fun closestPowerOf2(value:Int):Int{
                var x = value - 1
                x = x.or(x.shr(1))
                x = x.or(x.shr(2))
                x = x.or(x.shr(4))
                x = x.or(x.shr(8))
                return x + 1;
            }
        }
    private var state = DynamicMeshState.Changed
    override fun resetState() {
        state = DynamicMeshState.Unchanged
    }
    override fun checkState(): DynamicMeshState {
        return state
    }

    override fun getVisibleIndices(): VisibleIndices {
        return VisibleIndices(0,currentIndicesSize)
    }

    fun updateMesh(newVertices:List<T>, newIndices:List<Short>){
        val newVertexSize = newVertices.size
        val newIndexSize = newIndices.size
        val shouldResizeVertices = newVertexSize > vertexCount
        val shouldResizeIndices = newIndexSize > indicesCount
        if(shouldResizeVertices || shouldResizeIndices)
            state = DynamicMeshState.CapacityChanged

        if(newVertexSize > vertexCount ){
            vertexCount = grow(vertexCount, newVertexSize)
        }
        verticies = newVertices.toMutableList()
        currentVertexSize = newVertices.size
        if(newIndexSize > indicesCount){
            indicesCount = grow(indicesCount,newIndexSize)
        }
        currentIndicesSize = newIndices.size
        indicies = newIndices.toMutableList()
        if(!shouldResizeIndices)
            state = DynamicMeshState.Changed
    }

    fun addVertices(newVertices:List<T>, newIndices:List<Short>){
        val newVertexSize = newVertices.size + currentVertexSize
        val newIndexSize = newIndices.size + currentIndicesSize

        val shouldResizeVertices = newVertexSize > vertexCount
        val shouldResizeIndices = newIndexSize > indicesCount
        if(shouldResizeVertices || shouldResizeIndices)
            state = DynamicMeshState.CapacityChanged

        if(newVertexSize > vertexCount ){
            vertexCount = grow(vertexCount, newVertexSize)
        }
        verticies.addAll(newVertices)
        currentVertexSize = verticies.size

        if(newIndexSize > indicesCount){
            indicesCount = grow(indicesCount,newIndexSize)
        }
        indicies.addAll(newIndices)
        currentIndicesSize = indicies.size

        if(!shouldResizeIndices)
            state = DynamicMeshState.Changed
    }
    override fun getVertexDataBuffer(): Buffer {
        val vertexData = ByteBuffer.allocate(currentVertexSize * vertexSizeInBytes)
            .order(ByteOrder.nativeOrder())
            .also {
                verticies.toMutableList().forEach { v -> onByteBufferPut(it,v) }
            }
            .flip()
        return vertexData
    }
    override fun getIndexDataBuffer(): Buffer {
        val indexData = ByteBuffer.allocate(currentIndicesSize * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .also {
                indicies.toMutableList().forEach { i -> it.putShort(i) }
            }
            .flip()
        return indexData
    }

}