package com.mag.slam3dvideo.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.MediaController

class SurfaceMediaPlayerControl(context: Context?, attrs: AttributeSet?) : SurfaceView(context,attrs),MediaController.MediaPlayerControl {
    private var mMediaController: MediaController? = null
    var mIsPlaying:Boolean = false
        private set
    override fun start() {
        mIsPlaying =true
    }

    override fun pause() {
        mIsPlaying=false
    }

    override fun getDuration(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        return  0
    }

    override fun seekTo(pos: Int) {
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
        return  false
    }

    override fun canSeekForward(): Boolean {
        return  false
    }

    override fun getAudioSessionId(): Int {
        return  0
    }
    fun setMediaController(controller: MediaController) {
        mMediaController?.hide()
        mMediaController = controller
        attachMediaController()
    }
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && mMediaController != null) {
            toggleMediaControlsVisiblity()
        }
        return super.onTouchEvent(ev)
    }
    private fun toggleMediaControlsVisiblity() {
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
        mMediaController!!.setEnabled(true)
    }

}