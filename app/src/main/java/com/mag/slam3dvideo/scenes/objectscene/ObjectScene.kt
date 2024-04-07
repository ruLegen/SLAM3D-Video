package com.mag.slam3dvideo.scenes.objectscene

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Renderer
import com.google.android.filament.VertexBuffer
import com.google.android.filament.Viewport
import com.mag.slam3dvideo.math.MatShared
import com.mag.slam3dvideo.math.toGlMatrix
import com.mag.slam3dvideo.orb3.MapPoint
import com.mag.slam3dvideo.orb3.Plane
import com.mag.slam3dvideo.scenes.OrbScene
import com.mag.slam3dvideo.utils.CameraUtils

data class CameraCallibration(val x:Double,val h:Double,val fx:Double,val fy:Double,val cx: Double,val cy: Double)
class ObjectScene(private val surfaceView: SurfaceView) : OrbScene {
    private var mMapPoints: List<MapPoint> = ArrayList()

    private var plane: Plane? = null
    private var cameraCallibration = CameraCallibration(0.0,0.0,0.0,0.0,0.0,0.0)

    private lateinit var indexBuffer: IndexBuffer
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var sceneContext: ObjectSceneContext
    private val handler:Handler = Handler(Looper.getMainLooper())
    override fun init(e: Engine) {
        sceneContext = ObjectSceneContext(e,)
        sceneContext.initScene()
    }
    override fun activate() {

    }
    override fun update() {
        sceneContext.update()
    }
    override fun beforeRender(renderer: Renderer) {

    }

    override fun render(renderer: Renderer) {
        if(plane == null)
            return
        renderer.render(sceneContext.view)
    }
    private fun createMesh() {

    }
    fun setCameraCallibration(w:Double,h:Double, fx:Double,fy:Double,cx:Double,cy:Double){
       cameraCallibration = CameraCallibration(w,h,fx,fy,cx,cy)
       val doubleArray = CameraUtils.getProjectionMatrix(w,h,fx,fy,cx,cy,0.001,1000.0)
       sceneContext.camera.setCustomProjection(doubleArray, 0.001, 1000.0)
    }
    fun updateCameraMatrix(tcw: MatShared?) {
        if(tcw == null)
            return
        handler.post{
            //https://github.com/google/filament/blob/ba9cb2fe43f45c407e31fe197aa7e72d0e2810e5/filament/src/details/Camera.cpp#L201
            var res= FloatArray(16)
            android.opengl.Matrix.invertM(res,0,tcw.toGlMatrix(),0)
            sceneContext.camera.setModelMatrix(res)
        }
    }


    fun setPlane(p:Plane?){
        if(p == null)
            return
        plane = p
        val glMatrix = plane!!.getGLTpw()
        handler.post{
            sceneContext.setBoxTransform(glMatrix)
        }
    }
    fun setMapPoints(mapPoints: List<MapPoint>) {
        mMapPoints = mapPoints

    }

    override fun onResize(width: Int, height: Int) {
        val near = 0.001
        val far = 1000.0
        val doubleArray = CameraUtils.getProjectionMatrix(cameraCallibration.x,
            cameraCallibration.h,
            cameraCallibration.fx,
            cameraCallibration.fy,
            cameraCallibration.cx,
            cameraCallibration.cy,
            near,far)
        sceneContext.camera.setCustomProjection(doubleArray, 0.001, 1000.0)
        sceneContext.view.viewport = Viewport(0, 0, width, height)
    }

    override fun destroy(engine: Engine) {
    }

}