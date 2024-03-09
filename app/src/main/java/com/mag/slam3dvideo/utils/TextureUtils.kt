package com.mag.slam3dvideo.utils

import android.graphics.Bitmap
import com.google.android.filament.Texture
import java.lang.IllegalArgumentException

object TextureUtils {
   fun format(bitmap: Bitmap) = when (bitmap.config.name) {
        "ALPHA_8"   -> Texture.Format.ALPHA
        "RGB_565"   -> Texture.Format.RGB
        "ARGB_8888" -> Texture.Format.RGBA
        "RGBA_F16"  -> Texture.Format.RGBA
        else -> throw IllegalArgumentException("Unknown bitmap configuration")
    }
    fun type(bitmap: Bitmap) = when (bitmap.config.name) {
        "ALPHA_8"   -> Texture.Type.USHORT
        "RGB_565"   -> Texture.Type.USHORT_565
        "ARGB_8888" -> Texture.Type.UBYTE
        "RGBA_F16"  -> Texture.Type.HALF
        else -> throw IllegalArgumentException("Unsupported bitmap configuration")
    }
}