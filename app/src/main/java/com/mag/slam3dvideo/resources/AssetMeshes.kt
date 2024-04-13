package com.mag.slam3dvideo.resources

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.mag.slam3dvideo.render.mesh.Mesh
import com.mag.slam3dvideo.render.mesh.MeshOf
import com.mokiat.data.front.parser.IMTLParser
import com.mokiat.data.front.parser.IOBJParser
import com.mokiat.data.front.parser.MTLColor
import com.mokiat.data.front.parser.MTLLibrary
import com.mokiat.data.front.parser.MTLMaterial
import com.mokiat.data.front.parser.MTLParser
import com.mokiat.data.front.parser.OBJModel
import com.mokiat.data.front.parser.OBJParser
import de.javagl.obj.Obj
import de.javagl.obj.ObjReader
import java.io.InputStream
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
        val name = "Models/ChevroletCamaro.obj"
        if (assetMeshes.containsKey(name))
            return assetMeshes[name]!!
        val input = context.assets.open(name)
        val parser: IOBJParser = OBJParser()
        val model: OBJModel = parser.parse(input)
        val materialMap: HashMap<String, MTLMaterial> = getMaterials(model) {
            try {
                return@getMaterials context.assets.open("Models/$it")
            } catch (ex: java.lang.Exception) {
                return@getMaterials null
            }
        }
        val vertices = ArrayList<StaticMeshes.MeshVertex>(model.vertices.size)
        val indices = ArrayList<Short>(2048)

        model.vertices.forEach {
            vertices.add(StaticMeshes.MeshVertex(it.x, it.y, it.z, Color.GRAY))
        }
        val rnd = Random()
        for (obj in model.objects) {
            for (mesh in obj.meshes) {

//                val meshMaterialName =mesh.materialName
//                var currentMaterial :MTLMaterial? = null
//                if(meshMaterialName != null){
//                    if(materialMap.containsKey(meshMaterialName)){
//                        currentMaterial = materialMap[meshMaterialName]
//                    }
//                }
//                val mtlClr = currentMaterial?.ambientColor ?: MTLColor(0.5f,0.5f,0.5f)
//                val intClr = Color.rgb(mtlClr.r,mtlClr.g,mtlClr.b)
                val intClr = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))


                for (face in mesh.faces) {
                    val numVerticies = face.references.size
                    face.references.forEach {
                        val v = vertices.get(it.vertexIndex)
                        v.c = intClr
                    }
                    /**
                     * https://cs418.cs.illinois.edu/website/text/obj.html
                     * If there are more than 3, the triangles are the first and each pair of adjacent others
                     * f 2 3 5 7 11 defines 3 triangles: f 2 3 5, f 2 5 7, and f 2 7 11
                     */
                    var offset = 1
                    while ((offset + 1) < numVerticies) {
                        indices.add(face.references.get(0).vertexIndex.toShort())
                        indices.add(face.references.get(offset).vertexIndex.toShort())
                        indices.add(face.references.get(offset + 1).vertexIndex.toShort())
                        offset++
                    }
                }
            }
        }

        val cameraMesh = MeshOf(
            1,
            StaticMeshes.meshVertexSize,
            StaticMeshes.meshAttributes,
            vertices.toTypedArray(),
            indices.toTypedArray(),
            StaticMeshes::put
        )
        assetMeshes[name] = cameraMesh
        return cameraMesh
    }

    private fun getMaterials(
        model: OBJModel,
        modelStreamProvider: (str: String) -> InputStream?
    ): HashMap<String, MTLMaterial> {
        val res = HashMap<String, MTLMaterial>();
        if (model.materialLibraries.isNotEmpty()) {
            model.materialLibraries.forEach {
                try {
                    val matInput = modelStreamProvider(it) ?: return res
                    
                    val matParser: IMTLParser = MTLParser()
                    val library: MTLLibrary = matParser.parse(matInput)
                    for (material in library.materials) {
                        res[material.name] = material
                    }
                    matInput?.close()
                } catch (ex: Exception) {

                }
            }
        }
        return res
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