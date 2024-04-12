package com.mag.slam3dvideo.resources

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.mag.slam3dvideo.render.mesh.Mesh
import com.mag.slam3dvideo.render.mesh.MeshOf
import de.javagl.obj.Obj
import de.javagl.obj.ObjReader
import java.util.Random

@SuppressLint("StaticFieldLeak")
object AssetMeshes {
    private lateinit var context: Context
    private val assetMeshes = HashMap<String, Mesh>()
    fun init(newContext: Context) {
        context = newContext
    }

    fun getCamera(): Mesh {
        val name = "Models/camera2.obj"
        if (assetMeshes.containsKey(name))
            return assetMeshes[name]!!
        val cameraMesh = readAsset(name)
        assetMeshes[name] = cameraMesh
        return cameraMesh
    }

    fun getCamaro(): Mesh {
        val name = "Models/Chevrolet Camaro.obj"
        if (assetMeshes.containsKey(name))
            return assetMeshes[name]!!
        val cameraMesh = readAsset(name)
        assetMeshes[name] = cameraMesh
        return cameraMesh
    }

    private fun readAsset(name: String): Mesh {
        val input = context.assets.open(name)
        val obj: Obj = ObjReader.read(input)

        val verticies = getVerices(obj)
        val indices = getIndices(obj)
        return MeshOf(
            1,
            StaticMeshes.meshVertexSize,
            StaticMeshes.meshAttributes,
            verticies.toTypedArray(),
            indices.toTypedArray(),
            StaticMeshes::put
        )
    }

    private fun getVerices(obj: Obj): ArrayList<StaticMeshes.MeshVertex> {
        val vertices = ArrayList<StaticMeshes.MeshVertex>(obj.numVertices)
        val rnd = Random()
        for (i in 0 until obj.numVertices) {
            val tuple = obj.getVertex(i)
            val clr = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            vertices.add(StaticMeshes.MeshVertex(tuple.x, tuple.y, tuple.z, clr))
        }
        return vertices
    }

    private fun getIndices(obj: Obj): ArrayList<Short> {
        val indices = ArrayList<Short>()
        for (i in 0 until obj.numFaces) {
            val face = obj.getFace(i)
            val numVerticies = face.numVertices

            /**
             * https://cs418.cs.illinois.edu/website/text/obj.html
             * If there are more than 3, the triangles are the first and each pair of adjacent others
             * f 2 3 5 7 11 defines 3 triangles: f 2 3 5, f 2 5 7, and f 2 7 11
             */

            var offset = 1
            while ((offset + 1) < numVerticies) {
                indices.add(face.getVertexIndex(0).toShort())
                indices.add(face.getVertexIndex(offset).toShort())
                indices.add(face.getVertexIndex(offset + 1).toShort())
                offset++
            }
        }
        return indices
    }
}