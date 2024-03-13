package com.mag.slam3dvideo.orb3

import android.graphics.Bitmap
enum class TrackingState(val state:Int){
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
class OrbSlamProcessor(vocabFileName:String, configFileName:String) {
    open var ptr : Long = 0;
    init {
        System.loadLibrary("orbvideoslam")
    }
    init {
        ptr = initOrb(vocabFileName,configFileName);
    }
    fun processFrame(bitmap: Bitmap):TrackingState {
        val intState = processBitmap(ptr,bitmap)
        return TrackingState.fromInt(intState)
    }
    fun resaveVocabularyAsBinary(textVocabInputPath:String, binaryVocabOutputPath:String){
//        resaveVocabularyAsBinaryNative(textVocabInputPath,binaryVocabOutputPath)
    }
    private external fun processBitmap(ptr:Long,bitmap: Bitmap):Int
    private external fun initOrb(vocabFileName: String, configFileName: String): Long;
//    private external fun resaveVocabularyAsBinaryNative(textVocabInputPath:String, binartVocabOutputPath:String);
}