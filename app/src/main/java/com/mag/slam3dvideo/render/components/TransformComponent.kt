package com.mag.slam3dvideo.render.components

import com.google.android.filament.EntityManager
import com.mag.slam3dvideo.render.SceneContext
/**
 * The TransformComponent class contains logic for managing transformations (location and orientation) of a scene object.
 */
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

    /**
     * Do nothing
     */
    override fun update(context: SceneContext) {
    }
    /**
     * Updates the children of this transform component with the given scene context and child transform component.
     * @param context The scene context.
     * @param childTransformComponent The child transform component to update.
     */
    fun updateChildren(context: SceneContext, childTransformComponent: TransformComponent) {
        val tm = context.engine.transformManager
        tm.setParent(childTransformComponent.tcmEntity,tcmEntity)
    }

    /**
     * Set transform
     *
     * @param context
     * @param matrix OpenGL 4x4 (16x1, column major) transform matrix
     */
    fun setTransform(context:SceneContext,matrix: FloatArray) {
        ensureEntityCreated(context)
        val tcm = context.engine.transformManager
        tcm.setTransform(tcmEntity, matrix)
    }
    /**
     * Gets the transform relative parent object. If parent null, returns World position.
     * @param context The scene context.
     * @return OpenGL 4x4 (16x1, column major) transform matrix
     */
    fun getTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getTransform(tcmEntity,trasform)
        return trasform
    }
    /**
     * Gets the world transform
     * @param context The scene context.
     * @return OpenGL 4x4 (16x1, column major) transform matrix
     */
    fun getWorldTransform(context:SceneContext):FloatArray{
        val trasform = FloatArray(16)
        val tcm = context.engine.transformManager
        tcm.getWorldTransform(tcmEntity,trasform)
        return trasform
    }


}