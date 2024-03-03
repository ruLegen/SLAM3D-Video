package com.mag.slam3dvideo.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mag.slam3dvideo.utils.AndroidUtilities.dp


class VideoSeekBarView(context: Context?,attribSet:AttributeSet?=null) : View(context,attribSet) {
    private val paint = Paint()
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbWidth = dp(12)
    private val thumbHeight = dp(12)
    private var thumbDX = 0
    private var progress = 0f
    private var pressed = false
    private var delegate: SeekBarDelegate? = null

    interface SeekBarDelegate {
        fun onSeekBarDrag(progress: Float)
    }

    init {
        paint.color = -0xa3a3a4
        paint2.color = -0x1
    }

    fun setDelegate(seekBarDelegate: SeekBarDelegate?) {
        delegate = seekBarDelegate
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        val x = event.x
        val y = event.y
        var thumbX = (measuredWidth - thumbWidth) * progress
        if (event.action == MotionEvent.ACTION_DOWN) {
            val additionWidth: Int = (measuredHeight - thumbWidth) / 2
            if (thumbX - additionWidth <= x && x <= thumbX + thumbWidth + additionWidth && y >= 0 && y <= measuredHeight) {
                pressed = true
                thumbDX = (x - thumbX).toInt()
                parent.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (pressed) {
                if (event.action == MotionEvent.ACTION_UP && delegate != null) {
                    delegate!!.onSeekBarDrag(thumbX / (measuredWidth - thumbWidth))
                }
                pressed = false
                invalidate()
                return true
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (pressed) {
                thumbX = (x - thumbDX).toInt().toFloat()
                if (thumbX < 0) {
                    thumbX = 0f
                } else if (thumbX > measuredWidth - thumbWidth) {
                    thumbX = (measuredWidth - thumbWidth).toFloat()
                }
                progress = thumbX / (measuredWidth - thumbWidth)
                invalidate()
                return true
            }
        }
        return false
    }

    fun setProgress(progress: Float) {
        var progress = progress
        if (progress < 0) {
            progress = 0f
        } else if (progress > 1) {
            progress = 1f
        }
        this.progress = progress
        invalidate()
    }

    fun getProgress(): Float {
        return progress
    }

    override fun onDraw(canvas: Canvas) {
        val y: Int = (measuredHeight - thumbHeight) / 2
        val thumbX = ((measuredWidth - thumbWidth) * progress)
        canvas.drawRect(
            (thumbWidth / 2).toFloat(),
            (measuredHeight / 2 - dp(1)).toFloat(),
            (measuredWidth - thumbWidth / 2).toFloat(),
            (measuredHeight / 2 + dp(1)).toFloat(),
            paint
        )
        canvas.drawCircle(
            (thumbX + thumbWidth / 2).toFloat(),
            (y + thumbHeight / 2).toFloat(),
            (thumbWidth / 2).toFloat(),
            paint2
        )
    }
}