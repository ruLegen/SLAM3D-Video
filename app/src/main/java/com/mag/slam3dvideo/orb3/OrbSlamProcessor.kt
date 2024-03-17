package com.mag.slam3dvideo.orb3

import android.graphics.Bitmap
import com.mag.slam3dvideo.math.MatShared
import org.opencv.core.KeyPoint
import java.lang.RuntimeException

enum class TrackingState(val state: Int) {
    SYSTEM_NOT_READY(-1),
    NO_IMAGES_YET(0),
    NOT_INITIALIZED(1),
    OK(2),
    RECENTLY_LOST(3),
    LOST(4),
    OK_KLT(5),
    UNKNOWN(Int.MAX_VALUE);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.state == value } ?: UNKNOWN;
    }
}

class OrbSlamProcessor(vocabFileName: String, configFileName: String) {
    open var ptr: Long = 0;

    init {
        System.loadLibrary("orbvideoslam")
    }

    init {
        ptr = nInitOrb(vocabFileName, configFileName);
    }

    fun processFrame(bitmap: Bitmap): MatShared? {
        val tcwMatrix = nProcessBitmap(ptr, bitmap)
        if(tcwMatrix == 0L)
            return  null
        return MatShared(tcwMatrix,true)
    }
    fun getTrackingState():TrackingState{
        var nativeTrackingState :Int = nGetTrackingState(ptr)
        return TrackingState.fromInt(nativeTrackingState);
    }


    fun getMapPoints(): List<MatShared> {
        val ptrs = nGetMapPointsPositions(ptr)
        if (ptrs == null || ptrs.size == 0)
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
    private external fun nGetMapPointsPositions(ptr: Long): LongArray?
    private external fun nGetCurrentFrameKeyPoints(ptr: Long): FloatArray?
    private external fun nGetTrackingState(ptr: Long): Int

    private external fun nProcessBitmap(ptr: Long, bitmap: Bitmap): Long
    private external fun nInitOrb(vocabFileName: String, configFileName: String): Long


    fun resaveVocabularyAsBinary(textVocabInputPath: String, binaryVocabOutputPath: String) {
//        resaveVocabularyAsBinaryNative(textVocabInputPath,binaryVocabOutputPath)
    }
//    private external fun resaveVocabularyAsBinaryNative(textVocabInputPath:String, binartVocabOutputPath:String);
}
