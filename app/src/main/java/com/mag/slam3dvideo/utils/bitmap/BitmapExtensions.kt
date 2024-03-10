package com.mag.slam3dvideo.utils.bitmap

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.core.math.MathUtils
import com.mag.slam3dvideo.utils.bitmap.BitmapUtils.calculateDisplayRect


fun Bitmap.getTransform(
    dest: RectF,
    stretch: BitmapStretch,
    horizontal: BitmapAlignment = BitmapAlignment.Center,
    vertical: BitmapAlignment = BitmapAlignment.Center
):RectF {

    val rectW = dest.width()
    val rectH = dest.height()
    if (stretch == BitmapStretch.Fill) {
        return RectF()
    } else {
        val scale = when (stretch) {
            BitmapStretch.None -> 1f
            BitmapStretch.AspectFit -> Math.min(rectW / this.width, rectH / this.height);
            BitmapStretch.AspectFill -> Math.max(rectW / this.width, rectH / this.height);
            else -> 1f
        }
        val dstRect = calculateDisplayRect(
            dest, scale * this.width, scale * this.height, horizontal, vertical
        );
        return  dstRect
    }
}

object BitmapUtils {
    fun calculateDisplayRect(
        dest: RectF,
        bmpWidth: Float,
        bmpHeight: Float,
        horizontal: BitmapAlignment,
        vertical: BitmapAlignment
    ): RectF {
        var x = when (horizontal) {
            BitmapAlignment.Center -> (dest.width() - bmpWidth) / 2f
            BitmapAlignment.Start -> 0f
            BitmapAlignment.End -> dest.width() - bmpWidth;
            else -> 0f
        }
        var y = when (vertical) {
            BitmapAlignment.Center -> dest.height()/2f - bmpHeight/ 2f;
            BitmapAlignment.Start -> 0f
            BitmapAlignment.End -> dest.height() - bmpHeight;
            else -> 0f
        }
        x += dest.left;
        y += dest.top;
        return RectF(x, y, x + bmpWidth, y + bmpHeight)
    }
}