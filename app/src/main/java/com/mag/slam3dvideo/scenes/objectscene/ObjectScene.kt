package com.mag.slam3dvideo.scenes.objectscene

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Viewport
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.Manipulator
import com.mag.slam3dvideo.math.MatShared
import com.mag.slam3dvideo.math.toGlMatrix
import com.mag.slam3dvideo.orb3.MapPoint
import com.mag.slam3dvideo.orb3.Plane
import com.mag.slam3dvideo.resources.StaticMeshes
import com.mag.slam3dvideo.scenes.OrbScene
import com.mag.slam3dvideo.utils.CameraUtils

data class CameraCallibration(
    val x: Double,
    val h: Double,
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double
)

class ObjectScene(private val surfaceView: SurfaceView) : OrbScene {
    private var lastWidth: Int = 1
    private var lastHeight: Int = 1

    private var isEditMode: Boolean = true
    private var mMapPoints: List<MapPoint> = ArrayList()

    private var plane: Plane? = null
    private var cameraCallibration = CameraCallibration(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var sceneContext: ObjectSceneContext
    private lateinit var cameraManipulator: Manipulator
    private lateinit var gestureDetector: GestureDetector

    private val eyePos = DoubleArray(3)
    private val target = DoubleArray(3)
    private val upward = DoubleArray(3)

    private val surfaceGestureHandler = object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            gestureDetector.onTouchEvent(event!!);
            cameraManipulator.getLookAt(eyePos, target, upward)
            sceneContext.camera.lookAt(
                eyePos[0], eyePos[1], eyePos[2],
                target[0], target[1], target[2],
                upward[0], -1.0, upward[2]
            )
            return true
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun init(e: Engine) {
        sceneContext = ObjectSceneContext(e)
        sceneContext.initScene()
        cameraManipulator = Manipulator.Builder()
            .orbitHomePosition(0f, 0f, 1f)
            .targetPosition(0f, 0f, 0f)
            .orbitSpeed(0.01f, 0.01f)
            .build(Manipulator.Mode.ORBIT)
        gestureDetector = GestureDetector(surfaceView, cameraManipulator)
        updateGestureHandler()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateGestureHandler() {
        if (isEditMode) {
            surfaceView.setOnTouchListener(surfaceGestureHandler)
        } else
            surfaceView.setOnTouchListener(null)
    }

    override fun activate() {

    }

    override fun update() {
        sceneContext.update()
    }

    var i = 0
    override fun beforeRender(renderer: Renderer) {
        if(i == 0){
            val cameraObjectInitPos = FloatArray(16)
            android.opengl.Matrix.setIdentityM(cameraObjectInitPos,0)
            android.opengl.Matrix.translateM(cameraObjectInitPos,0,10f,10f,10f)
            sceneContext.setCameraTransform(cameraObjectInitPos)
            sceneContext.setBoxTransform(cameraObjectInitPos)
            i++
        }
    }

    override fun render(renderer: Renderer) {
//        if(plane == null)
//            return
        renderer.render(sceneContext.view)
    }

    private fun createMesh() {

    }

    fun setCameraCallibration(
        w: Double,
        h: Double,
        fx: Double,
        fy: Double,
        cx: Double,
        cy: Double
    ) {
        cameraCallibration = CameraCallibration(w, h, fx, fy, cx, cy)
        val doubleArray = CameraUtils.getProjectionMatrix(w, h, fx, fy, cx, cy, 0.001, 1000.0)
        sceneContext.camera.setCustomProjection(doubleArray, 0.001, 1000.0)
    }

    fun updateCameraMatrix(tcw: MatShared?) {
        if (tcw == null)
            return
        handler.post {
            //https://github.com/google/filament/blob/ba9cb2fe43f45c407e31fe197aa7e72d0e2810e5/filament/src/details/Camera.cpp#L201
            var twc = FloatArray(16)
            android.opengl.Matrix.invertM(twc, 0, tcw.toGlMatrix(), 0)
            sceneContext.camera.setModelMatrix(twc)
        }
    }


    fun setMapPoints(mapPoints: List<MapPoint>) {
        mMapPoints = mapPoints
        val vertexes = mMapPoints.map {
            StaticMeshes.MeshVertex(
                it.x,
                it.y,
                it.z,
                if (it.isReferenced) Color.GREEN else Color.RED
            )
        }
        sceneContext.updatePointCloud(vertexes)
    }

    override fun onResize(width: Int, height: Int) {
        lastWidth = width
        lastHeight = height

        updateCameraProjection(width, height)

    }

    private fun updateCameraProjection(width: Int, height: Int) {
        val near = 0.001
        val far = 1000.0
        if (isEditMode) {
            val aspect = width.toDouble() / height.toDouble()
            sceneContext.camera.setProjection(45.0, aspect, near, far, Camera.Fov.VERTICAL)
        } else {
            val doubleArray = CameraUtils.getProjectionMatrix(
                cameraCallibration.x,
                cameraCallibration.h,
                cameraCallibration.fx,
                cameraCallibration.fy,
                cameraCallibration.cx,
                cameraCallibration.cy,
                near, far
            )
            sceneContext.camera.setCustomProjection(doubleArray, near,far)
        }
        sceneContext.view.viewport = Viewport(0, 0, width, height)
        cameraManipulator.setViewport(width, height)
    }

    override fun destroy(engine: Engine) {
    }

    fun setPlane(p: Plane?) {
        if (p == null)
            return
        plane = p
        val glMatrix = plane!!.getGLTpw()
        val iv = FloatArray(16)
        android.opengl.Matrix.invertM(iv, 0, glMatrix, 0)
        handler.post {
            sceneContext.setBoxTransform(glMatrix)
        }
    }

    fun setCameraObjectTransform(tcw: MatShared?) {
        val glTcw = tcw?.toGlMatrix() ?: return
        val glTwc = FloatArray(16)
        android.opengl.Matrix.invertM(glTwc, 0, glTcw, 0)
        val glOw = FloatArray(16)  // world origin
        android.opengl.Matrix.setIdentityM(glOw, 0)

        //ORB_SLAM3\src\MapDrawer.cc#462
        glOw[12] = glTwc[12]
        glOw[13] = glTwc[13]
        glOw[14] = glTwc[14]

        sceneContext.setCameraTransform(glOw)
//        val res= FloatArray(16)
//        android.opengl.Matrix.invertM(res,0,glMatrix,0)
    }

    fun setCloudPointOrigin(tcw: MatShared?) {
        val glTcw = tcw?.toGlMatrix() ?: return
//        val glTwc = FloatArray(16)
//        android.opengl.Matrix.invertM(glTwc,0,glTcw,0)
//        val glOw  = FloatArray(16)  // world origin
//        android.opengl.Matrix.setIdentityM(glOw,0)
//
//        //ORB_SLAM3\src\MapDrawer.cc#462
//        glOw[12] = glTwc[12]
//        glOw[13] = glTwc[13]
//        glOw[14] = glTwc[14]
//
//        val localTransform = FloatArray(16)
//        android.opengl.Matrix.setIdentityM(localTransform,0)
//        android.opengl.Matrix.rotateM(localTransform,0,180f,0f,0f,1f)
//        val r = FloatArray(16)
//        android.opengl.Matrix.multiplyMM(r,0,glOw,0,localTransform,0)
//        sceneContext.setCloudPointOrigin(r)
//        val res= FloatArray(16)
//        android.opengl.Matrix.invertM(res,0,glMatrix,0)

    }

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        updateCameraProjection(lastWidth, lastHeight)
        updateGestureHandler()
        sceneContext.enableSkyBox(editMode)
        sceneContext.setCameraObjectVisibility(isEditMode)
    }

}