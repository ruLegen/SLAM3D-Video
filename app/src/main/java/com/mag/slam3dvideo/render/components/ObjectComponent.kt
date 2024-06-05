package com.mag.slam3dvideo.render.components

import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import java.lang.RuntimeException
import java.util.UUID
/**
 * The ObjectComponent class represents a logic that attached to a scene object.
 */
abstract class ObjectComponent() {
    var isEnabled: Boolean = true
        private set
    var guid : UUID = UUID.randomUUID()
    var parent: SceneObject?=null
        private set

    /**
     * Start called once at initialization time
     *
     * @param context
     */
    abstract fun start(context: SceneContext)

    /**
     * Update called each tick
     *
     * @param context
     */
    abstract fun update(context: SceneContext)

    fun setParent(newParent:SceneObject){
        setParent(newParent,false)
    }

    /**
     * Sets the parent object that it's bound to, optionally forcing the change.
     * @param newParent The new parent scene object.
     * @param force True to force the change even if the component already has a parent, false otherwise.
     * @throws RuntimeException If force is false and the component already has a parent.
     */
    fun setParent(newParent:SceneObject, force: Boolean){
        if(parent == null){
            parent = newParent
            return;
        }
        if(!force)
            throw RuntimeException("cannot set parent, cause this component already have one")
        newParent.removeComponent(this)
        parent = newParent
    }
    fun setEnabled(enabled: Boolean){
        isEnabled = enabled;
    }
}