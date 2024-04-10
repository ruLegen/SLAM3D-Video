package com.mag.slam3dvideo.render

import com.mag.slam3dvideo.render.components.ObjectComponent
import com.mag.slam3dvideo.render.components.TransformComponent
import java.util.ArrayList
import java.util.UUID
import kotlin.reflect.typeOf
class Generic<T : Any>(val c: Class<T>) {
    companion object {
        inline operator fun <reified T : Any>invoke() = Generic(T::class.java)
    }
    fun isInstance(t:T?):Boolean{
        return c.isInstance(t)
    }
}
open class SceneObject {
    val guid:UUID = UUID.randomUUID()
    val components:Map<UUID,ObjectComponent>
        get()= componentContainer

    val transformComponent = TransformComponent()
    val entity:Int
        get() = transformComponent.entity

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

    inline fun <reified T> getComponent():T? where T : ObjectComponent{
        return components.values.find { it is T } as T?
    }
    fun update(context: SceneContext) {
        if(notInitializedComponents.size > 0){
            val initialized = ArrayList<UUID>(notInitializedComponents.size)
            if(notInitializedComponents.containsKey(transformComponent.guid)){
                transformComponent.start(context)
                initialized.add(transformComponent.guid)
                notInitializedComponents.remove(transformComponent.guid)
            }
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