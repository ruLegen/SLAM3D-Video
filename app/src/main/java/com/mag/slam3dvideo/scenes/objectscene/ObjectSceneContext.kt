package com.mag.slam3dvideo.scenes.objectscene

import android.graphics.Color
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import com.google.android.filament.View
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
import com.mag.slam3dvideo.render.mesh.DynamicMesh
import com.mag.slam3dvideo.render.mesh.DynamicMeshOf
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
    private lateinit var cameraObj: SceneObject
    private lateinit var pointCloud: SceneObject
    private lateinit var dynamicCube: DynamicMeshOf<StaticMeshes.MeshVertex>
    init {
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }
    fun addVertex(){
        fun randomPointInCircle(radius:Float):StaticMeshes.MeshVertex{
                val u = Math.random();
                val v = Math.random();
                val theta = 2 * Math.PI * u;
                val phi = Math.acos(2 * v - 1);
                val x = 0 + (radius * Math.sin(phi) * Math.cos(theta));
                val y = 0 + (radius * Math.sin(phi) * Math.sin(theta));
                val z = 0 + (radius * Math.cos(phi));
                return StaticMeshes.MeshVertex(x.toFloat(),y.toFloat(),z.toFloat(), Color.MAGENTA.toInt());
        }
        val vertIndex = dynamicCube.indicies.last()
        val newIndicies = (1..3).map { (vertIndex+it).toShort() }
        val newVertecies= (1..3).map { randomPointInCircle(0.07f) }
        dynamicCube.addVertices(newVertecies,newIndicies)

    }
    fun initScene() {
        scene.skybox = null
        box = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = StaticMeshes.getCubeDynamicMesh()
                setMesh(mesh)
                dynamicCube = mesh as DynamicMeshOf<StaticMeshes.MeshVertex>

                val material = StaticMaterials.getMeshMetal(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLE_STRIP
            }
            addComponent(renderComponent)
        }
        cameraObj = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = StaticMeshes.getCubeMesh(0.01f)
                setMesh(mesh)
                val material = StaticMaterials.getMeshMetal(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLE_STRIP
            }
            addComponent(renderComponent)
        }
//        pointCloud = SceneObject().apply {
//            val renderComponent = MeshRendererComponent().apply {
//                val mesh = StaticMeshes.getDynamicMesh()
//                setMesh(mesh)
//                val material = StaticMaterials.getMeshMetal(this@ObjectSceneContext)
//                setMaterialInstance(material.createInstance())
//                primitiveType = RenderableManager.PrimitiveType.POINTS
//            }
//            addComponent(renderComponent)
//        }
        sceneObjectContainer.addObject(box)
        sceneObjectContainer.addObject(cameraObj)
//        sceneObjectContainer.addObject(pointCloud)
    }
    fun setBoxTransform(matrix:FloatArray){
        box.transformComponent.setTransform(this,matrix)
    }
    fun setCameraTransform(matrix:FloatArray){
        cameraObj.transformComponent.setTransform(this,matrix)
    }
}