package com.mag.slam3dvideo.render

class SceneContainer {
    var objects = ArrayList<SceneObject>()
        private set
    fun update(context: SceneContext){
        objects.forEach {
            it.update(context)
        }
    }
    fun addObject(sceneObject: SceneObject){
        objects.add(sceneObject)
    }
    fun destroy(){
        objects.forEach {
            it.destroy()
        }
    }
}