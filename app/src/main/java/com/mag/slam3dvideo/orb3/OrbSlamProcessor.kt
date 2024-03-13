package com.mag.slam3dvideo.orb3

import android.graphics.Bitmap

class OrbSlamProcessor(vocabFileName:String, configFileName:String) {
    open var ptr : Long = 0;
    init {
        System.loadLibrary("orbvideoslam")
    }
    init {
        ptr = initOrb(vocabFileName,configFileName);
    }
    fun processFrame(bitmap: Bitmap) {
        processBitmap(ptr,bitmap)
    }
    fun resaveVocabularyAsBinary(textVocabInputPath:String, binaryVocabOutputPath:String){
//        resaveVocabularyAsBinaryNative(textVocabInputPath,binaryVocabOutputPath)
    }
    private external fun processBitmap(ptr:Long,bitmap: Bitmap)
    private external fun initOrb(vocabFileName: String, configFileName: String): Long;
//    private external fun resaveVocabularyAsBinaryNative(textVocabInputPath:String, binartVocabOutputPath:String);
}