package com.mag.slam3dvideo.resources

import com.google.android.filament.VertexBuffer
import com.mag.slam3dvideo.render.mesh.DynamicMesh
import com.mag.slam3dvideo.render.mesh.DynamicMeshOf
import com.mag.slam3dvideo.render.mesh.Mesh
import com.mag.slam3dvideo.render.mesh.MeshOf
import com.mag.slam3dvideo.render.mesh.VertexAttribute
import java.nio.ByteBuffer

object StaticMeshes {
    data class MeshVertex(
        val x: Float,
        val y: Float,
        val z: Float,
        var c: Int,
    )
    val meshVertexSize = 3 * Float.SIZE_BYTES + Int.SIZE_BYTES
    val meshAttributes = arrayListOf(
        VertexAttribute(
            VertexBuffer.VertexAttribute.POSITION,
            VertexBuffer.AttributeType.FLOAT3,
            0,
            0,
            meshVertexSize
        ),
        VertexAttribute(
            VertexBuffer.VertexAttribute.COLOR,
            VertexBuffer.AttributeType.UBYTE4,
            0,
            3 * Float.SIZE_BYTES,
            meshVertexSize
        ),
    )
    fun put(buffer: ByteBuffer, v: MeshVertex): Unit {
        buffer.apply {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            putInt(v.c)
        }
    }

    fun getCubeMesh(size:Float = 0.05f,xOffset:Float = 0f, yOffset:Float = 0f, zOffset:Float=0f): Mesh {
        val l: Float = -size     // length
        val h: Float = size      // heigh
        //AABBGGRR
        val red = 0xff0000ff.toInt()
        val green = 0xff00ff00.toInt()
        val blue = 0xffff0000.toInt()

        val verticies = arrayOf(
            MeshVertex(l+xOffset, l+yOffset, h+zOffset, red),
            MeshVertex(h+xOffset, l+yOffset, h+zOffset, red),
            MeshVertex(l+xOffset, h+yOffset, h+zOffset, red),
            MeshVertex(h+xOffset, h+yOffset, h+zOffset, red),  // FRONT

            MeshVertex(l+xOffset, l+yOffset, l+zOffset, red),
            MeshVertex(l+xOffset, h+yOffset, l+zOffset, red),
            MeshVertex(h+xOffset, l+yOffset, l+zOffset, red),
            MeshVertex(h+xOffset, h+yOffset, l+zOffset, red),  // BACK

            MeshVertex(l+xOffset, l+yOffset, h+zOffset, green),
            MeshVertex(l+xOffset, h+yOffset, h+zOffset, green),
            MeshVertex(l+xOffset, l+yOffset, l+zOffset, green),
            MeshVertex(l+xOffset, h+yOffset, l+zOffset, green), // LEFT

            MeshVertex(h+xOffset, l+yOffset, l+zOffset, blue),
            MeshVertex(h+xOffset, h+yOffset, l+zOffset, blue),
            MeshVertex(h+xOffset, l+yOffset, h+zOffset, blue),
            MeshVertex(h+xOffset, h+yOffset, h+zOffset, blue), // RIGHT

            MeshVertex(l+xOffset, h+yOffset, h+zOffset, green),
            MeshVertex(h+xOffset, h+yOffset, h+zOffset, green),
            MeshVertex(l+xOffset, h+yOffset, l+zOffset, green),
            MeshVertex(h+xOffset, h+yOffset, l+zOffset, green), // TOP

            MeshVertex(l+xOffset, l+yOffset, h+zOffset, blue),
            MeshVertex(l+xOffset, l+yOffset, l+zOffset, blue),
            MeshVertex(h+xOffset, l+yOffset, h+zOffset, blue),
            MeshVertex(h+xOffset, l+yOffset, l+zOffset, blue),  // BOTTOM
        )

        val indeces = arrayOf<Short>(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
        )


        return MeshOf(1, meshVertexSize, meshAttributes, verticies, indeces, ::put)
    }

    fun <T> MeshOf<T>.asDynamic():DynamicMesh{
        return DynamicMeshOf(this.bufferCount,
            this.vertexSizeInBytes,
            this.attributes,
            this.verticies.toMutableList(),
            this.indicies.toMutableList(),
            this.onByteBufferPut)

    }

    fun getDynamicMesh(capacity:Int): DynamicMesh {
        val vertices = ArrayList<MeshVertex>(capacity).apply { add(MeshVertex(0f,0f,0f,0)) }
        val indices = ArrayList<Short>(capacity).apply { add(0) }
        return DynamicMeshOf(1,
            meshVertexSize,
            meshAttributes,
            vertices,
            indices,
            ::put)
    }

    fun getGizmo(size: Float): Mesh {
        val l: Float = size             // length
        //AABBGGRR
        val red = 0xff0000ff.toInt()
        val green = 0xff00ff00.toInt()
        val yellow = 0xff00ffff.toInt()
        val blue = 0xffff0000.toInt()

        val verticies = arrayOf(
            MeshVertex(0f, 0f, 0f, red),
            MeshVertex(l, 0f, 0f, red),

            MeshVertex(0f, 0f, 0f, green),
            MeshVertex(0f, l, 0f, green),


            MeshVertex(0f, 0f, 0f, blue),
            MeshVertex(0f, 0f, l, blue),

        )

        val indeces = arrayOf<Short>(
            0, 1, 2, 3, 4,5,
        )
        return MeshOf(1, meshVertexSize, meshAttributes, verticies, indeces, ::put)
    }
}