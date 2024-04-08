package com.mag.slam3dvideo.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.SwapChain
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.Utils
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.databinding.ActivityMapViewBinding
import com.mag.slam3dvideo.orb3.OrbSlamProcessor
import com.mag.slam3dvideo.orb3.Plane
import com.mag.slam3dvideo.orb3.TrackingState
import com.mag.slam3dvideo.scenes.KeypointsScene
import com.mag.slam3dvideo.scenes.objectscene.ObjectScene
import com.mag.slam3dvideo.scenes.OrbScene
import com.mag.slam3dvideo.scenes.VideoFrameListener
import com.mag.slam3dvideo.scenes.VideoScene
import com.mag.slam3dvideo.ui.components.MediaPlayerControlCallback
import com.mag.slam3dvideo.ui.components.SurfaceMediaPlayerControl
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.utils.BufferQueue
import com.mag.slam3dvideo.utils.OrbFrameInfoHolder
import com.mag.slam3dvideo.utils.TaskRunner
import com.mag.slam3dvideo.utils.video.SpeedControlCallback
import com.mag.slam3dvideo.utils.video.VideoDecoder
import com.mag.slam3dvideo.utils.video.VideoFrameRetriever
import com.mag.slam3dvideo.utils.video.VideoPlaybackCallback
import org.opencv.android.OpenCVLoader
import java.io.Closeable
import java.io.File
import kotlin.math.roundToInt


class SurfaceControlCallback(
    val videoDecoder: VideoDecoder,
    val decoderSpeedControlCallback: SpeedControlCallback
) : MediaPlayerControlCallback {
    var currentDecodeFrame: Int = 0
    override fun onStart(sender: MediaController.MediaPlayerControl) {
        decoderSpeedControlCallback.resume()
    }

    override fun onPause(sender: MediaController.MediaPlayerControl) {
        decoderSpeedControlCallback.pause()
    }

    override fun onSeek(sender: MediaController.MediaPlayerControl, pos: Int) {
        val isBackwards = pos < 0
        if (isBackwards)
            videoDecoder.seekTo(0)
        else
            videoDecoder.seekTo((videoDecoder.mFrameTimeUs * currentDecodeFrame).toLong())
    }
}

class MapViewActivity : AppCompatActivity() {
    data class BitmapItem(var frameNumber: Int, var bitmap: Bitmap?) : Closeable {
        override fun close() {
            try {
                bitmap?.recycle()
            } finally {
                bitmap = null
            }
        }

    }

    companion object {
        init {
            Filament.init()
            Utils.init()
            OpenCVLoader.initDebug()
            MaterialBuilder.init()
        }
    }


    private var mFrameOffset: Int = 0
    private var shouldRegenPlane: Boolean = false
    private lateinit var surfaceControlCallback: SurfaceControlCallback
    private lateinit var decoderSpeedControlCallback: SpeedControlCallback
    private lateinit var videoDecoder: VideoDecoder
    private lateinit var binding: ActivityMapViewBinding
    private lateinit var infoText: TextView
    private var plane: Plane? = null

    private lateinit var orbProcessor: OrbSlamProcessor
    private lateinit var frameBufferQueue: BufferQueue<BitmapItem>
    private var imageDecoderTaskRunner: TaskRunner? = null
    private var frameProcessorTaskRunner: TaskRunner? = null
    private var imagePreviewTaskRunner: TaskRunner? = null

    //    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
//    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20240223_143249538.mp4"
    var file: String = "/storage/emulated/0/DCIM/Camera/with_frames.mp4"

    private lateinit var surfaceView: SurfaceMediaPlayerControl
    private lateinit var uiHelper: UiHelper
    private lateinit var displayHelper: DisplayHelper
    private lateinit var choreographer: Choreographer
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private var swapChain: SwapChain? = null
    private val handler = Handler(Looper.getMainLooper())

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)
    private var videoRetriever: VideoFrameRetriever? = null
    private lateinit var videoScene: VideoScene;
    private lateinit var keypointsScene: KeypointsScene
    private lateinit var objectScene: ObjectScene
    private lateinit var orbFrameInfoHolder: OrbFrameInfoHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)
        val path = intent.getStringExtra("path")
        if (path != null)
            file = path
