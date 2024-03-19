package com.mag.slam3dvideo.scenes

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.SizeF
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
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.filamat.MaterialBuilder
import com.mag.slam3dvideo.utils.MathHelpers
import com.mag.slam3dvideo.utils.TextureUtils
import com.mag.slam3dvideo.utils.bitmap.BitmapAlignment
import com.mag.slam3dvideo.utils.bitmap.BitmapStretch
import com.mag.slam3dvideo.utils.bitmap.getTransform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch

class VideoScene(private val surfaceView: SurfaceView) : OrbScene {
    private lateinit var view: View
    var drawingRect: RectF = RectF()
        private set
    var bitmapStretch: BitmapStretch = BitmapStretch.Fill
        private  set
    var bitmapSize:SizeF = SizeF(0f,0f)
        private set
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

    override fun init(e: Engine) {
        engine = e
        view = engine.createView()

        scene = engine.createScene()
        camera = engine.createCamera(engine.entityManager.create())

        loadMaterial()
        createMesh()
        createTexture()

        renderable = EntityManager.get().create()
        matInstance = material.createInstance()
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
        scene.skybox = Skybox.Builder().color(1.0f, 1f, 1f, 1.0f).build(engine)
        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }

    override fun activate() {

    }

    override fun render(renderer: Renderer) {
        renderer.render(view)

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
            Texture.PixelBufferDescriptor(
                ByteBuffer.allocate(128),
                Texture.Format.RGBA,
                Texture.Type.UBYTE
            );
        videoTexture.setImage(engine, 0, buffer);
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

    fun processBitmap(bitmap: Bitmap) {
        bitmapSize = SizeF(bitmap.width.toFloat(),bitmap.height.toFloat())

        val textureW = videoTexture.getWidth(0)
        val textureH = videoTexture.getHeight(0)
        var processTexture = videoTexture

        if (textureW != bitmap.width || textureH != bitmap.height) {
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
//                view.viewport = Viewport(dstRect.left.toInt(),dstRect.top.toInt(),dstRect.width().toInt(),dstRect.height().toInt())
                latch.countDown()
            })
            try {
                latch.await()
            } catch (ex: Exception) {
            }
        }

        var surfaceRect = RectF(0f, 0f, surfaceView.width.toFloat(), surfaceView.height.toFloat())
        drawingRect = bitmap.getTransform(
            surfaceRect,
            bitmapStretch,
            BitmapAlignment.Center,
            BitmapAlignment.Center
        )

        var tx = MathHelpers.map(drawingRect.left,0f,surfaceRect.width(),0f,1f)
        var ty = MathHelpers.map(drawingRect.top,0f,surfaceRect.height(),0f,1f)
        tx = if (tx.isNaN() || !tx.isFinite()) 0f else tx
        ty = if (ty.isNaN() || !ty.isFinite()) 0f else ty
        val sx = drawingRect.width() / surfaceRect.width()
        val sy = drawingRect.height() / surfaceRect.height()
        val transformMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(transformMatrix, 0);
        android.opengl.Matrix.orthoM(transformMatrix, 0, 0f, 1f, 1f,0f,-1f,1f);
        android.opengl.Matrix.translateM(transformMatrix, 0, tx, ty, 0f);
        android.opengl.Matrix.scaleM(transformMatrix, 0, sx, sy, 1f);

        val format = TextureUtils.format(bitmap)
        val type = TextureUtils.type(bitmap)

        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.flip()
        val latch = CountDownLatch(1)
        handler.post {
            val newBuffer = Texture.PixelBufferDescriptor(buffer, format, type)
            processTexture.setImage(engine, 0, newBuffer)
            videoTexture = processTexture
            matInstance?.setParameter("videoTexture", videoTexture, videoTextureSampler)
            matInstance?.setParameter(
                "vertexTransform",
                MaterialInstance.FloatElement.MAT4,
                transformMatrix,
                0,
                1
            )
            latch.countDown()
        }
        try {
           // latch.await()
        } catch (ex: Exception) {
        }
    }
    private fun generateMaterial(): ByteBuffer {
        val mat = MaterialBuilder()
            .name("backed_color")
            .uniformParameter(MaterialBuilder.UniformType.MAT4, "vertexTransform")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_2D,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "videoTexture"
            )

            .require(MaterialBuilder.VertexAttribute.UV0)
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
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
                   material.baseColor =  texture(materialParams_videoTexture, getUV0());
               }
            """
            )
            .build()
        return mat.buffer
    }
}