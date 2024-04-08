package com.mag.slam3dvideo.render.mesh

data class VisibleIndices(val offsetInBytes:Int,val count:Int)
abstract class DynamicMesh (bufferCount:Int,
                            vertexSizeInBytes:Int,
                            initialVertexCapacity:Int,
                            initialIndicesCount:Int,
                            attributes:List<VertexAttribute>)
    : Mesh(bufferCount,vertexSizeInBytes,initialVertexCapacity,initialIndicesCount,attributes){

    var currentVertexSize:Int = initialVertexCapacity
    var currentIndicesSize:Int = initialIndicesCount
    abstract fun resetState()
    abstract fun checkState(): DynamicMeshState
    abstract fun getVisibleIndices(): VisibleIndices
}