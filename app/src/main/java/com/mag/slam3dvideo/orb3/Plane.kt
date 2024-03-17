package com.mag.slam3dvideo.orb3

import android.opengl.Matrix
import java.lang.RuntimeException

class Plane(private var ptr: Long) {
    protected fun finalize() {
        nDestroyPlane(ptr)
        ptr = 0
    }

    fun getGLTpw(): FloatArray {
        if (ptr == 0L)
            throw RuntimeException("Plane native object is invalid")
        var floatArray: FloatArray? = nGetGLTpw(ptr)
        if(floatArray == null)
        {
            val res = FloatArray(16)
            Matrix.setIdentityM(res,0)
            return  res
        }
        if(floatArray.size != 16)
            throw  RuntimeException("the size of returned array is invalid. Returned ${floatArray.size} instead of 16")

        return  floatArray
    }

    private external fun nDestroyPlane(ptr: Long)
    private external fun nGetGLTpw(ptr: Long):FloatArray?
}