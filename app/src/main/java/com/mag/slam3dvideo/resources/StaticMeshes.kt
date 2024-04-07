package com.mag.slam3dvideo.resources

import com.google.android.filament.VertexBuffer
import com.mag.slam3dvideo.render.Mesh
import com.mag.slam3dvideo.render.MeshOf
import com.mag.slam3dvideo.render.VertexAttribute
import java.nio.ByteBuffer

object StaticMeshes {
    data class MeshVertex(
        val x: Float,
        val y: Float,
        val z: Float,
        val c: Int,
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
            putFloat(v.z)
            putFloat(v.y)
            putInt(v.c)
        }
    }

    fun getCubeMesh(): Mesh {

        val size = 0.05f;

        val l: Float = -size     // length
        val h: Float = size      // heigh
        val red = 0xffff0000.toInt()
        val green = 0xff00ff00.toInt()
        val blue = 0xff0000ff.toInt()

        val verticies = arrayOf(
            MeshVertex(l, l, h, red),
            MeshVertex(h, l, h, red),
            MeshVertex(l, h, h, red),
            MeshVertex(h, h, h, red),  // FRONT

            MeshVertex(l, l, l, red),
            MeshVertex(l, h, l, red),
            MeshVertex(h, l, l, red),
            MeshVertex(h, h, l, red),  // BACK

            MeshVertex(l, l, h, green),
            MeshVertex(l, h, h, green),
            MeshVertex(l, l, l, green),
            MeshVertex(l, h, l, green), // LEFT

            MeshVertex(h, l, l, blue),
            MeshVertex(h, h, l, blue),
            MeshVertex(h, l, h, blue),
            MeshVertex(h, h, h, blue), // RIGHT

            MeshVertex(l, h, h, green),
            MeshVertex(h, h, h, green),
            MeshVertex(l, h, l, green),
            MeshVertex(h, h, l, green), // TOP

            MeshVertex(l, l, h, blue),
            MeshVertex(l, l, l, blue),
            MeshVertex(h, l, h, blue),
            MeshVertex(h, l, l, blue),  // BOTTOM
        )

        val indeces = arrayOf<Short>(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
        )


        return MeshOf(1, meshVertexSize, meshAttributes, verticies, indeces, ::put)
    }

    fun getDynamicMesh(): Mesh {
        return MeshOf(1, meshVertexSize, meshAttributes, arrayOf(MeshVertex(0f,0f,0f,0)), arrayOf(0), ::put)
    }
}