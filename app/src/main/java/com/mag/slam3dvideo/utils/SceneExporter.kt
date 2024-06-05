package com.mag.slam3dvideo.utils

import android.util.Log
import androidx.arch.core.executor.TaskExecutor
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.mag.slam3dvideo.math.toGlMatrix
import com.mag.slam3dvideo.render.SceneContext
import com.mag.slam3dvideo.render.SceneObject
import com.mag.slam3dvideo.render.components.MeshRendererComponent
import com.mag.slam3dvideo.render.mesh.DynamicMesh
import com.mag.slam3dvideo.scenes.objectscene.CameraCallibration
import de.javagl.jgltf.model.AccessorDatas
import de.javagl.jgltf.model.AnimationModel
import de.javagl.jgltf.model.ElementType
import de.javagl.jgltf.model.GltfConstants
import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.NodeModel
import de.javagl.jgltf.model.creation.GltfModelBuilder
import de.javagl.jgltf.model.creation.MeshPrimitiveBuilder
import de.javagl.jgltf.model.impl.DefaultAccessorModel
import de.javagl.jgltf.model.impl.DefaultAnimationModel
import de.javagl.jgltf.model.impl.DefaultAnimationModel.DefaultChannel
import de.javagl.jgltf.model.impl.DefaultAnimationModel.DefaultSampler
import de.javagl.jgltf.model.impl.DefaultCameraModel
import de.javagl.jgltf.model.impl.DefaultMeshModel
import de.javagl.jgltf.model.impl.DefaultNodeModel
import de.javagl.jgltf.model.impl.DefaultSceneModel
import de.javagl.jgltf.model.io.GltfModelWriter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder

private object Exporter{
}
/**
 * The SceneExporter class is responsible for exporting a scene to a GLTF file format.
 * @property cameraLocationHolder The holder for camera location information.
 * @property cameraCalibration The camera calibration data.
 */
class SceneExporter (val cameraLocationHolder: OrbFrameInfoHolder,val cameraCalibration: CameraCallibration) {
    companion object{
        val executor= TaskRunner()
    }
    /**
     * Exports the scene to a GLTF file format.
     * @param sceneContext The scene to export
     */
    fun export(sceneContext: SceneContext) {
        try {
            val scene = DefaultSceneModel()
            val nodes: List<DefaultNodeModel> = getNodesFromObjects(sceneContext)
            nodes.forEach {
                scene.addNode(it)
            }

            val gltfModelBuilder = GltfModelBuilder.create()
            gltfModelBuilder.addSceneModel(scene)

            val cameraRoot = flattenNodes(nodes).find { it.name == "cameraRoot" }
            if(cameraRoot != null){
                val animationModel = createCameraAnimation(cameraLocationHolder,cameraRoot as DefaultNodeModel)
                gltfModelBuilder.addAnimationModel(animationModel)
            }
            executor.executeAsync({
                try {
                    val gltfModel: GltfModel? = gltfModelBuilder.build()
                    val outFile = File.createTempFile("gltf_", ".gltf")
                    Log.d("DEBUG", outFile.absolutePath);
                    val gltfModelWriter = GltfModelWriter()
                    gltfModelWriter.writeEmbedded(gltfModel,outFile)
                }catch (ex:Exception){
                    Log.e("EXPORT_ERROR",ex.message.toString())
                }
            })
        }catch (exx:Exception){
            Log.e("EXPORT_ERROR_out",exx.message.toString())
        }
    }

    private fun flattenNodes(nodes: List<NodeModel>): List<NodeModel> {
        val res = ArrayList<NodeModel>()
        fun processNode(outList:ArrayList<NodeModel>,node: NodeModel){
            outList.add(node)
            node.children.forEach {
                processNode(outList,it)
            }
        }
        nodes.forEach{ processNode(res,it) }
        return  res
    }

