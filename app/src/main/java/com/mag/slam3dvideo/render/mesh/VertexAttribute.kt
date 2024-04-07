package com.mag.slam3dvideo.render.mesh

import com.google.android.filament.VertexBuffer

data class VertexAttribute(val attribute: VertexBuffer.VertexAttribute, val type: VertexBuffer.AttributeType, val bufferIndex:Int, val offset:Int, val stride:Int)