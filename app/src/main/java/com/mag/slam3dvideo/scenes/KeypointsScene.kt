package com.mag.slam3dvideo.scenes

import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.SizeF
import android.view.SurfaceView
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.filamat.MaterialBuilder
import com.mag.slam3dvideo.utils.bitmap.BitmapStretch
import com.mag.slam3dvideo.utils.bitmap.BitmapUtils
import org.opencv.core.KeyPoint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch

class KeypointsScene(private val surfaceView: SurfaceView) :OrbScene{
    private lateinit var view: View
    var bitmapSize:SizeF = SizeF(1f,1f)
    var drawingRect:RectF = RectF()

    private var bitmapStretch:BitmapStretch = BitmapStretch.AspectFit

    private lateinit var indexBuffer: IndexBuffer
    private lateinit var vertexBuffer: VertexBuffer
    private var renderable: Int = 0
    private lateinit var matInstance: MaterialInstance
    private lateinit var material: Material
    private lateinit var camera: Camera
    private lateinit var scene: Scene
    private lateinit var engine: Engine;
    private val handler = Handler(Looper.getMainLooper())

    override fun init(e: Engine) {
        engine = e
        view = engine.createView()

        scene = engine.createScene()
        camera = engine.createCamera(engine.entityManager.create())

        loadMaterial()
        createMesh()
        matInstance = material.createInstance()
        renderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            .geometry(0,RenderableManager.PrimitiveType.POINTS,vertexBuffer,indexBuffer)
            .material(0, matInstance!!)
            .culling(false)
            .build(engine, renderable)
        scene.addEntity(renderable)
        scene.skybox = null//Skybox.Builder().color(1.0f, 1f, 1f, 1.0f).build(engine)
        view.isPostProcessingEnabled = false
        view.camera = camera
        view.scene = scene
    }

    override fun activate() {

    }

    override fun beforeRender(renderer: Renderer) {

    }

    override fun render(renderer: Renderer) {
        renderer.render(view)
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
    private fun generateMaterial(): ByteBuffer {
        val mat = MaterialBuilder()
            .name("points_material")
            .uniformParameter(MaterialBuilder.UniformType.MAT4, "vertexTransform")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT, "size")
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .vertexDomain(MaterialBuilder.VertexDomain.DEVICE)
            // .depthWrite(false)
            .variantFilter(
                Material.UserVariantFilterBit.SKINNING
                    .or(Material.UserVariantFilterBit.SHADOW_RECEIVER)
                    .or(Material.UserVariantFilterBit.VSM)
            )
            .culling(MaterialBuilder.CullingMode.NONE)
            .materialVertex(
                """
                void materialVertex(inout MaterialVertexInputs material) {
                   material.clipSpaceTransform = materialParams.vertexTransform;
                   gl_PointSize = materialParams.size;
                }"""
            )
            .material(
                """
               void material(inout MaterialInputs material) {
                   prepareMaterial(material);
                   material.baseColor =  vec4(1.0,0.0,0.0,1.0);
               }
            """
            )
            .build()
        return mat.buffer
    }
    fun updateTransform(){
        val transformMatrix = FloatArray(16)
        var tx =drawingRect.left
        var ty =drawingRect.top
        val surfaceW = if(surfaceView.width == 0) 1f else surfaceView.width.toFloat()
        val surfaceH = if(surfaceView.height == 0) 1f else surfaceView.height.toFloat()
        val s = BitmapUtils.computeScale(SizeF(surfaceW,surfaceH),bitmapSize,bitmapStretch)
//        val s = Math.max(surfaceW/bitmapSize.width,surfaceH/bitmapSize.height)

        android.opengl.Matrix.setIdentityM(transformMatrix,0)
        android.opengl.Matrix.orthoM(transformMatrix,0,0f,surfaceW, surfaceH,0f,-1f,1f)
        android.opengl.Matrix.translateM(transformMatrix,0,tx,ty,0f)
        android.opengl.Matrix.scaleM(transformMatrix,0,s,s,1f)
        matInstance?.setParameter(
            "vertexTransform",
            MaterialInstance.FloatElement.MAT4,
            transformMatrix,
            0,
            1
        )
        matInstance?.setParameter("size", 10f)
    }
        data class Vertex(
            val x: Float,
            val y: Float,
        )

        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            return this
        }
        val vertexSize = 2 * Float.SIZE_BYTES
    private fun createMesh() {
        val verticies = arrayOf(
            Vertex(0f, 0f),
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
            .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT2,0,vertexSize)
            .build(engine)

        vertexBuffer.setBufferAt(engine, 0, vertexData)

        val indeces = (0..<verticies.size).map { it.toShort() }
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
        view.viewport=Viewport(0,0,width,height)
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

    override fun destroy(engine: Engine) {
    }

    fun setBitmapInfo(stretchType: BitmapStretch, size: SizeF) {
        bitmapSize = size
        bitmapStretch = stretchType
    }

    fun updateKeypoints(k: List<KeyPoint>?) {
        if(k == null)
            return
        var keys = k
        if(keys.isEmpty())
        {
            keys = ArrayList<KeyPoint>(1).also{it.add(KeyPoint())}
        }
        val verticies = keys.map { k -> Vertex(k.pt.x.toFloat(),k.pt.y.toFloat()) }

        val vertexData = ByteBuffer.allocate(keys.size * vertexSize)
            .order(ByteOrder.nativeOrder())
            .also {
                verticies.forEach { v -> it.put(v) }
            }
            .flip()

        val latch = CountDownLatch(1)
        handler.post{
            engine.destroyVertexBuffer(vertexBuffer)
            engine.destroyIndexBuffer(indexBuffer)
            vertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(verticies.size)
                .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT2,0,vertexSize)
                .build(engine)

            vertexBuffer.setBufferAt(engine, 0, vertexData)

            val indeces = (0..<verticies.size).map { it.toShort() }
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
            scene.removeEntity(renderable)
            engine.destroyEntity(renderable)
            renderable = EntityManager.get().create()
            RenderableManager.Builder(1)
                .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
                .geometry(0,RenderableManager.PrimitiveType.POINTS,vertexBuffer,indexBuffer)
                .material(0, matInstance!!)
                .culling(false)
                .build(engine, renderable)
            scene.addEntity(renderable)
            latch.countDown()
        }
       // latch.await()
        return

    }
}