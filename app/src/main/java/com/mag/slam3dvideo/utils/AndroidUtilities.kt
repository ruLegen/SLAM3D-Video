package com.mag.slam3dvideo.utils

import android.graphics.Rect

import android.graphics.RectF
import java.util.Locale








object AndroidUtilities {
    var density = 1f
    var rectTmp = RectF()
    var rectTmp2 = Rect()
    fun formatShortDuration(duration: Int): String {
        return formatDuration(duration, false)
    }

    fun formatLongDuration(duration: Int): String {
        return formatDuration(duration, true)
    }

    fun formatDuration(duration: Int, isLong: Boolean): String {
        val h = duration / 3600
        val m = duration / 60 % 60
        val s = duration % 60
        return if (h == 0) {
            if (isLong) {
                String.format(Locale.US, "%02d:%02d", m, s)
            } else {
                String.format(Locale.US, "%d:%02d", m, s)
            }
        } else {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        }
    }
    fun dp(value: Number): Int {
        return if (value == 0f) {
            0
        } else Math.ceil((density * value.toFloat()).toDouble()).toInt()
    }

    fun dpr(value: Float): Int {
        return if (value == 0f) {
            0
        } else Math.round(density * value)
    }

    fun dp2(value: Float): Int {
        return if (value == 0f) {
            0
        } else Math.floor((density * value).toDouble()).toInt()
    }

    fun compare(lhs: Int, rhs: Int): Int {
        if (lhs == rhs) {
            return 0
        } else if (lhs > rhs) {
            return 1
        }
        return -1
    }

    fun compare(lhs: Long, rhs: Long): Int {
        if (lhs == rhs) {
            return 0
        } else if (lhs > rhs) {
            return 1
        }
        return -1
    }

    fun dpf2(value: Float): Float {
        return if (value == 0f) {
            0f
        } else density * value
    }
    interface IntColorCallback {
        fun run(color: Int)
    }
}