    /**
     * Create glTF animation for camera node
     *
     * @param cameraLocationHolder
     * @param node
     * @return
     */
    private fun createCameraAnimation(cameraLocationHolder: OrbFrameInfoHolder, node: DefaultNodeModel): AnimationModel {
        val allTransformMatrix = cameraLocationHolder.cameraTransformMatrixList
        val decomposedTransforms = allTransformMatrix
            .map {
                var glMatrix:FloatArray? = null
                if(it == null){
                    glMatrix = FloatArray(16)
                    android.opengl.Matrix.setIdentityM(glMatrix,0)
                }
                else{
                    glMatrix = it.toGlMatrix()
                }
                val glTwc = FloatArray(16)
                android.opengl.Matrix.invertM(glTwc, 0, glMatrix, 0)
                val decomposition:MatrixDecomposition = MathHelpers.decomposeGlMatrix(glTwc)
                return@map decomposition
            }

        val model = DefaultAnimationModel()
        val animationChannels = createAnimationChannels(node,decomposedTransforms,30.01f)
        animationChannels.forEach {
            model.addChannel(it)
        }
        return model
    }
    private fun createAnimationChannels(node: DefaultNodeModel, transforms: List<MatrixDecomposition>, fps: Float): Array<AnimationModel.Channel> {
        val timeCount = transforms.size
        val timeBuffer = ByteBuffer.allocate(timeCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        val frameTime = 1/fps
        (0 until    timeCount).forEach{
            timeBuffer.putFloat((it+1)*frameTime)
        }
        timeBuffer.flip()
        val inputAccessor = DefaultAccessorModel(GltfConstants.GL_FLOAT, timeCount, ElementType.SCALAR)
        inputAccessor.accessorData = AccessorDatas.create(inputAccessor, timeBuffer)
        val translationChannel = createPositionChannel(node,transforms,inputAccessor)
        val rotationChannel = createRotationChannel(node,transforms,inputAccessor)
        val scaleChannel = createScaleChannel(node,transforms,inputAccessor)
        return arrayOf(translationChannel,rotationChannel,scaleChannel)
    }


    private fun createPositionChannel(node: NodeModel,transforms: List<MatrixDecomposition>,inputAccessor: DefaultAccessorModel): AnimationModel.Channel {
        val outputBuffer = ByteBuffer.allocate(transforms.size * Float.SIZE_BYTES * 3)
            .order(ByteOrder.nativeOrder())
        (transforms.indices).forEach {
            val pos = transforms[it].translation
            outputBuffer.putFloat(pos.x)
            outputBuffer.putFloat(pos.y)
            outputBuffer.putFloat(pos.z)
        }
        outputBuffer.flip()
        return createOutputChannel(GltfConstants.GL_FLOAT,outputBuffer,transforms.size,ElementType.VEC3,inputAccessor,"translation",node)
    }
    private fun createRotationChannel(
        node: DefaultNodeModel,
        transforms: List<MatrixDecomposition>,
        inputAccessor: DefaultAccessorModel
    ): AnimationModel.Channel {
        val outputBuffer = ByteBuffer.allocate(transforms.size * Float.SIZE_BYTES * 4)
            .order(ByteOrder.nativeOrder())
        (transforms.indices).forEach {
            val rotation = transforms[it].rotation
            outputBuffer.putFloat(rotation.x)
            outputBuffer.putFloat(rotation.y)
            outputBuffer.putFloat(rotation.z)
            outputBuffer.putFloat(rotation.w)
        }
        outputBuffer.flip()
        return createOutputChannel(GltfConstants.GL_FLOAT,outputBuffer,transforms.size,ElementType.VEC4,inputAccessor,"rotation",node)
    }

    private fun createScaleChannel(
        node: DefaultNodeModel,
        transforms: List<MatrixDecomposition>,
        inputAccessor: DefaultAccessorModel
    ): AnimationModel.Channel {
        val outputBuffer = ByteBuffer.allocate(transforms.size * Float.SIZE_BYTES * 3)
            .order(ByteOrder.nativeOrder())
        (transforms.indices).forEach {
            val rotation = transforms[it].scale
            outputBuffer.putFloat(rotation.x)
            outputBuffer.putFloat(rotation.y)
            outputBuffer.putFloat(rotation.z)
        }
        outputBuffer.flip()
        return createOutputChannel(GltfConstants.GL_FLOAT,outputBuffer,transforms.size,ElementType.VEC3,inputAccessor,"scale",node)
    }
    private fun createOutputChannel(componentType:Int,
                                    buffer:ByteBuffer,
                                    elementCount:Int,
                                    elementType:ElementType,
                                    inputAccessor:DefaultAccessorModel,
                                    channelName:String,
                                    node:NodeModel): DefaultChannel {
        val outputAccessor = DefaultAccessorModel(componentType, elementCount, elementType)
        outputAccessor.accessorData = AccessorDatas.create(outputAccessor, buffer)
        val sampler =DefaultSampler(inputAccessor, AnimationModel.Interpolation.STEP, outputAccessor)
        return DefaultChannel(sampler, node, channelName)
    }
    private fun getNodesFromObjects(context: SceneContext): List<DefaultNodeModel> {
        val sceneObjects = context.sceneObjectContainer.objects
        val nodes = sceneObjects.map { obj -> getNode(context, null, obj) }
        return nodes
    }

    /**
     * Generates glTF node
     *
     * @param context
     * @param parentNode
     * @param obj
     * @return
     */
    private fun getNode(context: SceneContext,parentNode: DefaultNodeModel?,obj: SceneObject): DefaultNodeModel {
        val node = DefaultNodeModel()
        node.setParent(parentNode)
        node.name = obj.name
        node.matrix = obj.transformComponent.getTransform(context)

        // what if object has multiple mesh renderers?
        val meshRenderer = obj.getComponent<MeshRendererComponent>()
        val objMesh = meshRenderer?.getMesh()
        if (objMesh != null) {
            val dynamicMesh = objMesh as? DynamicMesh

            val vertexCount  = dynamicMesh?.currentVertexSize ?: objMesh.vertexCount
            val indexCount = dynamicMesh?.currentIndicesSize?:objMesh.indicesCount

            val vertexData = objMesh.getVertexDataBuffer()
            val indexBuffer = objMesh.getIndexDataBuffer()

            val attributes = objMesh.attributes
            val gltfMesh = DefaultMeshModel()
            val meshPrimitiveBuilder = MeshPrimitiveBuilder.create()
            when (meshRenderer.primitiveType) {
                RenderableManager.PrimitiveType.POINTS -> meshPrimitiveBuilder.setPoints()
                RenderableManager.PrimitiveType.LINES -> meshPrimitiveBuilder.setLines()
                RenderableManager.PrimitiveType.TRIANGLES -> meshPrimitiveBuilder.setTriangles()
                else -> {}
            }
            val indicesAccessor = DefaultAccessorModel(GltfConstants.GL_UNSIGNED_SHORT, indexCount, ElementType.SCALAR)
            indicesAccessor.accessorData = AccessorDatas.create(indicesAccessor, indexBuffer as ByteBuffer)
            meshPrimitiveBuilder.setIndices(indicesAccessor)
            attributes.forEach {
                val name = it.attribute.name
                val componentType = it.type.toGlTFComponentType()
                val elementType = it.type.toGlTFElementType()
                val accessorModel = DefaultAccessorModel(componentType, vertexCount, elementType)
                accessorModel.byteStride = it.stride
                accessorModel.byteOffset = it.offset
                accessorModel.isNormalized = name == "COLOR"
                accessorModel.accessorData = AccessorDatas.create(accessorModel, vertexData as ByteBuffer)
                meshPrimitiveBuilder.addAttribute(name, accessorModel)
            }
            val primitives = meshPrimitiveBuilder.build()
            gltfMesh.addMeshPrimitiveModel(primitives)
            node.addMeshModel(gltfMesh)
        }

//        val cameraComponent = null
        if(obj.name == "cameraObj"){
            node.cameraModel = DefaultCameraModel().apply {
                val cameraModel = CameraUtils.getGltfCameraParameters(cameraCalibration.x,cameraCalibration.h,cameraCalibration.fx,cameraCalibration.fy,cameraCalibration.cx,cameraCalibration.cy,0.001,1000.0)
                cameraPerspectiveModel= cameraModel
            }
        }

        val children = obj.children.map {
            getNode(context, node, it)
        }
        children.forEach {
            node.addChild(it)
        }
        return node
    }
}

private fun VertexBuffer.AttributeType.toGlTFElementType(): ElementType {
    return when (this) {
        VertexBuffer.AttributeType.BYTE,
        VertexBuffer.AttributeType.UBYTE,
        VertexBuffer.AttributeType.SHORT,
        VertexBuffer.AttributeType.USHORT,
        VertexBuffer.AttributeType.INT,
        VertexBuffer.AttributeType.UINT,
        VertexBuffer.AttributeType.FLOAT,
        VertexBuffer.AttributeType.HALF -> ElementType.SCALAR

        VertexBuffer.AttributeType.BYTE2,
        VertexBuffer.AttributeType.SHORT2,
        VertexBuffer.AttributeType.UBYTE2,
        VertexBuffer.AttributeType.USHORT2,
        VertexBuffer.AttributeType.FLOAT2,
        VertexBuffer.AttributeType.HALF2 -> ElementType.VEC2

        VertexBuffer.AttributeType.BYTE3,
        VertexBuffer.AttributeType.UBYTE3,
        VertexBuffer.AttributeType.SHORT3,
        VertexBuffer.AttributeType.USHORT3,
        VertexBuffer.AttributeType.FLOAT3,
        VertexBuffer.AttributeType.HALF3 -> ElementType.VEC3

        VertexBuffer.AttributeType.BYTE4,
        VertexBuffer.AttributeType.UBYTE4,
        VertexBuffer.AttributeType.SHORT4,
        VertexBuffer.AttributeType.USHORT4,
        VertexBuffer.AttributeType.FLOAT4,
        VertexBuffer.AttributeType.HALF4 -> ElementType.VEC4
    }
}

private fun VertexBuffer.AttributeType.toGlTFComponentType(): Int {
    return when (this) {
        VertexBuffer.AttributeType.BYTE,
        VertexBuffer.AttributeType.BYTE2,
        VertexBuffer.AttributeType.BYTE3,
        VertexBuffer.AttributeType.BYTE4 -> GltfConstants.GL_BYTE

        VertexBuffer.AttributeType.UBYTE,
        VertexBuffer.AttributeType.UBYTE2,
        VertexBuffer.AttributeType.UBYTE3,
        VertexBuffer.AttributeType.UBYTE4 -> GltfConstants.GL_UNSIGNED_BYTE

        VertexBuffer.AttributeType.SHORT,
        VertexBuffer.AttributeType.SHORT2,
        VertexBuffer.AttributeType.SHORT3,
        VertexBuffer.AttributeType.SHORT4 -> GltfConstants.GL_SHORT

        VertexBuffer.AttributeType.USHORT,
        VertexBuffer.AttributeType.USHORT2,
        VertexBuffer.AttributeType.USHORT3,
        VertexBuffer.AttributeType.USHORT4 -> GltfConstants.GL_UNSIGNED_SHORT

        VertexBuffer.AttributeType.INT -> GltfConstants.GL_INT
        VertexBuffer.AttributeType.UINT -> GltfConstants.GL_UNSIGNED_INT
        VertexBuffer.AttributeType.FLOAT,
        VertexBuffer.AttributeType.FLOAT2,
        VertexBuffer.AttributeType.FLOAT3,
        VertexBuffer.AttributeType.FLOAT4 -> GltfConstants.GL_FLOAT

        VertexBuffer.AttributeType.HALF -> TODO()
        VertexBuffer.AttributeType.HALF2 -> TODO()
        VertexBuffer.AttributeType.HALF3 -> TODO()
        VertexBuffer.AttributeType.HALF4 -> TODO()
    }
}