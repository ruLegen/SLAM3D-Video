package com.mag.slam3dvideo.scenes.objectscene

import android.graphics.Color
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
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
    private lateinit var emptySkyBox: Skybox
    private lateinit var box: SceneObject
    private lateinit var cameraObj: SceneObject
    private lateinit var pointCloud: SceneObject
    private lateinit var dynamicCube: DynamicMeshOf<StaticMeshes.MeshVertex>
    private lateinit var pointCloudMesh: DynamicMeshOf<StaticMeshes.MeshVertex>
    init {
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
        emptySkyBox = Skybox.Builder().color(0f,0f,0f,1f).build(engine)
        scene.skybox = emptySkyBox

        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }
    fun enableSkyBox(enabled:Boolean){
        val newSkyBox = if(enabled) emptySkyBox else null
        scene.skybox = newSkyBox
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
//        dynamicCube.addVertices(newVertecies,newIndicies)

    }
    fun initScene() {
        box = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val meshSize = 0.025f
                val mesh = StaticMeshes.getCubeMesh(meshSize,0f,-meshSize,0f)
                setMesh(mesh)
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLE_STRIP
            }
            addComponent(renderComponent)
        }
        cameraObj = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = StaticMeshes.getCubeMesh(0.01f)
                setMesh(mesh)
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLES
            }
            addComponent(renderComponent)
        }
        pointCloud = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                pointCloudMesh = StaticMeshes.getDynamicMesh(1024) as DynamicMeshOf<StaticMeshes.MeshVertex>
                setMesh(pointCloudMesh)
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                val matInstance = material.createInstance()
                matInstance.setParameter("size",5f)
                setMaterialInstance(matInstance)
                primitiveType = RenderableManager.PrimitiveType.POINTS
            }
            addComponent(renderComponent)
        }
        val gizmo = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val gizmo = StaticMeshes.getGizmo(1f)
                setMesh(gizmo)
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.LINES
            }
            addComponent(renderComponent)
        }
        sceneObjectContainer.addObject(box)
        sceneObjectContainer.addObject(cameraObj)
        sceneObjectContainer.addObject(gizmo)
        sceneObjectContainer.addObject(pointCloud)


    }
    fun setBoxTransform(matrix:FloatArray){
        box.transformComponent.setTransform(this,matrix)

    }
    fun setCameraTransform(matrix:FloatArray){
        cameraObj.transformComponent.setTransform(this,matrix)
    }

    fun updatePointCloud(vertexes: List<StaticMeshes.MeshVertex>) {
        val indices = ShortArray(vertexes.size)
        for (i in 0 until  vertexes.size)
            indices[i] = i.toShort()
        pointCloudMesh.updateMesh(vertexes,indices.toList())
    }

    fun setCloudPointOrigin(res: FloatArray) {
        val tr = FloatArray(16)
//        android.opengl.Matrix.setIdentityM(tr,0)
//        android.opengl.Matrix.rotateM(tr,0,-90f,1f,0f,0f)
//        val out = FloatArray(16)
//        android.opengl.Matrix.multiplyMM(out,0,tr,0,res,0)
        pointCloud.transformComponent.setTransform(this,res)

//        val lt = pointCloud.transformComponent.getTransform(this)
//        val wt = pointCloud.transformComponent.getWorldTransform(this)
    }

    fun setCameraObjectVisibility(editMode: Boolean) {
        cameraObj.getComponent<MeshRendererComponent>()?.setVisibility(editMode)
    }
}