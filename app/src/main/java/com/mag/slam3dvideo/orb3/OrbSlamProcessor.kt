package com.mag.slam3dvideo.orb3

import android.graphics.Bitmap
import com.charleskorn.kaml.Yaml
import com.mag.slam3dvideo.math.MatShared
import com.mag.slam3dvideo.utils.FileUtils
import com.mag.slam3dvideo.utils.OrbSlamSettings
import kotlinx.serialization.decodeFromString
import org.opencv.core.KeyPoint
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException

data class MapPoint(val x:Float,val y:Float,val z:Float,val isReferenced:Boolean)
class OrbSlamProcessor() {
    var ptr: Long = 0;
    init {
        System.loadLibrary("orbvideoslam")

    }
    constructor(vocabFileName: String, configFileName: String) : this() {
        ptr = nInitOrb(vocabFileName, configFileName);
    }
    constructor(vocabFileName: String, settings:OrbSlamSettings):this(){
        val serialized = Yaml.default.encodeToString(OrbSlamSettings.serializer(),settings)
        val opencv  = "%YAML:1.0\n".plus(serialized)
        val file = File.createTempFile("setting_",".yaml")
        file.writeText(opencv)
        val filePath = file.absolutePath
        ptr = nInitOrb(vocabFileName,filePath)
        file.delete()
    }
    fun processFrame(bitmap: Bitmap): MatShared? {
        val tcwMatrix = nProcessBitmap(ptr, bitmap)
        if(tcwMatrix == 0L)
            return  null
        return MatShared(tcwMatrix,true)
    }
    fun getTrackingState():TrackingState{
        val nativeTrackingState :Int = nGetTrackingState(ptr)
        return TrackingState.fromInt(nativeTrackingState);
    }
    private fun getMapPoints(): List<MatShared> {
        val ptrs = nGetMapPointsPositions(ptr)
        if (ptrs == null || ptrs.isEmpty())
            return ArrayList(0)
        return ptrs.map { MatShared(it, true) }
    }
    fun getCurrentFrameKeyPoints(): List<KeyPoint>{
        val keyPointMemberCount = 2 //keep in sync with cv::KeyPoint and orb3_jni.cpp
        val floatArray : FloatArray? = nGetCurrentFrameKeyPoints(ptr);
        if(floatArray == null || floatArray.isEmpty())
            return ArrayList<KeyPoint>(0);
        if(floatArray.size % keyPointMemberCount != 0)
            throw RuntimeException("the size of returned array must be size%${keyPointMemberCount} == 0; Now size=${floatArray.size}  size%${keyPointMemberCount} = ${floatArray.size%keyPointMemberCount}")

        val res = ArrayList<KeyPoint>(floatArray.size/keyPointMemberCount)
        for (i in 0 ..< floatArray.size step keyPointMemberCount){
            val x = floatArray[i+0]
            val y = floatArray[i+1]
            res.add(KeyPoint(x,y,1f))
        }
        return res;
    }

    fun getCurrentMapPoints():List<MapPoint>{
        val mapPointMemberCount = 4 //keep in sync with value in orb3_jni.cpp::nGetCurrentMapPoints
        val floatArray : FloatArray? = nGetCurrentMapPoints(ptr);
        if(floatArray == null || floatArray.isEmpty())
            return ArrayList(0);
        if(floatArray.size % mapPointMemberCount != 0)
            throw RuntimeException("the size of returned array must be size%${mapPointMemberCount} == 0; Now size=${floatArray.size}  size%${mapPointMemberCount} = ${floatArray.size%mapPointMemberCount}")

        val res = ArrayList<MapPoint>(floatArray.size/mapPointMemberCount)
        for (i in 0 ..< floatArray.size step mapPointMemberCount){
            val x = floatArray[i+0]
            val y = floatArray[i+1]
            val z = floatArray[i+2]
            val isReference = floatArray[i+3]
            res.add(MapPoint(x,y,z,isReference>0))
        }
        return res;
    }


    fun detectPlane() : Plane?{
        val ptr:Long = nDetectPlane(ptr);
        if(ptr == 0L)
            return null;
        return Plane(ptr)
    }


    private external fun nGetMapPointsPositions(ptr: Long): LongArray?
    private external fun nGetCurrentFrameKeyPoints(ptr: Long): FloatArray?
    private external fun nGetCurrentMapPoints(ptr: Long): FloatArray?

    private external fun nGetTrackingState(ptr: Long): Int

    private external fun nProcessBitmap(ptr: Long, bitmap: Bitmap): Long
    private external fun nInitOrb(vocabFileName: String, configFileName: String): Long
    private external fun nDetectPlane(ptr: Long): Long


    fun resaveVocabularyAsBinary(textVocabInputPath: String, binaryVocabOutputPath: String) {
//        resaveVocabularyAsBinaryNative(textVocabInputPath,binaryVocabOutputPath)
    }
//    private external fun resaveVocabularyAsBinaryNative(textVocabInputPath:String, binartVocabOutputPath:String);
}
