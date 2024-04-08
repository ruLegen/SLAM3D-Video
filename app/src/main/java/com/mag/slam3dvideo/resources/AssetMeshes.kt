package com.mag.slam3dvideo.resources

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.google.android.filament.Colors
import com.google.android.filament.Material
import com.mag.slam3dvideo.render.mesh.Mesh
import com.mag.slam3dvideo.render.mesh.MeshOf
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader

@SuppressLint("StaticFieldLeak")
object AssetMeshes {
    private lateinit var context:Context
    private val assetMeshes  = HashMap<String, Mesh>()
    fun init(newContext:Context){
        context = newContext
    }

    fun getCamera(): Mesh {
        val name="Models/camera.obj"
        if(assetMeshes.containsKey(name))
            return assetMeshes[name]!!
        val input = context.assets.open(name)
        val obj: Obj = ObjReader.read(input)
        val vert = ObjData.getVertices(obj);
        val faceVertexIndices = ObjData.getFaceVertexIndicesArray(obj)
        val verticies = ArrayList<StaticMeshes.MeshVertex>(vert.capacity()/3)
        for (i in 0 until vert.capacity() step 3){
            verticies.add(StaticMeshes.MeshVertex(vert.get(i),vert.get(i+1),vert.get(i+2),Color.GRAY))
        }
        val indices = faceVertexIndices.map { it.toShort() }
        val cameraMesh = MeshOf(1,StaticMeshes.meshVertexSize,StaticMeshes.meshAttributes,verticies.toTypedArray(),indices.toTypedArray(),StaticMeshes::put)
        assetMeshes[name] = cameraMesh

        return cameraMesh
    }
}