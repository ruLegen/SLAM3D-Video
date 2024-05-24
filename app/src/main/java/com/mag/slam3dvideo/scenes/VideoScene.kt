package com.mag.slam3dvideo.scenes

import android.graphics.ImageFormat
import android.graphics.RectF
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SizeF
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.filamat.MaterialBuilder
import com.mag.slam3dvideo.utils.MathHelpers
import com.mag.slam3dvideo.utils.TextureSurface
import com.mag.slam3dvideo.utils.bitmap.BitmapAlignment
import com.mag.slam3dvideo.utils.bitmap.BitmapStretch
import com.mag.slam3dvideo.utils.bitmap.getTransform
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.cvtColor
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface VideoFrameListener
{
    fun frameIsAboutToDraw(timestamp:Long);
}
class VideoScene(private val surfaceView: SurfaceView, bitmapSize: SizeF ) : OrbScene {
    companion object{
        const val TAG="VideoScene"
    }

    private var isEnabled: Boolean = true
    private var frameListener: VideoFrameListener? = null
    private var lastVideoFrameTimeUsec: Long = 0
    private lateinit var tex: TextureSurface
    private var hasImages: Boolean =false
    private lateinit var imageReader: ImageReader
    private lateinit var filamentStream: Stream
    private lateinit var filamentTexture: Texture
    private lateinit var view: View
    var drawingRect: RectF = RectF()
        private set
    var bitmapStretch: BitmapStretch = BitmapStretch.Fill
        private set
    var bitmapSize: SizeF = bitmapSize
        private set

    val surface: Surface
        get() = tex.mSurface

    private lateinit var scene: Scene
    private lateinit var camera: Camera
    private var matInstance: MaterialInstance? = null

