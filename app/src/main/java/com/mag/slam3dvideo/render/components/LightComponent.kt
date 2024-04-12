package com.mag.slam3dvideo.render.components

import com.google.android.filament.Colors
import com.google.android.filament.LightManager
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.components.ObjectComponent

class LightComponent : ObjectComponent(){
    override fun start(context: SceneContext) {
        val (r, g, b) = Colors.cct(5_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            // Intensity of the sun in lux on a clear day
            .intensity(110_000.0f)
            // The direction is normalized on our behalf
            .direction(0.0f, -0.5f, -1.0f)
            .build(context.engine, parent!!.entity)
        context.scene.addEntity(parent!!.entity)
    }

    override fun update(context: SceneContext) {
    }
}