package com.mag.slam3dvideo.render

import com.google.android.filament.VertexBuffer
import java.lang.RuntimeException
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class VertexAttribute(val attribute: VertexBuffer.VertexAttribute,val type: VertexBuffer.AttributeType,val bufferIndex:Int,val offset:Int, val stride:Int)
abstract class Mesh (val bufferCount:Int,
                     val vertexSizeInBytes:Int,
                     val vertexCount:Int,
                     val indicesCount:Int,
                     val attributes:List<VertexAttribute>){
    init {
        if(bufferCount != 1)
            throw RuntimeException("Current more than 1 buffer is not supported")
    }

    abstract fun getVertexDataBuffer(): Buffer;
    abstract fun getIndexDataBuffer():Buffer;
}
class MeshOf<T>(bufferCount: Int,
                vertexSize: Int,
                attributes: List<VertexAttribute>,
                val verticies:Array<T>,
                val indicies:Array<Short>,
                val onByteBufferPut:(buffer:ByteBuffer,vertex:T)->Unit) : Mesh(bufferCount, vertexSize,verticies.size, indicies.size, attributes)
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
    override fun getIndexDataBuffer():Buffer {
        val indexData = ByteBuffer.allocate(indicesCount * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .also {
                indicies.forEach { i -> it.putShort(i) }
            }
            .flip()
        return indexData
    }

}