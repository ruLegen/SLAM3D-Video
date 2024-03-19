package com.mag.slam3dvideo.utils.bitmap

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.SizeF
import com.mag.slam3dvideo.utils.bitmap.BitmapUtils.calculateDisplayRect
import com.mag.slam3dvideo.utils.bitmap.BitmapUtils.computeScale

fun Bitmap.getTransform(
    dest: RectF,
    stretch: BitmapStretch,
    horizontal: BitmapAlignment = BitmapAlignment.Center,
    vertical: BitmapAlignment = BitmapAlignment.Center
): RectF {
    if(stretch == BitmapStretch.Fill)
        return dest
    val dstSize = SizeF(dest.width(), dest.height())
    val srcSize = SizeF(this.width.toFloat(), this.height.toFloat())
    val scale = computeScale(dstSize, srcSize, stretch)
    val dstRect = calculateDisplayRect(
        dest, scale * this.width, scale * this.height, horizontal, vertical
    );
    return dstRect
}

object BitmapUtils {
    fun computeScale(dstSize: SizeF, srcSize: SizeF, stretch: BitmapStretch): Float {
        var scale = 1f;
        if (stretch == BitmapStretch.Fill) {
            return 1f
        } else {
            scale = when (stretch) {
                BitmapStretch.None -> 1f
                BitmapStretch.AspectFit -> Math.min(
                    dstSize.width / srcSize.width,
                    dstSize.height / srcSize.height
                );
                BitmapStretch.AspectFill -> Math.max(
                    dstSize.width / srcSize.width,
                    dstSize.height / srcSize.height
                );
                else -> 1f
            }
            return scale
        }
    }

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
            BitmapAlignment.Center -> dest.height() / 2f - bmpHeight / 2f;
            BitmapAlignment.Start -> 0f
            BitmapAlignment.End -> dest.height() - bmpHeight;
            else -> 0f
        }
        x += dest.left;
        y += dest.top;
        return RectF(x, y, x + bmpWidth, y + bmpHeight)
    }
}