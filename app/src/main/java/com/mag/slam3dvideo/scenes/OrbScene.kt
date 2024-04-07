package com.mag.slam3dvideo.scenes

import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.View

interface OrbScene {
    fun init(engine: Engine)
    fun activate()
    fun beforeRender(renderer: Renderer)
    fun render(renderer: Renderer)
    fun onResize(width: Int, height: Int)
    fun destroy(engine: Engine)
    fun update(){}
}