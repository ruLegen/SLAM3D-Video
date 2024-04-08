package com.mag.slam3dvideo.utils

object CameraUtils {
    fun getProjectionMatrix(w:Double, h:Double, fu:Double, fv:Double, u0:Double, v0:Double, zNear:Double, zFar:Double):DoubleArray{
        val res = DoubleArray(16)           // column major GL matrix
        val L = -(u0) * zNear / fu;
        val R = +(w-u0) * zNear / fu;
        val T = -(v0) * zNear / fv;
        val B = +(h-v0) * zNear / fv;

        res[0*4+0] = 2 * zNear / (R-L);
        res[1*4+1] = 2 * zNear / (T-B);

        res[2*4+0] = (R+L)/(L-R);
        res[2*4+1] = (T+B)/(B-T);
        res[2*4+2] = (zFar +zNear) / (zFar - zNear);
        res[2*4+3] = 1.0;

        res[3*4+2] =  (2*zFar*zNear)/(zNear - zFar);
        return res;
    }
}