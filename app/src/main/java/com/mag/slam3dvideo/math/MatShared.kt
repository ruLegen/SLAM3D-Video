package com.mag.slam3dvideo.math

import org.opencv.core.Mat

/**
 * This class behaves the same as original CV::Mat
 * Except it doesn't own the native ptr
 * Use with caution, underlying object may be destructed
 */
class MatShared(ptr:Long,val isOwnPtr:Boolean = false) : Mat(ptr)
{
    override fun finalize() {
        if(isOwnPtr)
            super.finalize()
    }
}