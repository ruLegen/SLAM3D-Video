package com.mag.slam3dvideo.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.utils.AndroidUtilities
import com.mag.slam3dvideo.utils.CancellationToken
import com.mag.slam3dvideo.utils.CancellationTokenSource
import com.mag.slam3dvideo.utils.TaskRunner
import com.mag.slam3dvideo.utils.Theme
import java.io.FileInputStream
import java.util.concurrent.Callable


class VideoTimelineView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private val timeLineTopOffset: Int
        //       get() = measuredHeight - AndroidUtilities.dp(32) shr 1
        get() = measuredHeight

    var leftProgress = 0f
        private set
    var rightProgress = 1f
        private set
    var progress = 0f
        set(value) {
            field = MathUtils.clamp(value, 0f, 1f)
            invalidate()
        }

    private val paint2 = Paint()
    private val backgroundGrayPaint = Paint()
    private var pressedLeft = false
    private var pressedRight = false
    private var pressedProgress = false
    private var pressDx = 0f
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null
    private var delegate: VideoTimelineViewDelegate? = null
    private val frames = ArrayList<Bitmap>()
    private var currentTask: BitmapLoadingTask = BitmapLoadingTask()
    private var frameTimeOffset: Long = 0
    private var frameWidth = 0
    private var frameHeight = 0
    private var framesToLoad = 0
    private var maxProgressDiff = 1.0f
    private var minProgressDiff = 0.0f
    private var isRoundFrames = false
    private var rect1: Rect? = null
    private var rect2: Rect? = null
    private var roundCornersSize = 0
    private var roundCornerBitmap: Bitmap? = null
    private val keyframes = ArrayList<Bitmap>()
    private var framesLoaded = false
    private var scalePreviewFrames = true
    private var timeHintView: TimeHintView? = null
    private val bitmapTaskRunner: TaskRunner = TaskRunner(Handler(Looper.getMainLooper()))
    private var bitmapCancelTokenSource = CancellationTokenSource()
    private var isBitmapLoadingInProgress: Boolean = false

    var videoFrameCount: Long = 0
        private set
    var videoCaptureFps: Float = 0f
        private set
    var videoLength: Long = 0
        private set

    var path: Path? = null
    var thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var useClip = true
    fun setKeyframes(keyframes: ArrayList<Bitmap>?) {
        this.keyframes.clear()
        this.keyframes.addAll(keyframes!!)
    }

    interface VideoTimelineViewDelegate {
        fun onLeftProgressChanged(view: VideoTimelineView, progress: Float)
        fun onRightProgressChanged(view: VideoTimelineView, progress: Float)
        fun onSeek(view: VideoTimelineView, progress: Float)
        fun didStartDragging()
        fun didStopDragging()
    }

    init {
        paint2.color = 0x7f000000
        thumbPaint.apply {
            color = Color.WHITE
            strokeWidth = AndroidUtilities.dpf2(2f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        progressPaint.apply {
            strokeWidth = AndroidUtilities.dpf2(4f)
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND

        }
        updateColors()
    }

    fun updateColors() {
//        backgroundGrayPaint.color = Theme.getColor(Theme.key_windowBackgroundGray)
        roundCornersSize = 0
        timeHintView?.updateColors()
    }

    fun setMinProgressDiff(value: Float) {
        minProgressDiff = value
    }

    fun setMaxProgressDiff(value: Float) {
        maxProgressDiff = value
        if (rightProgress - leftProgress > maxProgressDiff) {
            rightProgress = leftProgress + maxProgressDiff
            invalidate()
        }
    }

    fun setRoundFrames(value: Boolean) {
        isRoundFrames = value
        if (isRoundFrames) {
            rect1 = Rect(
                AndroidUtilities.dp(14f),
                AndroidUtilities.dp(14f),
                AndroidUtilities.dp(14f + 28),
                AndroidUtilities.dp(14f + 28)
            )
            rect2 = Rect()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        val x = event.x
        val y = event.y
        val width: Int = getMeasuredWidth() - AndroidUtilities.dp(24)
        var startX: Int = (width * leftProgress).toInt() + AndroidUtilities.dp(12)
        var endX: Int = (width * rightProgress).toInt() + AndroidUtilities.dp(12)
        if (event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
            if (mediaMetadataRetriever == null) {
                return false
            }
            val additionWidth: Int = AndroidUtilities.dp(24)
            if (startX - additionWidth <= x && x <= startX + additionWidth && y >= 0 && y <= measuredHeight) {
                delegate?.didStartDragging()
                pressedLeft = true
                pressDx = x - startX
                timeHintView?.setTime((videoLength / 1000f * leftProgress).toInt())
                timeHintView?.setCx((startX + getLeft() + AndroidUtilities.dp(4f)).toFloat())
                timeHintView?.show(true)
                invalidate()
                return true
            } else if (endX - additionWidth <= x && x <= endX + additionWidth && y >= 0 && y <= measuredHeight) {
                delegate?.didStartDragging()
                pressedRight = true
                pressDx = x - endX
                timeHintView?.setTime((videoLength / 1000f * rightProgress).toInt())
                timeHintView?.setCx(((endX + getLeft() - AndroidUtilities.dp(4f)).toFloat()))
                timeHintView?.show(true)
                invalidate()
                return true
            } else {
                timeHintView?.show(false)
                pressedProgress = true
                invalidate()
                return true
            }
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (pressedLeft) {
                delegate?.didStopDragging()
                pressedLeft = false
                invalidate()
                timeHintView?.show(false)
                return true
            } else if (pressedRight) {
                delegate?.didStopDragging()
                pressedRight = false
                invalidate()
                timeHintView?.show(false)
                return true
            } else if (pressedProgress) {
                delegate?.didStopDragging()
                pressedProgress = false
                invalidate()
                timeHintView?.show(false)
                return true
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (pressedLeft) {
                startX = (x - pressDx).toInt()
                if (startX < AndroidUtilities.dp(16f)) {
                    startX = AndroidUtilities.dp(16f).toInt()
                } else if (startX > endX) {
                    startX = endX
                }
                leftProgress = (startX - AndroidUtilities.dp(16f)) / width.toFloat()
                if (rightProgress - leftProgress > maxProgressDiff) {
                    rightProgress = leftProgress + maxProgressDiff
                } else if (minProgressDiff != 0f && rightProgress - leftProgress < minProgressDiff) {
                    leftProgress = rightProgress - minProgressDiff
                    if (leftProgress < 0) {
                        leftProgress = 0f
                    }
                }
                timeHintView?.setCx(
                    width * leftProgress + AndroidUtilities.dpf2(12f) + getLeft() - AndroidUtilities.dp(
                        4
                    )
                )
                timeHintView?.setTime((videoLength / 1000f * leftProgress).toInt())
                timeHintView?.show(true)
                delegate?.onLeftProgressChanged(this, leftProgress)
                invalidate()
                return true
            } else if (pressedRight) {
                endX = (x - pressDx).toInt()
                if (endX < startX) {
                    endX = startX
                } else if (endX > width + AndroidUtilities.dp(16f)) {
                    endX = (width + AndroidUtilities.dp(16f)).toInt()
                }
                rightProgress = (endX - AndroidUtilities.dp(16f)) / width.toFloat()
                if (rightProgress - leftProgress > maxProgressDiff) {
                    leftProgress = rightProgress - maxProgressDiff
                } else if (minProgressDiff != 0f && rightProgress - leftProgress < minProgressDiff) {
                    rightProgress = leftProgress + minProgressDiff
                    if (rightProgress > 1.0f) {
                        rightProgress = 1.0f
                    }
                }
                timeHintView?.setCx(
                    width * rightProgress + AndroidUtilities.dpf2(12f) + getLeft() + AndroidUtilities.dp(
                        4f
                    )
                )
                timeHintView?.show(true)
                timeHintView?.setTime((videoLength / 1000f * rightProgress).toInt())
                delegate?.onRightProgressChanged(this, rightProgress)
                invalidate()
                return true
            } else if (pressedProgress) {
                progress = x / width.toFloat()
                progress = MathUtils.clamp(progress, 0f, 1f)
                timeHintView?.setCx(
                    width * progress + AndroidUtilities.dpf2(12f) + left + AndroidUtilities.dp(
                        4f
                    )
                )
                timeHintView?.setTime((videoLength / 1000f * progress).toInt())
                timeHintView?.show(true)
                delegate?.onSeek(this, progress)
                invalidate()
                return true
            }
        }
        return false
    }

    fun setVideoPath(path: String?) {
        destroy(false)
        mediaMetadataRetriever = MediaMetadataRetriever()
        leftProgress = 0.0f
        rightProgress = 1.0f
        try {
            val inputStream = FileInputStream(path)
            mediaMetadataRetriever!!.setDataSource(inputStream.fd)
            val duration =
                mediaMetadataRetriever!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            videoLength = duration!!.toLong()
            videoCaptureFps =
                mediaMetadataRetriever!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    ?.toFloat() ?: 0f
            videoFrameCount =
                mediaMetadataRetriever!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                    ?.toLong() ?: -1
        } catch (e: Exception) {
            Log.e("VIDEOTIMELINE", e.message.toString());
            // FileLog.e(e)
        }
        invalidate()
    }

    fun setDelegate(videoTimelineViewDelegate: VideoTimelineView.VideoTimelineViewDelegate?) {
        delegate = videoTimelineViewDelegate
    }

    private fun reloadFrames() {
        if (mediaMetadataRetriever == null) {
            return
        }
        bitmapCancelTokenSource?.cancel()
        bitmapCancelTokenSource = CancellationTokenSource()
        val token = bitmapCancelTokenSource.token

        if (isRoundFrames) {
            frameWidth = AndroidUtilities.dp(56)
            frameHeight = frameWidth
            framesToLoad = Math.max(
                1,
                Math.ceil(((getMeasuredWidth() - AndroidUtilities.dp(16)) / (frameHeight / 2.0f)).toDouble())
                    .toInt()
            )
        } else {
            frameHeight = AndroidUtilities.dp(120)
            framesToLoad =
                Math.max(1, (getMeasuredWidth() - AndroidUtilities.dp(16)) / frameHeight)
            frameWidth =
                Math.ceil(((getMeasuredWidth() - AndroidUtilities.dp(16)) / framesToLoad.toFloat()).toDouble())
                    .toInt()
        }
        frameTimeOffset = videoLength / framesToLoad
        if (!keyframes.isEmpty()) {
            val keyFramesCount = keyframes.size
            val step = keyFramesCount / framesToLoad.toFloat()
            var currentP = 0f
            for (i in 0 until framesToLoad) {
                frames.add(keyframes[currentP.toInt()])
                currentP += step
            }
            return
        }
        framesLoaded = false
        currentTask = loadFramesAsync(framesToLoad, token)
    }

    private fun loadFramesAsync(maxFrameCount: Int, token: CancellationToken):BitmapLoadingTask{
        var loadingTask= BitmapLoadingTask()
        if (maxFrameCount == 0 || token.isCancelRequested)
            return loadingTask
        token.register {
            loadingTask.isActive=false
        }
        var currentLoadFrame = 0
        loadingTask.isActive = true
        val task = object : Callable<Bitmap?> {
            override fun call(): Bitmap? {
                var bitmap: Bitmap? = null
                if (token.isCancelRequested) {
                    return null as Bitmap
                }
                try {
                    bitmap = mediaMetadataRetriever!!.getFrameAtTime(
                        frameTimeOffset * currentLoadFrame * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (token.isCancelRequested) {
                        return null as Bitmap
                    }
                    if (bitmap != null) {
                        val result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.config)
                        val canvas = Canvas(result)
                        val scaleX = frameWidth.toFloat() / bitmap.width.toFloat()
                        val scaleY = frameHeight.toFloat() / bitmap.height.toFloat()
                        val scale = Math.max(scaleX, scaleY)
                        val w = (bitmap.width * scale).toInt()
                        val h = (bitmap.height * scale).toInt()
                        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                        val destRect = Rect((frameWidth - w) / 2, (frameHeight - h) / 2, w, h)
                        canvas.drawBitmap(bitmap, srcRect, destRect, null)
                        bitmap.recycle()
                        bitmap = result
                    }
                } catch (e: Exception) {
                    // FileLog.e(e)
                }
                return bitmap!!
            }
        }
        val callback = object : TaskRunner.Callback<Bitmap?> {
            override fun onComplete(bitmap: Bitmap?) {
                if (token.isCancelRequested) {
                    return
                }
                if (bitmap == null) {
                    loadingTask.isActive = false
                    return
                }
                frames.add(bitmap)
                invalidate()
                if (currentLoadFrame < framesToLoad) {
                    currentLoadFrame++
                    bitmapTaskRunner.executeAsync(task, this)
                } else {
                    framesLoaded = true
                    loadingTask.isActive = false
                }
            }
        }

        bitmapTaskRunner.executeAsync(task, callback)
        return loadingTask
    }

    @JvmOverloads
    fun destroy(recycle: Boolean = true) {
        synchronized(VideoTimelineView.Companion.sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever!!.release()
                    mediaMetadataRetriever = null
                }
            } catch (e: Exception) {
                // FileLog.e(e)
            }
        }
        if (recycle) {
            if (!keyframes.isEmpty()) {
                for (a in keyframes.indices) {
                    val bitmap = keyframes[a]
                    bitmap?.recycle()
                }
            } else {
                for (a in frames.indices) {
                    val bitmap = frames[a]
                    bitmap?.recycle()
                }
            }
        }
        bitmapCancelTokenSource.cancel()
        keyframes.clear()
        frames.clear()
    }

    fun clearFrames() {
        bitmapCancelTokenSource.cancel()
        if (keyframes.isEmpty()) {
            for (a in frames.indices) {
                val bitmap = frames[a]
                bitmap?.recycle()
            }
        }
        frames.clear()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (useClip) {
            if (path == null) {
                path = Path()
            }
            path!!.rewind()
            val topOffset = timeLineTopOffset
            AndroidUtilities.rectTmp.set(
                0F,
                topOffset.toFloat(),
                measuredWidth.toFloat(),
                (measuredHeight - topOffset).toFloat()
            )
            path!!.addRoundRect(
                AndroidUtilities.rectTmp,
                AndroidUtilities.dp(7).toFloat(),
                AndroidUtilities.dp(7).toFloat(),
                Path.Direction.CCW
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDraw(canvas: Canvas) {
        if (useClip) {
            canvas.save()
            if (path != null) {
                canvas.clipPath(path!!)
            }
        }
        val width: Int = measuredWidth - AndroidUtilities.dp(24)
        val startX: Int = (width * leftProgress).toInt() + AndroidUtilities.dp(12)
        val endX: Int = (width * rightProgress).toInt() + AndroidUtilities.dp(12)
        val progressX: Int = (width * progress).toInt() + AndroidUtilities.dp(12)
        val topOffset: Int = timeLineTopOffset
        if (frames.isEmpty() && !currentTask.isActive) {
            reloadFrames()
        }
        if (!frames.isEmpty()) {
            if (!framesLoaded) {
                canvas.drawRect(
                    0f,
                    topOffset.toFloat(),
                    measuredWidth.toFloat(),
                    (measuredHeight - topOffset).toFloat(),
                    backgroundGrayPaint
                )
            }
            var offset = 0
            for (a in frames.indices) {
                val bitmap = frames[a]
                if (!bitmap.isRecycled) {
                    val x = offset * if (isRoundFrames) frameWidth / 2 else frameWidth
                    if (isRoundFrames) {
                        rect2!![x, topOffset, x + AndroidUtilities.dp(28)] =
                            topOffset + AndroidUtilities.dp(32)
                        canvas.drawBitmap(bitmap, rect1, rect2!!, null)
                    } else {
                        if (scalePreviewFrames)
                            canvas.drawBitmap(
                                bitmap,
                                null,
                                Rect(x, 0, x + frameWidth, topOffset),
                                null
                            )
                        else
                            canvas.drawBitmap(
                                bitmap,
                                x.toFloat(),
                                measuredHeight - topOffset.toFloat(),
                                null
                            )
                    }
                }
                offset++
            }
        } else {
            if (useClip) {
                canvas.restore()
            }
            return
        }
        canvas.drawRect(
            0f,
            topOffset.toFloat(),
            startX.toFloat(),
            (measuredHeight - topOffset).toFloat(),
            paint2
        )
        canvas.drawRect(
            endX.toFloat(),
            topOffset.toFloat(),
            measuredWidth.toFloat(),
            (measuredHeight - topOffset).toFloat(),
            paint2
        )
        drawTimeLineVerticalLine(canvas, startX, topOffset, thumbPaint)
        drawTimeLineVerticalLine(canvas, endX, topOffset, thumbPaint)
        drawTimeLineVerticalLine(canvas, progressX, topOffset, progressPaint)

        if (useClip) {
            canvas.restore()
        } else {
            drawCorners(
                canvas,
                measuredHeight - topOffset * 2,
                measuredWidth,
                0,
                topOffset
            )
        }

    }

    fun drawTimeLineVerticalLine(canvas: Canvas, x: Int, height: Int, paint: Paint) {
        canvas.drawLine(
            (x + AndroidUtilities.dp(4)).toFloat(),
            (height + AndroidUtilities.dp(10)).toFloat(),
            (x + AndroidUtilities.dp(4)).toFloat(),
            (measuredHeight - AndroidUtilities.dp(10) - height).toFloat(),
            paint
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun drawCorners(canvas: Canvas, height: Int, width: Int, left: Int, top: Int) {
        if (AndroidUtilities.dp(6) !== roundCornersSize) {
            roundCornersSize = AndroidUtilities.dp(6)
            roundCornerBitmap = Bitmap.createBitmap(
                AndroidUtilities.dp(6),
                AndroidUtilities.dp(6),
                Bitmap.Config.ARGB_8888
            )
            val bitmapCanvas = Canvas(roundCornerBitmap!!)
            val xRefP = Paint(Paint.ANTI_ALIAS_FLAG)
            xRefP.color = 0
            xRefP.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
            bitmapCanvas.drawColor(Color.CYAN)
            bitmapCanvas.drawCircle(
                roundCornersSize.toFloat(),
                roundCornersSize.toFloat(),
                roundCornersSize.toFloat(),
                xRefP
            )
        }
        val sizeHalf = roundCornersSize shr 1
        canvas.save()
        canvas.drawBitmap(roundCornerBitmap!!, left.toFloat(), top.toFloat(), null)
        canvas.rotate(-90f, (left + sizeHalf).toFloat(), (top + height - sizeHalf).toFloat())
        canvas.drawBitmap(
            roundCornerBitmap!!,
            left.toFloat(),
            (top + height - roundCornersSize).toFloat(),
            null
        )
        canvas.restore()
        canvas.save()
        canvas.rotate(
            180f,
            (left + width - sizeHalf).toFloat(),
            (top + height - sizeHalf).toFloat()
        )
        canvas.drawBitmap(
            roundCornerBitmap!!,
            (left + width - roundCornersSize).toFloat(),
            (top + height - roundCornersSize).toFloat(),
            null
        )
        canvas.restore()
        canvas.save()
        canvas.rotate(90f, (left + width - sizeHalf).toFloat(), (top + sizeHalf).toFloat())
        canvas.drawBitmap(
            roundCornerBitmap!!,
            (left + width - roundCornersSize).toFloat(),
            top.toFloat(),
            null
        )
        canvas.restore()
    }

    fun setTimeHintView(timeHintView: TimeHintView?) {
        this.timeHintView = timeHintView
    }

    fun reset() {
        clearFrames()
        progress = 0f
        leftProgress = 0f
        rightProgress = 1f
    }

    companion object {
        private val sync = Any()
    }
}

class TimeHintView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private lateinit var tooltipBackground: Drawable
    private var defaultArrowHeight: Float = 32f
    private var tooltipBackgroundArrow: Drawable? = null
    private var tooltipLayout: StaticLayout? = null
    private val tooltipPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var lastTime: Long = -1
    private val tooltipAlpha = 0f
    private val showTooltip = false
    private val showTooltipStartTime: Long = 0
    private var cx = 0f
    private var scale = 0f
    private var show = false

    init {
        tooltipPaint.textSize = AndroidUtilities.dp(14f).toFloat()
        tooltipBackgroundArrow = ContextCompat.getDrawable(context!!, R.drawable.tooltip_arrow)
        tooltipBackground = Theme.createRoundRectDrawable(
            AndroidUtilities.dp(5), Color.MAGENTA
        )
        updateColors()
        setTime(0)
    }

    fun setTime(timeInSeconds: Int) {
        if (timeInSeconds.toLong() != lastTime) {
            lastTime = timeInSeconds.toLong()
            val s: String = AndroidUtilities.formatShortDuration(timeInSeconds)
            tooltipLayout = StaticLayout.Builder.obtain(
                s,
                0,
                s.length,
                tooltipPaint,
                tooltipPaint.measureText(s).toInt(),
            ).build()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                tooltipLayout!!.height + AndroidUtilities.dp(4) + (tooltipBackgroundArrow?.intrinsicHeight
                    ?: defaultArrowHeight).toInt(),
                MeasureSpec.EXACTLY
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (tooltipLayout == null) {
            return
        }
        if (show) {
            if (scale != 1f) {
                scale += 0.12f
                if (scale > 1f) scale = 1f
                invalidate()
            }
        } else {
            if (scale != 0f) {
                scale -= 0.12f
                if (scale < 0f) scale = 0f
                invalidate()
            }
            if (scale == 0f) {
                return
            }
        }
        val alpha = (255 * if (scale > 0.5f) 1f else scale / 0.5f).toInt()
        canvas.save()
        canvas.scale(scale, scale, cx, measuredHeight.toFloat())
        canvas.translate(cx - tooltipLayout!!.width / 2f, 0f)
        tooltipBackground.setBounds(
            -AndroidUtilities.dp(8),
            0,
            tooltipLayout!!.width + AndroidUtilities.dp(8),
            (tooltipLayout!!.height + AndroidUtilities.dpf2(4f)).toInt()
        )
        tooltipBackgroundArrow!!.setBounds(
            tooltipLayout!!.width / 2 - (tooltipBackgroundArrow?.intrinsicWidth?.div(2)
                ?: (defaultArrowHeight / 2).toInt()),
            (tooltipLayout!!.height + AndroidUtilities.dpf2(4f)).toInt(),
            tooltipLayout!!.width / 2 + (tooltipBackgroundArrow?.intrinsicWidth?.div(2)
                ?: (defaultArrowHeight / 2).toInt()),
            (tooltipLayout!!.height + AndroidUtilities.dpf2(4f)).toInt() + (tooltipBackgroundArrow?.intrinsicHeight
                ?: 0)
        )
        tooltipBackgroundArrow?.alpha = alpha
        tooltipBackground.alpha = alpha
        tooltipPaint.alpha = alpha
        tooltipBackgroundArrow?.draw(canvas)
        tooltipBackground.draw(canvas)
        canvas.translate(0f, AndroidUtilities.dpf2(1f))
        tooltipLayout!!.draw(canvas)
        canvas.restore()
    }

    fun updateColors() {
        tooltipPaint.color = Color.RED
        tooltipBackground = Theme.createRoundRectDrawable(
            AndroidUtilities.dp(5), Color.CYAN
        )
        tooltipBackgroundArrow!!.colorFilter = PorterDuffColorFilter(
            Color.GREEN,
            PorterDuff.Mode.MULTIPLY
        )
    }

    fun setCx(v: Float) {
        cx = v
        invalidate()
    }

    fun show(s: Boolean) {
        show = s
        invalidate()
    }


}
    class BitmapLoadingTask{
        var isActive:Boolean = false
    }


