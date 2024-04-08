package com.mag.slam3dvideo.render

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Scene
import com.google.android.filament.View

open class SceneContext(val engine: Engine) {
    var scene: Scene
        protected set

    val sceneObjectContainer = SceneContainer()

    init {
        scene = engine.createScene()
    }
    fun update() {
        sceneObjectContainer.update(this)
    }
}