package com.mag.slam3dvideo.utils

import de.javagl.jgltf.impl.v2.CameraPerspective
import de.javagl.jgltf.model.CameraPerspectiveModel
import de.javagl.jgltf.model.impl.DefaultCameraPerspectiveModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan

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
    fun getGltfCameraParameters(w:Double, h:Double, fu:Double, fv:Double, u0:Double, v0:Double, zNear:Double, zFar:Double) : CameraPerspectiveModel {
        val L = -(u0) * zNear / fu;
        val R = +(w-u0) * zNear / fu;
        val T = -(v0) * zNear / fv;
        val B = +(h-v0) * zNear / fv;

        //https://stackoverflow.com/a/46195462
        val mat00 = 2 * zNear / (R-L)
        val mat11 = 2 * zNear / (T-B)
        val fovY = 2.0 * atan(1.0/mat11)
        val aspect = mat11 / mat00


        val cameraParams =  DefaultCameraPerspectiveModel()
        cameraParams.aspectRatio = abs(aspect).toFloat()
        cameraParams.yfov = abs(fovY).toFloat()
        cameraParams.zfar = zFar.toFloat()
        cameraParams.znear = zNear.toFloat()
        return  cameraParams
    }
}