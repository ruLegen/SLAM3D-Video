package com.mag.slam3dvideo.scenes

import android.os.Handler
import android.os.Looper
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
import com.mag.slam3dvideo.math.MatShared
import com.mag.slam3dvideo.math.toGlMatrix
import com.mag.slam3dvideo.orb3.Plane
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectScene(private val surfaceView: SurfaceView) :OrbScene{
    private lateinit var view: View
    private var plane: Plane? = null
    private var w: Double = 0.0
    private var h: Double = 0.0
    private var fx: Double = 0.0
    private var fy: Double = 0.0
    private var cx: Double = 0.0
    private var cy: Double = 0.0

    private lateinit var indexBuffer: IndexBuffer
    private lateinit var vertexBuffer: VertexBuffer
    private var renderable: Int = 0
    private lateinit var matInstance: MaterialInstance
    private lateinit var material: Material
    private lateinit var camera: Camera
    private lateinit var scene: Scene
    private lateinit var engine: Engine
    private val handler:Handler = Handler(Looper.getMainLooper())
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
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLE_STRIP,vertexBuffer,indexBuffer)
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

    override fun render(renderer: Renderer) {
        if(plane == null)
            return
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
            .name("mesh_material")
            .require(MaterialBuilder.VertexAttribute.COLOR)
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .culling(MaterialBuilder.CullingMode.NONE)
            .material(
                """
               void material(inout MaterialInputs material) {
                   prepareMaterial(material);
                   material.baseColor =  getColor();
               }
            """
            )
            .build()
        return mat.buffer
    }
    private fun createMesh() {
        data class Vertex(
            val x: Float,
            val y: Float,
            val z: Float,
            val c: Int,
        )

        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            putInt(v.c)
            return this
        }
        val size = 0.05f;
        val vertexSize = 3 * Float.SIZE_BYTES + Int.SIZE_BYTES

        val l:Float = -size     // length
        val h:Float = size      // heigh
        val red = 0xffff0000.toInt()
        val green = 0xff00ff00.toInt()
        val blue = 0xff0000ff.toInt()

        val verticies = arrayOf(
           Vertex(l,l,h,red),  Vertex( h,l,h,red), Vertex( l,h,h,red),Vertex( h,h,h,red),  // FRONT
           Vertex(l,l,l,red),  Vertex( l,h,l,red), Vertex(h,l,l,red ),Vertex(h,h,l,red),  // BACK
           Vertex(l,l,h,green),  Vertex( l,h,h,green), Vertex(l,l,l,green ),Vertex(l,h,l,green ), // LEFT
           Vertex(h,l,l,blue),  Vertex( h,h,l,blue), Vertex(h,l,h,blue ),Vertex(h,h,h,blue ), // RIGHT
           Vertex(l,h,h,green),  Vertex( h,h,h,green), Vertex(l,h,l,green ),Vertex(h,h,l,green ), // TOP
           Vertex(l,l,h,blue),  Vertex( l,l,l,blue), Vertex(h,l,h,blue ),Vertex(h,l,l,blue ),  // BOTTOM
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
            .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,vertexSize)
            .attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.UBYTE4,3*Float.SIZE_BYTES,vertexSize)
            .normalized(VertexBuffer.VertexAttribute.COLOR)
            .build(engine)

        vertexBuffer.setBufferAt(engine, 0, vertexData)

        val indeces = arrayOf<Short>(
            0,1,2,3,
            4,5,6,7,
            8,9,10,11,
            12,13,14,15,
            16,17,18,19,
            20,21,22,23
        )
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

    fun getProjectionMatrix(w:Double, h:Double, fu:Double, fv:Double, u0:Double, v0:Double, zNear:Double, zFar:Double):DoubleArray{
        val res = DoubleArray(16)           // column major GL matrix
        val L = -(u0) * zNear / fu;
        val R = +(w-u0) * zNear / fu;
        val T = -(v0) * zNear / fv;
        val B = +(h-v0) * zNear / fv;

        res[0*4+0] = 2 * zNear / (R-L);
        res[1*4+1] = 2 * zNear / (T-B);

        res[2*4+0] = (R+L)/(L-R);
        res[2*4+1] = (T+B)/(B-T);
        res[2*4+2] = (zFar +zNear) / (zFar - zNear);
        res[2*4+3] = 1.0;

        res[3*4+2] =  (2*zFar*zNear)/(zNear - zFar);
        return res;
    }
    fun setCameraCallibration(w:Double,h:Double, fx:Double,fy:Double,cx:Double,cy:Double){
       this.w = w
       this.h = h
       this.fx = fx
       this.fy = fy
       this.cx = cx
       this.cy = cy
       val doubleArray = getProjectionMatrix(w,h,fx,fy,cx,cy,0.001,1000.0)
       camera.setCustomProjection(doubleArray, 0.001, 1000.0)
    }
    fun updateCameraMatrix(tcw: MatShared?) {
        if(tcw == null)
            return
        handler.post{
            //https://github.com/google/filament/blob/ba9cb2fe43f45c407e31fe197aa7e72d0e2810e5/filament/src/details/Camera.cpp#L201
            var res= FloatArray(16)
            android.opengl.Matrix.invertM(res,0,tcw.toGlMatrix(),0)
            camera.setModelMatrix(res)
        }
    }


    fun setPlane(p:Plane?){
        if(p == null)
            return
        plane = p
        val glMatrix = plane!!.getGLTpw()
        //android.opengl.Matrix.scaleM(glMatrix,0,1f,1f,-1f)
        handler.post{
            val tcm = engine.transformManager
            tcm.setTransform(tcm.getInstance(renderable), glMatrix)
        }
    }

    override fun onResize(width: Int, height: Int) {
        val near = 0.001
        val far = 1000.0
        val doubleArray = getProjectionMatrix(w,h,fx,fy,cx,cy,near,far)
        camera.setCustomProjection(doubleArray, 0.001, 1000.0)
        view.viewport = Viewport(0, 0, width, height)
    }

    override fun destroy(engine: Engine) {
    }


}