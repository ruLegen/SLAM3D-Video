package com.mag.slam3dvideo.ui

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.Material.UserVariantFilterBit
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.SKIP_BITMAP_COPY
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.utils.BufferQueue
import com.mag.slam3dvideo.utils.TaskRunner
import com.mag.slam3dvideo.utils.TextureUtils
import com.mag.slam3dvideo.utils.video.VideoFrameRetriever
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch


class MapViewActivity : AppCompatActivity() {
    // Make sure to initialize Filament first
    // This loads the JNI library needed by most API calls
    companion object {
        init {
            Filament.init()

            MaterialBuilder.init()
        }
    }
    private var videoTextureSampler: TextureSampler = TextureSampler(TextureSampler.MinFilter.LINEAR, TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT)
    private lateinit var frameBufferQueue: BufferQueue<Bitmap?>
    private var imageDecoderTaskRunner: TaskRunner? = null
    private var textureUpdaterTaskRunner: TaskRunner? = null

//    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20240223_143249538.mp4"

    
    private var matInstance: MaterialInstance? = null

    // The View we want to render into
    private lateinit var surfaceView: SurfaceView

    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    private lateinit var uiHelper: UiHelper

    // DisplayHelper is provided by Filament to manage the display
    private lateinit var displayHelper: DisplayHelper

    // Choreographer is used to schedule new frames
    private lateinit var choreographer: Choreographer

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    private lateinit var engine: Engine

    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer

    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var scene: Scene

    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View

    // Should be pretty obvious :)
    private lateinit var camera: Camera

    private lateinit var material: Material
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var indexBuffer: IndexBuffer
    private lateinit var videoTexture: Texture

