package com.mag.slam3dvideo.scenes.objectscene

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Viewport
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.Manipulator
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
    private val handler:Handler = Handler(Looper.getMainLooper())
    private lateinit var sceneContext: ObjectSceneContext
    private lateinit var cameraManipulator:Manipulator
    private lateinit var gestureDetector: GestureDetector

    private val eyePos = DoubleArray(3)
    private val target = DoubleArray(3)
    private val upward = DoubleArray(3)
    @SuppressLint("ClickableViewAccessibility")
    override fun init(e: Engine) {
        sceneContext = ObjectSceneContext(e)
        sceneContext.initScene()
        cameraManipulator = Manipulator.Builder()
            .orbitHomePosition(0f,1f,0f)
            .targetPosition(0f,0f,0.00f)
            .orbitSpeed(0.01f,0.01f)
            .build(Manipulator.Mode.ORBIT)
        //cameraManipulator.set
        gestureDetector = GestureDetector(surfaceView, cameraManipulator)
        surfaceView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event);
            cameraManipulator.getLookAt(eyePos, target, upward)
            sceneContext.camera.lookAt(
                eyePos[0], eyePos[1], eyePos[2],
                target[0], target[1], target[2],
                upward[0], upward[1], upward[2])
            return@setOnTouchListener true
        }
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
            val id = FloatArray(16)
            android.opengl.Matrix.setIdentityM(id,0)
//            android.opengl.Matrix.translateM(id,0,0f,0f,1f)
            sceneContext.setCameraTransform(id)
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
        cameraManipulator.setViewport(width,height)
    }

    override fun destroy(engine: Engine) {
    }

}