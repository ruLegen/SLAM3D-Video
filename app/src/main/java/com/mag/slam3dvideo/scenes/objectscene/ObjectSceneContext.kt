package com.mag.slam3dvideo.scenes.objectscene

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import com.google.android.filament.View
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
import com.mag.slam3dvideo.resources.StaticMaterials
import com.mag.slam3dvideo.resources.StaticMeshes

class ObjectSceneContext(engine: Engine) : SceneContext(engine) {
    var camera: Camera
        private set
    var view: View
        private set
    val boxEntity: Int
        get() {
            return box.entity
        }
    private lateinit var box: SceneObject
    private lateinit var pointCloud: SceneObject
    init {
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }
    fun initScene() {
        scene.skybox = null
        box = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = StaticMeshes.getCubeMesh()
                setMesh(mesh)
                val material = StaticMaterials.getMeshMetal(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLE_STRIP
            }
            addComponent(renderComponent)
        }
        pointCloud = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = StaticMeshes.getDynamicMesh()
                setMesh(mesh)
                val material = StaticMaterials.getMeshMetal(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.POINTS
            }
            addComponent(renderComponent)
        }
        sceneObjectContainer.addObject(box)
        sceneObjectContainer.addObject(pointCloud)
    }
    fun setBoxTransform(matrix:FloatArray){
        box.transformComponent.setTransform(this,matrix)
    }
}