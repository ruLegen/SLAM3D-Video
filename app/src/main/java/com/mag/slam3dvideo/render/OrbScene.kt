package com.mag.slam3dvideo.render

import com.google.android.filament.Engine
import com.google.android.filament.View

interface OrbScene {
    fun init(engine: Engine)
    fun activate(view:View)
    fun onResize(width: Int, height: Int)
    fun destroy(engine: Engine)
}