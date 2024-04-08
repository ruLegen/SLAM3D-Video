package com.mag.slam3dvideo.render.mesh

import java.lang.RuntimeException
import java.nio.Buffer

abstract class Mesh (val bufferCount:Int,
                     val vertexSizeInBytes:Int,
                     var vertexCount:Int,
                     var indicesCount:Int,
                     val attributes:List<VertexAttribute>){
    init {
        if(bufferCount != 1)
            throw RuntimeException("Current more than 1 buffer is not supported")
    }

    abstract fun getVertexDataBuffer(): Buffer;
    abstract fun getIndexDataBuffer():Buffer;
}

