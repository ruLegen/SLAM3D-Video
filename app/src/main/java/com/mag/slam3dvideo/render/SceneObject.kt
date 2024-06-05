package com.mag.slam3dvideo.render

import com.google.android.filament.EntityManager
import com.mag.slam3dvideo.render.components.ObjectComponent
import com.mag.slam3dvideo.render.components.TransformComponent
import java.util.UUID
import kotlin.collections.ArrayList
/**
 * The Generic class provides a generic wrapper around a class type.
 * @param <T> The type parameter for the class.
 */
class Generic<T : Any>(val c: Class<T>) {
    companion object {
        inline operator fun <reified T : Any>invoke() = Generic(T::class.java)
    }

    /**
     * Determines whether the provided object is an instance of the type parameter T.
     * @param t The object to check.
     * @return true if the object is an instance of T, false otherwise.
     */
    fun isInstance(t:T?):Boolean{
        return c.isInstance(t)
    }
}
/**
 * The SceneObject class represents an object within a scene.
 * It also contains components
 *
 * Creates a new SceneObject instance with the specified name.
 * Name is referenced when scene is exported to file
 * @param name The name of the scene object.
 */
open class SceneObject(val name:String="object") {
    val guid:UUID = UUID.randomUUID()
    val components:Map<UUID,ObjectComponent>
        get()= componentContainer

    val transformComponent = TransformComponent()

    var entity: Int = 0
        private set
    var parent:SceneObject? = null
        private set

    val children :Collection<SceneObject>
        get() = childerContainer.values

    private val childerContainer = HashMap<UUID,SceneObject>()
    private val componentContainer  = HashMap<UUID,ObjectComponent>()
    private val notInitializedComponents  = HashMap<UUID,ObjectComponent>()
    init {
        entity = EntityManager.get().create()
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
    /**
     * Retrieves a component of the specified type from the scene object.
     * @param <T> The type parameter for the component.
     * @return The component of the specified type, or null if not found.
     */
    inline fun <reified T> getComponent():T? where T : ObjectComponent{
        return components.values.find { it is T } as T?
    }
    /**
     * Retrieves components of the specified type from the scene object.
     * @param <T> The type parameter for the component collection.
     * @return The collection of components of the specified type, or null if not found.
     */
    inline fun <reified T> getComponents():T? where T : Collection<ObjectComponent>{
        return components.values.filter { it is T } as T?
    }
    /**
     * Adds a child scene object to this scene object.
     * @param context The scene context.
     * @param obj The child scene object to add.
     */
    fun addChild(context: SceneContext, obj:SceneObject){
        if(childerContainer.containsKey(obj.guid))
            return
        if(obj.parent != null)
            return
        childerContainer[obj.guid] = obj
        obj.parent = this

        if(transformComponent.isInitialized){
            transformComponent.updateChildren(context,obj.transformComponent)
        }
    }
    /**
     * Updates the scene object and its components.
     * Propagates update call to its children
     * @param context The scene context.
     */
    fun update(context: SceneContext) {
        if(childerContainer.size > 0){
            childerContainer.values.forEach {
                it.update(context)
            }
        }
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