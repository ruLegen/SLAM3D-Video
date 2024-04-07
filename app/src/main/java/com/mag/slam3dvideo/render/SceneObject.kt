package com.mag.slam3dvideo.render

import com.mag.slam3dvideo.render.components.ObjectComponent
import com.mag.slam3dvideo.render.components.TransformComponent
import java.util.ArrayList
import java.util.UUID

open class SceneObject {
    val guid:UUID = UUID.randomUUID()
    val components:Map<UUID,ObjectComponent>
        get()= componentContainer

    val transformComponent = TransformComponent()
    val entity:Int
        get() {return  transformComponent.entity}

    private val componentContainer  = HashMap<UUID,ObjectComponent>()
    private val notInitializedComponents  = HashMap<UUID,ObjectComponent>()
    init {
        addComponent(transformComponent)
    }
    fun addComponent(component:ObjectComponent){
        if(componentContainer.containsKey(component.guid))
            return;
        component.setParent(this)
        notInitializedComponents.put(component.guid,component)
        componentContainer.put(component.guid,component)
    }
    fun removeComponent(component: ObjectComponent) {
        val guid = component.guid
        if(componentContainer.containsKey(guid))
            return;
        notInitializedComponents.remove(component.guid)
        componentContainer.remove(component.guid)
    }

    fun update(context: SceneContext) {
        if(notInitializedComponents.size > 0){
            val initialized = ArrayList<UUID>(notInitializedComponents.size)
            notInitializedComponents.values.forEach {
                if(it.isEnabled){
                    it.start(context)
                    initialized.add(it.guid)
                }
            }
            initialized.forEach { notInitializedComponents.remove(it) }
        }
        componentContainer.values.forEach {
            if(it.isEnabled){
                it.update(context)
            }
        }
    }

    fun destroy(){

    }
}