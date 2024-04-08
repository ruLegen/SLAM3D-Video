package com.mag.slam3dvideo.utils

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Vector


class ObjLoader(context: Context, file: String?) {
    val numFaces: Int
    val normals: FloatArray
    val textureCoordinates: FloatArray
    val positions: FloatArray

    init {
        val vertices = Vector<Float>()
        val normals = Vector<Float>()
        val textures = Vector<Float>()
        val faces = Vector<String>()
        val indices= Vector<Short>()
        var reader: BufferedReader? = null
        try {
            val input = InputStreamReader(context.assets.open(file!!))
            reader = BufferedReader(input)
            var line: String
            while (reader.readLine().also { line = it } != null) {
                val parts = line.split(" ")
                when (parts[0]) {
                    "v" -> {
                        // vertices
                        vertices.add(java.lang.Float.valueOf(parts[1]))
                        vertices.add(java.lang.Float.valueOf(parts[2]))
                        vertices.add(java.lang.Float.valueOf(parts[3]))
                    }
//
//                    "vt" -> {
//                        // textures
//                        textures.add(java.lang.Float.valueOf(parts[1]))
//                        textures.add(java.lang.Float.valueOf(parts[2]))
//                    }
//
//                    "vn" -> {
//                        // normals
//                        normals.add(java.lang.Float.valueOf(parts[1]))
//                        normals.add(java.lang.Float.valueOf(parts[2]))
//                        normals.add(java.lang.Float.valueOf(parts[3]))
//                    }

                    "f" -> {
                        // faces: vertex/texture/normal
                        faces.add(parts[1])
                        faces.add(parts[2])
                        faces.add(parts[3])
                    }
                }
            }
        } catch (e: IOException) {
            val l = e
            // cannot load or read file
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    //log the exception
                }
            }
        }
        numFaces = faces.size
        this.normals = FloatArray(numFaces * 3)
        textureCoordinates = FloatArray(numFaces * 2)
        positions = FloatArray(numFaces * 3)
        var positionIndex = 0
        var normalIndex = 0
        var textureIndex = 0
        for (face in faces) {
            val parts = face.split("/".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
            var index = 3 * (parts[0].toShort() - 1)
            indices.add(index.toShort())
            positions[positionIndex++] = vertices[index++]
            positions[positionIndex++] = vertices[index++]
            positions[positionIndex++] = vertices[index]
            index = 2 * (parts[1].toShort() - 1)
            textureCoordinates[normalIndex++] = textures[index++]
            // NOTE: Bitmap gets y-inverted
            textureCoordinates[normalIndex++] = 1 - textures[index]
            index = 3 * (parts[2].toShort() - 1)
            this.normals[textureIndex++] = normals[index++]
            this.normals[textureIndex++] = normals[index++]
            this.normals[textureIndex++] = normals[index]
        }
    }
}