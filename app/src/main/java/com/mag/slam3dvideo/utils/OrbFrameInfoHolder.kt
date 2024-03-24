package com.mag.slam3dvideo.utils

import com.mag.slam3dvideo.math.MatShared
import org.opencv.core.KeyPoint
import java.lang.RuntimeException

/**
 * This class responsible for holding camera data for each frame
 */
class OrbFrameInfoHolder(frameCount:Int) {
    init {
        if(frameCount <= 0)
            throw RuntimeException("framecount must be > 0")
    }
    var frameCount:Int = frameCount
        private set

    private var tcwList : Array<MatShared?> = Array(frameCount){null}
    private var keyPoints: Array<List<KeyPoint>?> = Array(frameCount){null}

    /**
     * @param frameNumber from 0 to (@frameCount-1)
     */
    fun setCameraPosAtFrame(frameNumber: Int, mat:MatShared?){
        if(frameNumber >= frameCount)
            return  //discard
        tcwList[frameNumber] = mat
    }
    /**
     * @param frameNumber from 0 to (@frameCount-1)
     */
    fun setKeypointsAtFrame(frameNumber:Int,points:List<KeyPoint>?){
        if(frameNumber >= frameCount)
            return  //discard
        keyPoints[frameNumber] = points
    }
    fun getCameraPosAtFrame(frameNumber: Int) : MatShared? {
        if(frameNumber >= frameCount||frameNumber<0)
            return null
        return tcwList[frameNumber]
    }
    /**
     * @param frameNumber from 0 to (@frameCount-1)
     */
    fun getKeypointsAtFrame(frameNumber:Int):List<KeyPoint>?{
        if(frameNumber >= frameCount || frameNumber <0)
            return  null
        return keyPoints[frameNumber]
    }


}