    @Entity
    private var renderable = 0
    private lateinit var material: Material
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var indexBuffer: IndexBuffer
    private lateinit var videoTexture: Texture
    private var videoTextureSampler: TextureSampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR, TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT
    )
    private lateinit var engine: Engine
    private val handler = Handler(Looper.getMainLooper())

    init {
    }
    override fun init(e: Engine) {
        tex = TextureSurface(bitmapSize.width.toInt(),bitmapSize.height.toInt(),Handler(Looper.myLooper()!!)) { buf, timestamp ->
            val newBuffer = Texture.PixelBufferDescriptor(buf, Texture.Format.RGBA, Texture.Type.UBYTE)
            videoTexture.setImage(engine, 0, newBuffer)
//            Log.d("tex","texture $timestamp")
            lastVideoFrameTimeUsec = timestamp
        }
        engine = e
        view = engine.createView()

        scene = engine.createScene()
        camera = engine.createCamera(engine.entityManager.create())
        imageReader = ImageReader.newInstance(
            bitmapSize.width.toInt(),
            bitmapSize.height.toInt(),
            ImageFormat.YUV_420_888,
            7
        )
        hasImages = false
//        imageReader.setOnImageAvailableListener(OnImageAvailableListener {
////            Log.d("image reader","Image available")
//            hasImages = true
//        }, Handler(Looper.getMainLooper()))
        loadMaterial()
        createMesh()
        createTexture()
        filamentTexture = Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGBA8)
            .build(engine)

        videoTexture = Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .width(bitmapSize.width.toInt())
            .height(bitmapSize.height.toInt())
            .levels(0xFF)
            .build(engine)

        filamentStream = Stream
            .Builder()
            .width(bitmapSize.width.toInt())
            .height(bitmapSize.height.toInt())
            .build(engine)

        filamentTexture.setExternalStream(engine, filamentStream)
        renderable = EntityManager.get().create()
        matInstance = material.createInstance()
        matInstance!!.setParameter("videoTexture", videoTexture, videoTextureSampler)

        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                indexBuffer,
                0,
                indexBuffer.indexCount
            )
            .material(0, matInstance!!)
            .culling(false)
            .build(engine, renderable)
        scene.addEntity(renderable)
        scene.skybox = Skybox.Builder().color(0.0f, 0f, 0f, 1.0f).build(engine)
        view.blendMode = View.BlendMode.OPAQUE
        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }

    fun setEnabled(enabled:Boolean) {
        isEnabled = enabled
    }

    override fun activate() {

    }

    override fun beforeRender(renderer: Renderer) {
        val time = lastVideoFrameTimeUsec;
        frameListener?.frameIsAboutToDraw(time)
        matInstance
    }

    override fun render(renderer: Renderer) {
        if(isEnabled)
            renderer.render(view)
    }


    private fun createTexture() {

    }

    override fun destroy(engine: Engine) {
        engine.destroyEntity(renderable)
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
        engine.destroyMaterial(material)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        val entityManager = EntityManager.get()
        entityManager.destroy(renderable)
        entityManager.destroy(camera.entity)
    }
    fun setVideoFrameListener(listener: VideoFrameListener){
        frameListener = listener
    }
    private fun loadMaterial() {
        generateMaterial().let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
            material.compile(
                Material.CompilerPriorityQueue.HIGH,
                Material.UserVariantFilterBit.ALL,
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
    fun toRGB(image: Image?) : ByteArray? {
        try {
            if (image != null) {
                val planes = image.planes
                if(planes == null || planes.size != 3)
                    return null

                val nv21: ByteArray
                val yBuffer: ByteBuffer = planes[0].buffer
                val uBuffer: ByteBuffer = planes[1].buffer
                val vBuffer: ByteBuffer = planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()
                nv21 = ByteArray(ySize + uSize + vSize)

                //U and V are swapped
                yBuffer[nv21, 0, ySize]
                vBuffer[nv21, ySize, vSize]
                uBuffer[nv21, ySize + vSize, uSize]

                //val r = IntArray(image.width*image.height)
                //GPUImageNativeLibrary.YUVtoRBGA(nv21,image.width,image.height,r)
                return nv21
            }
        } catch (e: java.lang.Exception) {
            Log.w("TTT", e.message!!)
        } finally {
        }
        return  null
    }


    fun getYUV2Mat(data: ByteArray?,image:Image): Mat {
        val mYuv = Mat(image.height + image.height / 2, image.width, CV_8UC1)
        mYuv.put(0, 0, data)
        val mRGB = Mat()
        cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB_NV21, 3)
        return mRGB
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

        /* (0,0)
         * |--------|
         * |        |
         * |        |
         * |________| (1,1)
         */
        var verticies = arrayOf(
            Vertex(1f, 0.0f, 0.0f, 1f, 1f, 1f),
            Vertex(0f, 0f, 0.0f, 1f, 0f, 1f),
            Vertex(0f, 1f, 0.0f, 1f, 0f, 0f),
            Vertex(1f, 1f, 0.0f, 1f, 1f, 0f)
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
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT4,
                0,
                vertexSize
            )
            .attribute(
                VertexBuffer.VertexAttribute.UV0,
                0,
                VertexBuffer.AttributeType.FLOAT2,
                4 * Float.SIZE_BYTES,
                vertexSize
            )
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

    override fun onResize(width: Int, height: Int) {
        view.viewport = Viewport(0, 0, width, height)
        camera.setProjection(
            Camera.Projection.ORTHO,
            0.0,
            width.toDouble(),
            height.toDouble(),
            0.0,
            -1.0,
            1.0
        )
    }

    fun processBitmap(bitmapRect: RectF) {
//        bitmapSize = SizeF(bitmap.width.toFloat(), bitmap.height.toFloat())
//        val textureW = videoTexture.getWidth(0)
//        val textureH = videoTexture.getHeight(0)
//        var processTexture = videoTexture

//        if (textureW != bitmap.width || textureH != bitmap.height) {
//            val latch = CountDownLatch(1)
//            handler.post(Runnable { // Code to run on the UI thread
//                val newTexture = Texture.Builder()
//                    .width(bitmap.width)
//                    .height(bitmap.height)
//                    .levels(0xff)
//                    .format(Texture.InternalFormat.RGBA8)
//                    .sampler(Texture.Sampler.SAMPLER_2D)
//                    .build(engine);
//                processTexture = newTexture
////                view.viewport = Viewport(dstRect.left.toInt(),dstRect.top.toInt(),dstRect.width().toInt(),dstRect.height().toInt())
//                latch.countDown()
//            })
//            try {
//                latch.await()
//            } catch (ex: Exception) {
//            }
//        }

        var surfaceRect = RectF(0f, 0f, surfaceView.width.toFloat(), surfaceView.height.toFloat())
        drawingRect = bitmapRect.getTransform(
            surfaceRect,
            bitmapStretch,
            BitmapAlignment.Center,
            BitmapAlignment.Center
        )

        var tx = MathHelpers.map(drawingRect.left, 0f, surfaceRect.width(), 0f, 1f)
        var ty = MathHelpers.map(drawingRect.top, 0f, surfaceRect.height(), 0f, 1f)
        tx = if (tx.isNaN() || !tx.isFinite()) 0f else tx
        ty = if (ty.isNaN() || !ty.isFinite()) 0f else ty
        val sx = drawingRect.width() / surfaceRect.width()
        val sy = drawingRect.height() / surfaceRect.height()
        val transformMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(transformMatrix, 0);
        android.opengl.Matrix.orthoM(transformMatrix, 0, 0f, 1f, 1f, 0f, -1f, 1f);
        android.opengl.Matrix.translateM(transformMatrix, 0, tx, ty, 0f);
        android.opengl.Matrix.scaleM(transformMatrix, 0, sx, sy, 1f);
//        val format = TextureUtils.format(bitmap)
//        val type = TextureUtils.type(bitmap)

//        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
//        bitmap.copyPixelsToBuffer(buffer)
//        buffer.flip()
//        val latch = CountDownLatch(1)
        handler.post {
//            val newBuffer = Texture.PixelBufferDescriptor(buffer, format, type)
//            processTexture.setImage(engine, 0, newBuffer)
//            videoTexture = processTexture
//            matInstance?.setParameter("videoTexture", videoTexture, videoTextureSampler)
            matInstance?.setParameter(
                "vertexTransform",
                MaterialInstance.FloatElement.MAT4,
                transformMatrix,
                0,
                1
            )
//            latch.countDown()
        }
    }

    private fun generateMaterial(): ByteBuffer {
        val mat = MaterialBuilder()
            .name("backed_color")
            .uniformParameter(MaterialBuilder.UniformType.MAT4, "vertexTransform")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT, "opacity")
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_2D,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "videoTexture"
            )

            .require(MaterialBuilder.VertexAttribute.UV0)
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .blending(MaterialBuilder.BlendingMode.TRANSPARENT)
            .vertexDomain(MaterialBuilder.VertexDomain.DEVICE)
            // .depthWrite(false)
            .variantFilter(
                Material.UserVariantFilterBit.SKINNING.or(Material.UserVariantFilterBit.SHADOW_RECEIVER)
                    .or(Material.UserVariantFilterBit.VSM)
            )
            .culling(MaterialBuilder.CullingMode.NONE)
            .materialVertex(
                """
                void materialVertex(inout MaterialVertexInputs material) {
                   material.clipSpaceTransform = materialParams.vertexTransform;
                }"""
            )
            .material(
                """
               void material(inout MaterialInputs material) {
                   prepareMaterial(material);
                   vec4 bc =texture(materialParams_videoTexture, getUV0());
                   material.baseColor =  bc;
               }
            """
            )
            .build()
        return mat.buffer
    }

    fun setTotalDuration(mVideoDuration: Long) {
        tex.totalDurationUsec = mVideoDuration
    }

    fun setTotalFrameCount(frameCount: Long) {
        tex.frameCount = frameCount
    }
}