//        val objLoader = ObjLoader(this,"Models/camera.obj")

        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fpsButton.setOnClickListener {
            askUserInputInt("Enter FPS", null) {
                decoderSpeedControlCallback.setFixedPlaybackRate(it)
            }
        }
        binding.offsetButton.setOnClickListener {
            askUserInputInt("Enter frame offset", mFrameOffset.toString()) {
                mFrameOffset = it;
            }
        }
        binding.planeButton.setOnClickListener {
            shouldRegenPlane = true
        }
        surfaceView = binding.surfaceView
        surfaceView.setMediaController(MediaController(this))
        infoText = binding.infoText
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupSurfaceView()
        setupFilament()
        if (!AssetUtils.askMediaPermissions(this, 1)) {
            initVideoFrameGraber()
        }
    }

    private fun askUserInputInt(title: String, value: String?, callback: (i: Int) -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED;
        value?.let {
            input.setText(value);
        }
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, which ->
            val v = input.text.toString().toInt()
            callback?.invoke(v);
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }
        builder.show()
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
            setInfoText("Cannot start app, no media permissions granted")
            Toast.makeText(
                this,
                "Cannot start app, no media permissions granted",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private fun setInfoText(s: String) {
        handler.post {
            infoText.text = s
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

    }

    private fun setupScenes() {
        videoScene = VideoScene(surfaceView, videoRetriever!!.frameSize)
        keypointsScene = KeypointsScene(surfaceView)
        objectScene = ObjectScene(surfaceView)
        allScenes().forEach { it.init(engine) }

        objectScene.setCameraCallibration(
            1920.0,
            1080.0,
            1364.21109,
            1350.67390,
            960.52704,
            523.65354
        )
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

                allScenes().forEach {
                    it.update()
                }
//                Matrix.setRotateM(transformMatrix, 0, -(a.animatedValue as Float), 0.0f, 0.0f, 1.0f)
//                val tcm = engine.transformManager
//                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
//              ///  this@MapViewActivity.surfaceView.setBackgroundColor(Color.argb(1f,(a.currentPlayTime % 5000) / 5000f,0f,0f))
            }
        })
        animator.start()
    }

    private fun initVideoFrameGraber() {
        frameBufferQueue = BufferQueue(7)
        videoRetriever = VideoFrameRetriever(file);

        setInfoText("Initializing OrbSlamProcessor")
        val orbAssets = AssetUtils.getOrbFileAssets(this)
        orbProcessor = OrbSlamProcessor(orbAssets.vocabFile, orbAssets.configFile)
        videoRetriever!!.initialize()

        setupScenes()

        decoderSpeedControlCallback = SpeedControlCallback(object : VideoPlaybackCallback {
            override fun preRender(progress: Long) {
//                Log.d("speed", "$progress")
            }

            override fun postRender() {
            }
        })
        videoDecoder = VideoDecoder(File(file), videoScene.surface, decoderSpeedControlCallback)
        decoderSpeedControlCallback.setFixedPlaybackRate(30)
        decoderSpeedControlCallback.mTotalDuraionUsec = videoDecoder.mVideoDuration.toFloat()

        videoScene.setTotalDuration(videoDecoder.mVideoDuration)
        videoScene.setTotalFrameCount(videoRetriever!!.frameCount)
        videoScene.setVideoFrameListener(object : VideoFrameListener {
            var lastFrame = 0L
            override fun frameIsAboutToDraw(timestamp: Long) {
                if (lastFrame != timestamp) {
                    val totalDuration = videoDecoder.mVideoDuration
                    val progress = timestamp / totalDuration.toFloat()
                    val frame = (progress * (videoRetriever?.frameCount ?: 0))
                    val roundFrame = frame.roundToInt()
//                    Log.d("scene","current $timestamp = $roundFrame")
                    setupPreviewForFrame(roundFrame + mFrameOffset)
                    lastFrame = timestamp
                }
            }
        })


        surfaceControlCallback = SurfaceControlCallback(videoDecoder, decoderSpeedControlCallback)
        surfaceView.mediaControlCallback = surfaceControlCallback

        orbFrameInfoHolder = OrbFrameInfoHolder(videoRetriever!!.frameCount.toInt())
        imagePreviewTaskRunner = TaskRunner()
        imageDecoderTaskRunner = TaskRunner().apply {
            executeAsync({ decodeFrame(0) })
        }
        frameProcessorTaskRunner = TaskRunner().apply {
            executeAsync({ processFrame(0) })
        }
//        videoDecoder.start()
    }

    private fun decodeFrame(frame: Int) {
        val b = frameBufferQueue.getBufferToProduce()!!
        val decodedBitmap = videoRetriever?.getFrame(frame)
        b.value = BitmapItem(frame, decodedBitmap)

        frameBufferQueue.releaseProducedBuffer(b);
        if (frame < (videoRetriever?.frameCount ?: 0)) {
            imageDecoderTaskRunner?.executeAsync({ decodeFrame(frame + 1) })
        }
    }

    var i = 0

    @SuppressLint("SetTextI18n")
    private fun processFrame(retryNum: Int) {
        if (retryNum > 2)
            return

        val decodedBitmap = frameBufferQueue.getBufferToConsume()
        val isValid = decodedBitmap!!.value?.bitmap != null
        if (!isValid) {
            frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
            frameProcessorTaskRunner?.executeAsync({
                processFrame(retryNum + 1)
            })
            return
        }

        val bitmapItem = decodedBitmap.value!!
        val bitmap = bitmapItem.bitmap!!
        val frameNumber = bitmapItem.frameNumber

        surfaceControlCallback.currentDecodeFrame = frameNumber
        val channels = when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.ALPHA_8 -> 1
            else -> {
                -1
            }
        }
        if (channels == -1)
            throw Exception("Unsupported bitmap mode ${bitmap.config}")
        //videoScene.processBitmap(bitmap)
        val tcw = orbProcessor.processFrame(bitmap)
        orbFrameInfoHolder.setCameraPosAtFrame(frameNumber, tcw)
        val state = orbProcessor.getTrackingState()
        if (state == TrackingState.OK) {
            if (i == 15 || shouldRegenPlane) {
                shouldRegenPlane = false
                plane = orbProcessor.detectPlane()
                objectScene.setPlane(plane)
                i++
            } else {
                i++
            }
            val keys = orbProcessor.getCurrentFrameKeyPoints()
            orbFrameInfoHolder.setKeypointsAtFrame(frameNumber, keys)
            val mapPoints = orbProcessor.getCurrentMapPoints()
            objectScene.setMapPoints(mapPoints)

        }

        keypointsScene.drawingRect = videoScene.drawingRect
        keypointsScene.setBitmapInfo(videoScene.bitmapStretch, videoScene.bitmapSize)
