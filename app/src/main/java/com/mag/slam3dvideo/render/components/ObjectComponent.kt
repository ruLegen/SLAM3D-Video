package com.mag.slam3dvideo.render.components

import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import java.lang.RuntimeException
import java.util.UUID

abstract class ObjectComponent() {
    var isEnabled: Boolean = true
        private set
    var guid : UUID = UUID.randomUUID()
    var parent: SceneObject?=null
        private set
    abstract fun start(context: SceneContext)
    abstract fun update(context: SceneContext)

    fun setParent(newParent:SceneObject){
        setParent(newParent,false)
    }
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