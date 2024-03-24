package com.mag.slam3dvideo.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.MediaController
import android.widget.MediaController.MediaPlayerControl

interface MediaPlayerControlCallback{
    fun onStart(sender:MediaPlayerControl)
    fun onPause(sender: MediaPlayerControl)
    fun onSeek(sender:MediaPlayerControl, pos:Int)
}
class SurfaceMediaPlayerControl(context: Context?, attrs: AttributeSet?) : SurfaceView(context,attrs),
    MediaPlayerControl {
    private var mMediaController: MediaController? = null
    var mediaControlCallback: MediaPlayerControlCallback? = null
    var mIsPlaying:Boolean = false
        private set
    override fun start() {
        mIsPlaying =true
        mediaControlCallback?.onStart(this)
    }

    override fun pause() {
        mIsPlaying=false
        mediaControlCallback?.onPause(this)
    }

    override fun getDuration(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        return  0
    }

    override fun seekTo(pos: Int) {
        mediaControlCallback?.onSeek(this,pos)
    }

    override fun isPlaying(): Boolean {
        return mIsPlaying
    }

    override fun getBufferPercentage(): Int {
        return  0
    }

    override fun canPause(): Boolean {
        return  true
    }

    override fun canSeekBackward(): Boolean {
        return  true
    }

    override fun canSeekForward(): Boolean {
        return  true
    }

    override fun getAudioSessionId(): Int {
        return  0
    }
    fun setMediaController(controller: MediaController) {
        mMediaController?.hide()
        mMediaController = controller
        attachMediaController()
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && mMediaController != null) {
            toggleMediaControlsVisibility()
        }
        return super.onTouchEvent(ev)
    }
    private fun toggleMediaControlsVisibility() {
        if (mMediaController!!.isShowing) {
            mMediaController!!.hide()
        } else {
            mMediaController!!.show()
        }
    }
    private fun attachMediaController() {
        if (mMediaController == null)
            return
        mMediaController!!.setMediaPlayer(this)
        val anchorView = if (this.parent is View) this.parent as View else this
        mMediaController!!.setAnchorView(anchorView)
        mMediaController!!.isEnabled = true
    }

}