//        Log.d("DECODER", "Decoded ${frameNumber}/${videoRetriever?.frameCount}")
        setInfoText("Decoded $frameNumber/${videoRetriever?.frameCount} | State = $state")
        frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
        frameProcessorTaskRunner?.executeAsync({
            processFrame(0)
        })
    }

    private fun setupPreviewForFrame(frameNumber: Int) {
        if (frameNumber.toLong() == (videoRetriever?.frameCount ?: frameNumber))
            return
        val cameraPos = orbFrameInfoHolder.getCameraPosAtFrame(frameNumber)
        val points = orbFrameInfoHolder.getKeypointsAtFrame(frameNumber)
        val size = videoRetriever!!.frameSize
        videoScene.processBitmap(RectF(0f, 0f, size.width, size.height))
        objectScene.updateCameraMatrix(cameraPos)
        keypointsScene.updateKeypoints(points)
//        Thread.sleep(100)
//        imagePreviewTaskRunner?.executeAsync({ previewImage(frameNumber + 1) })
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
        videoScene.destroy(engine)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        engine.destroy()
    }

    fun allScenes(): Array<OrbScene> {
        return arrayOf(videoScene, objectScene, keypointsScene)
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
                        it.beforeRender(renderer)
                    }
                    allScenes().forEach {
                        it.render(renderer)
                    }
                    renderer.endFrame()
                    engine.flushAndWait()
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
            allScenes().forEach {
                it.onResize(width, height)
            }
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

}