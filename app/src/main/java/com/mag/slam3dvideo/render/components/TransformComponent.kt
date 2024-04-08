package com.mag.slam3dvideo.render.components

import com.google.android.filament.EntityManager
import com.mag.slam3dvideo.render.SceneContext

class TransformComponent : ObjectComponent() {
    var entity: Int = 0
        private set

    override fun start(context: SceneContext) {
        entity = EntityManager.get().create()
    }

    override fun update(context: SceneContext) {
    }

    fun setTransform(context:SceneContext,matrix: FloatArray) {
        val tcm = context.engine.transformManager
        tcm.setTransform(tcm.getInstance(entity), matrix)
    }
}