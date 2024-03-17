package com.mag.slam3dvideo.ui

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.mag.slam3dvideo.orb3.OrbSlamProcessor
import com.mag.slam3dvideo.orb3.TrackingState
import com.mag.slam3dvideo.scenes.KeypointsScene
import com.mag.slam3dvideo.scenes.ObjectScene
import com.mag.slam3dvideo.scenes.OrbScene
import com.mag.slam3dvideo.scenes.VideoScene
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.utils.BufferQueue
import com.mag.slam3dvideo.utils.TaskRunner
import com.mag.slam3dvideo.utils.video.VideoFrameRetriever
import org.opencv.android.OpenCVLoader


class MapViewActivity : AppCompatActivity() {
    companion object {
        init {
            Filament.init()
            OpenCVLoader.initDebug()
            MaterialBuilder.init()
        }
    }

    private lateinit var orbProcessor: OrbSlamProcessor
    private lateinit var frameBufferQueue: BufferQueue<Bitmap?>
    private var imageDecoderTaskRunner: TaskRunner? = null
    private var frameProcessorTaskRunner: TaskRunner? = null

    //    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20240223_143249538.mp4"

    private lateinit var surfaceView: SurfaceView

    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    private lateinit var uiHelper: UiHelper

    // DisplayHelper is provided by Filament to manage the display
    private lateinit var displayHelper: DisplayHelper

    // Choreographer is used to schedule new frames
    private lateinit var choreographer: Choreographer
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer

    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)
    private var videoRetriever: VideoFrameRetriever? = null
    private lateinit var videoScene: VideoScene;
    private lateinit var keypointsScene: KeypointsScene
    private lateinit var objectScene: ObjectScene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupSurfaceView()
        setupFilament()
        setupScenes()
        if (!AssetUtils.askMediaPermissions(this, 1)) {
            initVideoFrameGraber()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (AssetUtils.hasAllMediaPermissions(this)) {
            initVideoFrameGraber()
        } else {
            Toast.makeText(
                this,
                "Cannot start app, no media permissions granted",
                Toast.LENGTH_LONG
            ).show();
        }
    }


    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()
        // NOTE: To choose a specific rendering resolution, add the following line:
        // uiHelper.setDesiredSize(1280, 720)
        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.Builder().featureLevel(Engine.FeatureLevel.FEATURE_LEVEL_1).build()
        renderer = engine.createRenderer()
        view = engine.createView()

    }
    private fun setupScenes() {
        videoScene = VideoScene(surfaceView)
        keypointsScene = KeypointsScene(surfaceView)
        objectScene = ObjectScene(surfaceView)

        allScenes().forEach { it.init(engine) }

        startAnimation()
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 4000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(a: ValueAnimator) {
                keypointsScene.updateTransform()
//                Matrix.setRotateM(transformMatrix, 0, -(a.animatedValue as Float), 0.0f, 0.0f, 1.0f)
//                val tcm = engine.transformManager
//                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
//              ///  this@MapViewActivity.surfaceView.setBackgroundColor(Color.argb(1f,(a.currentPlayTime % 5000) / 5000f,0f,0f))
            }
        })
        animator.start()
    }
    private fun initVideoFrameGraber() {
        frameBufferQueue = BufferQueue(5)
        videoRetriever = VideoFrameRetriever(file);
        val orbAssets = AssetUtils.getOrbFileAssets(this)
        orbProcessor = OrbSlamProcessor(orbAssets.vocabFile, orbAssets.configFile)
        videoRetriever!!.initialize()
        imageDecoderTaskRunner = TaskRunner().apply {
            executeAsync({ decodeFrame(0) })
        }
        frameProcessorTaskRunner = TaskRunner().apply {
            executeAsync({ processFrame(0) })
        }
    }

    private fun decodeFrame(frame: Int) {
        val b = frameBufferQueue.getBufferToProduce()!!
        val decodedBitmap = videoRetriever?.getFrame(frame)
        b.value = decodedBitmap
        frameBufferQueue.releaseProducedBuffer(b);
        Log.d("DECODER", "Decoded ${frame}/${videoRetriever?.frameCount}")
        if (decodedBitmap != null) {
            imageDecoderTaskRunner?.executeAsync({ decodeFrame(frame + 1) })
        }
    }

    private fun processFrame(retryNum: Int) {
        if (retryNum > 2)
            return

        val decodedBitmap = frameBufferQueue.getBufferToConsume()
        val isValid = decodedBitmap!!.value != null
        if (!isValid) {
            frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
            frameProcessorTaskRunner?.executeAsync({
                processFrame(retryNum + 1)
            })
            return
        }

        val bitmap = decodedBitmap.value!!
        val channels = when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.ALPHA_8 -> 1
            else -> {
                -1
            }
        }
        if (channels == -1)
            throw Exception("Unsupported bitmap mode ${bitmap.config}")

        videoScene.processBitmap(bitmap)
        val tcw = orbProcessor.processFrame(bitmap)
        val state = orbProcessor.getTrackingState()
        if (state == TrackingState.OK) {
            val keys = orbProcessor.getCurrentFrameKeyPoints()
            keypointsScene.updateKeypoints(keys)
        }
        keypointsScene.drawingRect = videoScene.drawingRect
        keypointsScene.setBitmapInfo(videoScene.bitmapStretch,videoScene.bitmapSize)

        bitmap.recycle()
        frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
        frameProcessorTaskRunner?.executeAsync({
            processFrame(0)
        })
    }


    // Not required when SKIP_BITMAP_COPY is true


    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
        animator.start()
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop the animation and any pending frame
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel();

        // Always detach the surface before destroying the engine
        uiHelper.detach()

        // Cleanup all resources
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        videoScene.destroy(engine)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        engine.destroy()
    }

    fun  allScenes():Array<OrbScene>{
        return  arrayOf(videoScene,keypointsScene,objectScene)
    }
    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    allScenes().forEach {
                        it.activate(view)
                        renderer.render(view)
                    }
                    renderer.endFrame()
                }
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }

            // at feature level 0, we don't have post-processing, so we need to set
            // the colorspace to sRGB (FIXME: it's not supported everywhere!)
            var flags = uiHelper.swapChainFlags
            if (engine.activeFeatureLevel == Engine.FeatureLevel.FEATURE_LEVEL_0) {
                if (SwapChain.isSRGBSwapChainSupported(engine)) {
                    flags = flags or SwapChain.CONFIG_SRGB_COLORSPACE
                }
            }

            swapChain = engine.createSwapChain(surface, flags)
            displayHelper.attach(renderer, surfaceView.display);
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach();
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
//            val zoom = 1.5
//            val aspect = width.toDouble() / height.toDouble()
//            camera.setProjection(Camera.Projection.ORTHO,-aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0)S
            videoScene.onResize(width,height)
            view.viewport = Viewport(0, 0, width, height)
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }


}