package com.mag.slam3dvideo.math

import org.opencv.core.Mat
import org.opencv.core.at

/**
 * This class behaves the same as original CV::Mat
 * Except it doesn't own the native ptr
 * Use with caution, underlying object may be destructed
 */
class MatShared(var ptr:Long,val isOwnPtr:Boolean = false) : Mat(ptr),AutoCloseable
{
    override fun finalize() {
        if(isOwnPtr && ptr != 0L){
            ptr = 0;
            super.finalize()
        }
    }

    override fun close() {
        if(isOwnPtr && ptr != 0L){
            ptr = 0
            super.finalize()
        }
    }

}

fun Mat.toGlMatrix():FloatArray{
    val glMat = FloatArray(16)
    // convert rowMajor opencv mat to column major opengl mat
    glMat[0] = this.at<Float>(0, 0).v
    glMat[1] = this.at<Float>(1, 0).v
    glMat[2] = this.at<Float>(2, 0).v
    glMat[3] = 0.0f

    glMat[4] = this.at<Float>(0, 1).v
    glMat[5] = this.at<Float>(1, 1).v
    glMat[6] = this.at<Float>(2, 1).v
    glMat[7] = 0.0f

    glMat[8] = this.at<Float>(0, 2).v
    glMat[9] = this.at<Float>(1, 2).v
    glMat[10] = this.at<Float>(2, 2).v
    glMat[11] = 0.0f

    glMat[12] = this.at<Float>(0, 3).v
    glMat[13] = this.at<Float>(1, 3).v
    glMat[14] = this.at<Float>(2, 3).v
    glMat[15] = 1.0f
    return  glMat
}