    // Filament entity representing a renderable object
    @Entity
    private var renderable = 0

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()

    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    private var videoRetriever: VideoFrameRetriever? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(this)
        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
        if (!AssetUtils.askMediaPermissions(this, 1)) {
            initVideoFrameGraber()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
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
    private fun initVideoFrameGraber() {
        frameBufferQueue = BufferQueue(5)
        videoRetriever = VideoFrameRetriever(file);
        videoRetriever!!.initialize()
        imageDecoderTaskRunner = TaskRunner().apply {
            executeAsync({ decodeFrame(0) })
        }
        textureUpdaterTaskRunner = TaskRunner().apply {
            executeAsync({ updateTexture(0) })
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

    private fun updateTexture(retryNum: Int) {
        if (retryNum > 2)
            return

        val decodedBitmap = frameBufferQueue.getBufferToConsume()
        val isValid = decodedBitmap!!.value != null
        if (!isValid) {
            frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
            textureUpdaterTaskRunner?.executeAsync({
                updateTexture(retryNum + 1)
            })
            return
        }

        val bitmap = decodedBitmap.value!!
        val w = videoTexture.getWidth(0)
        val h = videoTexture.getHeight(0)
        val channels = when(bitmap.config){
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.ALPHA_8 -> 1
            else -> {-1}
        }
        if(channels == -1)
            throw Exception("Unsupported bitmap mode ${bitmap.config}")
        var processTexture = videoTexture
        if(w != bitmap.width || h != bitmap.height) {
            val latch = CountDownLatch(1)
            handler.post(Runnable { // Code to run on the UI thread
                val newTexture = Texture.Builder()
                    .width(bitmap.width)
                    .height(bitmap.height)
                    .levels(0xff)
                    .format(Texture.InternalFormat.RGBA8)
                    .sampler(Texture.Sampler.SAMPLER_2D)
                    .build(engine);
                processTexture = newTexture
                latch.countDown()
            })
            try {
                latch.await()
            }catch (ex:Exception){}
        }
        val format = TextureUtils.format(bitmap)
        val type = TextureUtils.type(bitmap)

        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.flip()
        bitmap.recycle()

        val latch = CountDownLatch(1)
        handler.post {
            val newBuffer = PixelBufferDescriptor(buffer,format,type)
            processTexture.setImage(engine, 0, newBuffer)
            videoTexture = processTexture
            matInstance?.setParameter("videoTexture", videoTexture, videoTextureSampler)
            latch.countDown()
        }
        try {
            latch.await()
        }catch (ex:Exception){}
        frameBufferQueue.releaseConsumedBuffer(decodedBitmap)
        textureUpdaterTaskRunner?.executeAsync({
            updateTexture(0)
        })
    }


    // Not required when SKIP_BITMAP_COPY is true


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
    scene = engine.createScene()
    view = engine.createView()
    camera = engine.createCamera(engine.entityManager.create())
}

private fun setupView() {
//        scene.skybox = Skybox.Builder().color(1.0f, 1f, 1f, 1.0f).build(engine)
    // post-processing is not supported at feature level 0
    view.isPostProcessingEnabled = false
    // Tell the view which camera we want to use
    view.camera = camera
    // Tell the view which scene we want to render
    view.scene = scene
}

private fun setupScene() {
    loadMaterial()
    createMesh()
    createTexture()
    // To create a renderable we first create a generic entity
    renderable = EntityManager.get().create()
    matInstance = material.createInstance()
        .apply {
            setParameter("baseColor", Colors.RgbaType.LINEAR, 1f, 0f, 0f, 1f)
        }
    RenderableManager.Builder(1)
        .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
        .geometry(0, PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer, 0, indexBuffer.indexCount)
        .material(0, matInstance!!)
        .culling(false)
        .build(engine, renderable)
    scene.addEntity(renderable)

    startAnimation()
}

private fun createTexture() {
    videoTexture = Texture.Builder()
        .width(1)
        .height(1)
        .levels(1)
        .format(Texture.InternalFormat.RGBA8)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .build(engine)
    var buffer =
        PixelBufferDescriptor(ByteBuffer.allocate(128), Texture.Format.RGBA, Texture.Type.UBYTE);
    videoTexture.setImage(engine, 0, buffer);
}

private fun loadMaterial() {
    readUncompressedAsset("materials/baked_color.filamat").let {
        material = Material.Builder().payload(it, it.remaining()).build(engine)
        material.compile(
            Material.CompilerPriorityQueue.HIGH,
            UserVariantFilterBit.ALL,
            Handler(Looper.getMainLooper())
        ) {
            android.util.Log.i(
                "hellotriangle",
                "Material " + material.name + " compiled."
            )
        }
        engine.flush()
    }
}

private fun createMesh() {
    val vertexSize = 6 * Float.SIZE_BYTES

    data class Vertex(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float,
        val u: Float,
        val v: Float
    )

    fun ByteBuffer.put(v: Vertex): ByteBuffer {
        putFloat(v.x)
        putFloat(v.y)
        putFloat(v.z)
        putFloat(v.w)
        putFloat(v.u)
        putFloat(v.v)
        return this
    }

    var verticies = arrayOf(
        Vertex(1f, 1.0f, 0.0f, 1f, 1f, 1f),
        Vertex(-1f, 1f, 0.0f, 1f, 0f, 1f),
        Vertex(-1f, -1f, 0.0f, 1f, 0f, 0f),
        Vertex(1f, -1f, 0.0f, 1f, 1f, 0f)
    )

    val vertexData = ByteBuffer.allocate(verticies.size * vertexSize)
        .order(ByteOrder.nativeOrder())
        .also {
            verticies.forEach { v -> it.put(v) }
        }
        .flip()

    vertexBuffer = VertexBuffer.Builder()
        .bufferCount(1)
        .vertexCount(verticies.size)
        .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT4, 0, vertexSize)
        .attribute(VertexAttribute.UV0, 0, AttributeType.FLOAT2, 4 * Float.SIZE_BYTES, vertexSize)
        .build(engine)
    vertexBuffer.setBufferAt(engine, 0, vertexData)

    val indeces = arrayOf<Short>(0, 1, 2, 2, 3, 0)
    val indexData = ByteBuffer.allocate(indeces.size * Short.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .also {
            indeces.forEach { i -> it.putShort(i) }
        }
        .flip()
    indexBuffer = IndexBuffer.Builder()
        .indexCount(indeces.size)
        .bufferType(IndexBuffer.Builder.IndexType.USHORT)
        .build(engine)
    indexBuffer.setBuffer(engine, indexData)
}

private fun startAnimation() {
    // Animate the triangle
    animator.interpolator = LinearInterpolator()
    animator.duration = 4000
    animator.repeatMode = ValueAnimator.RESTART
    animator.repeatCount = ValueAnimator.INFINITE
    animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
        val transformMatrix = FloatArray(16)
        override fun onAnimationUpdate(a: ValueAnimator) {
//                Matrix.setRotateM(transformMatrix, 0, -(a.animatedValue as Float), 0.0f, 0.0f, 1.0f)
//                val tcm = engine.transformManager
//                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
//              ///  this@MapViewActivity.surfaceView.setBackgroundColor(Color.argb(1f,(a.currentPlayTime % 5000) / 5000f,0f,0f))
            matInstance?.setParameter(
                "baseColor",
                Colors.RgbaType.LINEAR,
                0f,
                (a.currentPlayTime % 5000) / 5000f,
                0f,
                1f
            )
        }
    })
    animator.start()
}

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
    engine.destroyEntity(renderable)
    engine.destroyRenderer(renderer)
    engine.destroyVertexBuffer(vertexBuffer)
    engine.destroyIndexBuffer(indexBuffer)
    engine.destroyMaterial(material)
    engine.destroyView(view)
    engine.destroyScene(scene)
    engine.destroyCameraComponent(camera.entity)

