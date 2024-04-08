package com.mag.slam3dvideo.render.components

import com.google.android.filament.EntityManager
import com.google.android.filament.TransformManager
import com.mag.slam3dvideo.render.SceneContext
import java.lang.ref.WeakReference

class TransformComponent : ObjectComponent() {
    var entity: Int = 0
        private set
    init {
        entity = EntityManager.get().create()
    }
    override fun start(context: SceneContext) {
        context.engine.transformManager.create(entity)
    }

    override fun update(context: SceneContext) {
    }

    fun setTransform(context:SceneContext,matrix: FloatArray) {
        val tcm = context.engine.transformManager
        tcm.setTransform(tcm.getInstance(entity), matrix)
    }
    fun getTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getTransform(tcm.getInstance(entity),trasform)
        return trasform
    }
    fun getWorldTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getWorldTransform(tcm.getInstance(entity),trasform)
        return trasform
    }}