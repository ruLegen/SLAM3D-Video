package com.mag.slam3dvideo.render.mesh

enum class DynamicMeshState{
    Unchanged,
    CapacityChanged,
    Changed,
    Advanced,       // added or removed sequentially some vertices
}