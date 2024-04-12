package com.mag.slam3dvideo.render.components

import com.google.android.filament.EntityManager
import com.mag.slam3dvideo.render.SceneContext

class TransformComponent : ObjectComponent() {
    val isInitialized: Boolean
        get() = tcmEntity>0

    var tcmEntity = 0
        private set
    override fun start(context: SceneContext) {
        if (parent == null)
            return
        ensureEntityCreated(context)

        /** make sure that children are already called @see start **/
        val children = parent!!.children
        children.forEach {
            if(!it.transformComponent.isInitialized)
                return@forEach
            updateChildren(context,it.transformComponent)
        }
    }

    private fun ensureEntityCreated(context: SceneContext) {
        if(tcmEntity == 0)
            tcmEntity = context.engine.transformManager.create(parent!!.entity)
    }

    override fun update(context: SceneContext) {
    }
    fun updateChildren(context: SceneContext, childTransformComponent: TransformComponent) {
        val tm = context.engine.transformManager
        tm.setParent(childTransformComponent.tcmEntity,tcmEntity)
    }
    fun setTransform(context:SceneContext,matrix: FloatArray) {
        ensureEntityCreated(context)
        val tcm = context.engine.transformManager
        tcm.setTransform(tcmEntity, matrix)
    }
    fun getTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getTransform(tcmEntity,trasform)
        return trasform
    }
    fun getWorldTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getWorldTransform(tcmEntity,trasform)
        return trasform
    }


}