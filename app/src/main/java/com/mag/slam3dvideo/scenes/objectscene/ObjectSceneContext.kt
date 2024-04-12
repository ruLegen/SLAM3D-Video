package com.mag.slam3dvideo.scenes.objectscene

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
import com.mag.slam3dvideo.render.mesh.DynamicMeshOf
import com.mag.slam3dvideo.resources.AssetMeshes
import com.mag.slam3dvideo.resources.StaticMaterials
import com.mag.slam3dvideo.resources.StaticMeshes
import com.mag.slam3dvideo.render.components.LightComponent

class ObjectSceneContext(engine: Engine) : SceneContext(engine) {
    var camera: Camera
        private set
    var view: View
        private set

    private var emptySkyBox: Skybox
    private lateinit var boxRoot: SceneObject
    private lateinit var cameraObjRoot: SceneObject
    private lateinit var cameraObj: SceneObject
    private lateinit var pointCloud: SceneObject
    private lateinit var pointCloudMesh: DynamicMeshOf<StaticMeshes.MeshVertex>
    init {
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

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
    /**
     * builds a scene with next hierarchy
     *  - box (10,10,10)
     *  - cameraMeshRoot (10,10,10)
     *      - cameraMesh (scaleDown, flipY, look towards +Z)
     *  - pointCLoud (0,0,0)
     *  - gizmo (0,0,0)
     */
    fun initScene() {
        // just move objects to (10x10y10z)
        val initialObjectPositions = FloatArray(16)
        android.opengl.Matrix.setIdentityM(initialObjectPositions,0)
        android.opengl.Matrix.translateM(initialObjectPositions,0,10f,10f,10f)

        boxRoot = SceneObject().apply {
            transformComponent.setTransform(this@ObjectSceneContext,initialObjectPositions)
        }
        val box = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val meshSize = 0.025f
//                val mesh = StaticMeshes.getCubeMesh(meshSize,0f,-meshSize,0f)

                setMesh(AssetMeshes.getCamaro())
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLES
            }
            addComponent(renderComponent)
            val initialTransform = FloatArray(16)
            val size = 0.13f
            android.opengl.Matrix.setIdentityM(initialTransform,0)
            android.opengl.Matrix.scaleM(initialTransform,0,size,-size,size)            // flip Y
            android.opengl.Matrix.rotateM(initialTransform,0,180f,0f,1f,0f)    // rotate that forward looked to +Z
            transformComponent.setTransform(this@ObjectSceneContext,initialTransform)
            //transformComponent.setTransform(this@ObjectSceneContext,initialObjectPositions)
        }
        boxRoot.addChild(this,box)


        cameraObjRoot = SceneObject().apply {
            transformComponent.setTransform(this@ObjectSceneContext,initialObjectPositions)
        }
        cameraObj = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                val mesh = AssetMeshes.getCamera()
                setMesh(mesh)
                val material = StaticMaterials.getMeshMaterial(this@ObjectSceneContext)
                setMaterialInstance(material.createInstance())
                primitiveType = RenderableManager.PrimitiveType.TRIANGLES
            }
            addComponent(renderComponent)
            val initialTransform = FloatArray(16)
            val size = 0.03f
            android.opengl.Matrix.setIdentityM(initialTransform,0)
            android.opengl.Matrix.scaleM(initialTransform,0,size,-size,size)            // flip Y
            android.opengl.Matrix.rotateM(initialTransform,0,180f,0f,1f,0f)    // rotate that forward looked to +Z
            transformComponent.setTransform(this@ObjectSceneContext,initialTransform)
        }
        cameraObjRoot.addChild(this,cameraObj)
        pointCloud = SceneObject().apply {
            val renderComponent = MeshRendererComponent().apply {
                pointCloudMesh = StaticMeshes.getDynamicMesh(8000) as DynamicMeshOf<StaticMeshes.MeshVertex>
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
//        sceneObjectContainer.addObject(light)
        sceneObjectContainer.addObject(boxRoot)
        sceneObjectContainer.addObject(cameraObjRoot)
        sceneObjectContainer.addObject(gizmo)
        sceneObjectContainer.addObject(pointCloud)
    }
    fun setBoxTransform(matrix:FloatArray){
        boxRoot.transformComponent.setTransform(this,matrix)
    }
    fun setCameraTransform(matrix:FloatArray){
        cameraObjRoot.transformComponent.setTransform(this,matrix)
    }

    fun updatePointCloud(vertexes: List<StaticMeshes.MeshVertex>) {
        val indices = ShortArray(vertexes.size)
        for (i in 0 until  vertexes.size)
            indices[i] = i.toShort()
        pointCloudMesh.updateMesh(vertexes,indices.toList())
    }

    fun setCameraObjectVisibility(editMode: Boolean) {
        cameraObj.getComponent<MeshRendererComponent>()?.setVisibility(editMode)
    }
}