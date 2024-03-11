package com.mag.slam3dvideo.orb3

class ORB3(val vocabFileName:String, val configFileName:String) {
    open var ptr : Long = 0;
    init {
        System.loadLibrary("orbvideoslam")
    }
    init {
        ptr = initOrb(vocabFileName,configFileName);
    }

    fun resaveVocabularyAsBinary(textVocabInputPath:String, binaryVocabOutputPath:String){
        resaveVocabularyAsBinaryNative(textVocabInputPath,binaryVocabOutputPath)
    }
    private external fun resaveVocabularyAsBinaryNative(textVocabInputPath:String, binartVocabOutputPath:String);
    private external fun initOrb(vocabFileName: String, configFileName: String): Long;
}