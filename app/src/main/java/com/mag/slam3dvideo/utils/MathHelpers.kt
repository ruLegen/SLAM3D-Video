package com.mag.slam3dvideo.utils

import androidx.annotation.Size
import kotlin.math.sign
import kotlin.math.sqrt


data class Vector3F(var x:Float, var y:Float, var z:Float)
data class Vector4F(var x:Float, var y:Float, var z:Float,var w:Float)
data class MatrixDecomposition(var scale:Vector3F, var rotation:Vector4F, var translation:Vector3F)

object MathHelpers {
    fun map(x: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    /**    1  2  3  4
     *  _______________
     * M1| 0  4  8  12
     * M2| 1  5  9  13
     * M3| 2  6  10 14
     * M4| 3  7  11 15
     */
    fun decomposeGlMatrix(@Size(min = 16) matrix1: FloatArray): MatrixDecomposition {
        val matrix = FloatArray(16)
        android.opengl.Matrix.transposeM(matrix,0,matrix1,0)
        val translation = Vector3F(0f,0f,0f)
        val scale = Vector3F(0f,0f,0f)
        var rotation = Vector4F(0f,0f,0f, 0f)
        val M11 = matrix[0]
        val M12 = matrix[4]
        val M13 = matrix[8]
        val M14 = matrix[12]
        val M21 = matrix[1]
        val M22 = matrix[5]
        val M23 = matrix[9]
        val M24 = matrix[13]
        val M31 = matrix[2]
        val M32 = matrix[6]
        val M33 = matrix[10]
        val M34 = matrix[14]
        val M41 = matrix[3]
        val M42 = matrix[7]
        val M43 = matrix[11]
        val M44 = matrix[15]

        translation.x = M41
        translation.y = M42
        translation.z = M43

        val xs = (if (sign(M12 * M12 * M13 * M14) < 0) -1 else 1).toFloat()
        val ys = (if (sign(M21 * M22 * M23 * M24) < 0) -1 else 1).toFloat()
        val zs = (if (sign(M31 * M32 * M33 * M34) < 0) -1 else 1).toFloat()

        scale.x = xs * sqrt(M11 * M11 + M12 * M12 + M13 * M13)
        scale.y = ys * sqrt(M21 * M21 + M22 * M22 + M23 * M23)
        scale.z = zs * sqrt(M31 * M31 + M32 * M32 + M33 * M33)

        if (scale.x == 0.0f || scale.y == 0.0f || scale.z == 0.0f) {
            rotation = Vector4F(0f,0f,0f,1f)
            return MatrixDecomposition(scale,rotation,translation)
        }

        val m1 = arrayOf(
            M11 / scale.x,      // 0
            M21 / scale.y,      // 1
            M31 / scale.z,      // 2
            0f,                 // 3
            M12 / scale.x,      // 4
            M22 / scale.y,      // 5
            M32 / scale.z,      // 6
            0f,                 // 7
            M13 / scale.x,      // 8
            M23 / scale.y,      // 9
            M33 / scale.z,      // 10
            0f,                 // 11
            0f,                 // 12
            0f,                 // 13
            0f,                 // 14
            1f                  // 15
        )
        rotation = quternionFromRotationMatrix(m1)
        return MatrixDecomposition(scale,rotation,translation)
    }

    private fun quternionFromRotationMatrix(@Size(min=16) matrix: Array<Float>): Vector4F {
        val M11 = matrix[0]
        val M12 = matrix[4]
        val M13 = matrix[8]
        val M14 = matrix[12]
        val M21 = matrix[1]
        val M22 = matrix[5]
        val M23 = matrix[9]
        val M24 = matrix[13]
        val M31 = matrix[2]
        val M32 = matrix[6]
        val M33 = matrix[10]
        val M34 = matrix[14]
        val M41 = matrix[3]
        val M42 = matrix[7]
        val M43 = matrix[11]
        val M44 = matrix[15]

        val quaternion = Vector4F(0f,0f,0f,1f)
        var sqrt: Float
        val half: Float
        val scale: Float = M11 + M22 + M33

        if (scale > 0.0f) {
            sqrt = sqrt(scale + 1.0f)
            quaternion.w = sqrt * 0.5f
            sqrt = 0.5f / sqrt
            quaternion.x = (M23 - M32) * sqrt
            quaternion.y = (M31 - M13) * sqrt
            quaternion.z = (M12 - M21) * sqrt
            return quaternion
        }
        if (M11 >= M22 && M11 >= M33) {
            sqrt = sqrt(1.0f + M11 - M22 - M33)
            half = 0.5f / sqrt
            quaternion.x = 0.5f * sqrt
            quaternion.y = (M12 + M21) * half
            quaternion.z = (M13 + M31) * half
            quaternion.w = (M23 - M32) * half
            return quaternion
        }
        if (M22 > M33) {
            sqrt = sqrt(1.0f + M22 - M11 - M33)
            half = 0.5f / sqrt
            quaternion.x = (M21 + M12) * half
            quaternion.y = 0.5f * sqrt
            quaternion.z = (M32 + M23) * half
            quaternion.w = (M31 - M13) * half
            return quaternion
        }
        sqrt = sqrt(1.0f + M33 - M11 - M22)
        half = 0.5f / sqrt

        quaternion.x = (M31 + M13) * half
        quaternion.y = (M32 + M23) * half
        quaternion.z = 0.5f * sqrt
        quaternion.w = (M12 - M21) * half
        return quaternion
    }

}