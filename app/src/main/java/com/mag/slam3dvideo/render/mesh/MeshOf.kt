package com.mag.slam3dvideo.render.mesh

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MeshOf<T>(bufferCount: Int,
                vertexSize: Int,
                attributes: List<VertexAttribute>,
                val verticies:Array<T>,
                val indicies:Array<Short>,
                val onByteBufferPut:(buffer: ByteBuffer, vertex:T)->Unit) : Mesh(bufferCount, vertexSize,verticies.size, indicies.size, attributes)
{
    override fun getVertexDataBuffer(): Buffer {
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSizeInBytes)
            .order(ByteOrder.nativeOrder())
            .also {
                verticies.forEach { v -> onByteBufferPut(it,v) }
            }
            .flip()
        return vertexData
    }
    override fun getIndexDataBuffer(): Buffer {
        val indexData = ByteBuffer.allocate(indicesCount * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .also {
                indicies.forEach { i -> it.putShort(i) }
            }
            .flip()
        return indexData
    }

}