    // Engine.destroyEntity() destroys Filament related resources only
    // (components), not the entity itself
    val entityManager = EntityManager.get()
    entityManager.destroy(renderable)
    entityManager.destroy(camera.entity)

    // Destroying the engine will free up any resource you may have forgotten
    // to destroy, but it's recommended to do the cleanup properly
    engine.destroy()
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
                renderer.render(view)
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
//            camera.setProjection(Camera.Projection.ORTHO,-aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0)

        view.viewport = Viewport(0, 0, width, height)

        FilamentHelper.synchronizePendingFrames(engine)
    }
}

private fun readUncompressedAsset(assetName: String): ByteBuffer {
    val mat = MaterialBuilder()
        .name("backed_color")
        .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
        .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_2D,MaterialBuilder.SamplerFormat.FLOAT,MaterialBuilder.ParameterPrecision.DEFAULT,"videoTexture")

        .require(MaterialBuilder.VertexAttribute.UV0)
        .platform(MaterialBuilder.Platform.MOBILE)
        .shading(MaterialBuilder.Shading.UNLIT)
        .flipUV(true)
        .vertexDomain(MaterialBuilder.VertexDomain.DEVICE)
        // .depthWrite(false)
        .variantFilter(
            UserVariantFilterBit.SKINNING.or(UserVariantFilterBit.SHADOW_RECEIVER)
                .or(UserVariantFilterBit.VSM)
        )
        .culling(MaterialBuilder.CullingMode.NONE)
        .material(
            """
               void material(inout MaterialInputs material) {
                   prepareMaterial(material);
                   material.baseColor =  texture(materialParams_videoTexture, getUV0());
               }
            """
        )
        .build()
    return mat.buffer
//        assets.openFd(assetName).use { fd ->
//            val input = fd.createInputStream()
//
//            val dst = ByteBuffer.allocate(fd.length.toInt())
//
//            val src = Channels.newChannel(input)
//            src.read(dst)
//            src.close()
//
//            return dst.apply { rewind() }
//        }
}
}