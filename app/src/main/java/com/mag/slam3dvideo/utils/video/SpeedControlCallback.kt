package com.mag.slam3dvideo.utils.video

import android.util.Log
import com.mag.slam3dvideo.utils.MoviePlayer

interface VideoPlaybackCallback {
    fun preRender(progress: Float)
    fun postRender()
}

class SpeedControlCallback(val callback: VideoPlaybackCallback?) : MoviePlayer.FrameCallback {
    var mTotalDuraionUsec: Float = -1f

    private var mPrevPresentUsec: Long = 0
    private var mPrevMonoUsec: Long = 0
    private var mFixedFrameDurationUsec: Long = 0
    private var mLoopReset = false
    private var mIsPausedSignaled: Boolean = false
    private var pauseResumeLock = Object()

    /**
     * Sets a fixed playback rate.  If set, this will ignore the presentation time stamp
     * in the video file.  Must be called before playback thread starts.
     */
    fun setFixedPlaybackRate(fps: Int) {
        mFixedFrameDurationUsec = ONE_MILLION / fps
    }

    // runs on decode thread
    override fun preRender(presentationTimeUsec: Long) {
        callback?.preRender(presentationTimeUsec / mTotalDuraionUsec)
        // For the first frame, we grab the presentation time from the video
        // and the current monotonic clock time.  For subsequent frames, we
        // sleep for a bit to try to ensure that we're rendering frames at the
        // pace dictated by the video stream.
        //
        // If the frame rate is faster than vsync we should be dropping frames.  On
        // Android 4.4 this may not be happening.
        if (mPrevMonoUsec == 0L) {
            // Latch current values, then return immediately.
            mPrevMonoUsec = System.nanoTime() / 1000
            mPrevPresentUsec = presentationTimeUsec
        } else {
            // Compute the desired time delta between the previous frame and this frame.
            var frameDelta: Long
            if (mLoopReset) {
                // We don't get an indication of how long the last frame should appear
                // on-screen, so we just throw a reasonable value in.  We could probably
                // do better by using a previous frame duration or some sort of average;
                // for now we just use 30fps.
                mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30
                mLoopReset = false
            }
            frameDelta = if (mFixedFrameDurationUsec != 0L) {
                // Caller requested a fixed frame rate.  Ignore PTS.
                mFixedFrameDurationUsec
            } else {
                presentationTimeUsec - mPrevPresentUsec
            }
            if (frameDelta < 0) {
                Log.w(TAG, "Weird, video times went backward")
                frameDelta = 0
            } else if (frameDelta == 0L) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp")
            } else if (frameDelta > 10 * ONE_MILLION) {
                // Inter-frame times could be arbitrarily long.  For this player, we want
                // to alert the developer that their movie might have issues (maybe they
                // accidentally output timestamps in nsec rather than usec).
                Log.i(
                    TAG, "Inter-frame pause was " + frameDelta / ONE_MILLION +
                            "sec, capping at 5 sec"
                )
                frameDelta = 5 * ONE_MILLION
            }
            val desiredUsec = mPrevMonoUsec + frameDelta // when we want to wake up
            var nowUsec = System.nanoTime() / 1000
            while (nowUsec < desiredUsec - 100 /*&& mState == RUNNING*/) {
                // Sleep until it's time to wake up.  To be responsive to "stop" commands
                // we're going to wake up every half a second even if the sleep is supposed
                // to be longer (which should be rare).  The alternative would be
                // to interrupt the thread, but that requires more work.
                //
                // The precision of the sleep call varies widely from one device to another;
                // we may wake early or late.  Different devices will have a minimum possible
                // sleep time. If we're within 100us of the target time, we'll probably
                // overshoot if we try to sleep, so just go ahead and continue on.
                var sleepTimeUsec = desiredUsec - nowUsec
                if (sleepTimeUsec > 500000) {
                    sleepTimeUsec = 500000
                }
                try {
                    if (CHECK_SLEEP_TIME) {
                        val startNsec = System.nanoTime()
                        Thread.sleep(sleepTimeUsec / 1000, (sleepTimeUsec % 1000).toInt() * 1000)
                        val actualSleepNsec = System.nanoTime() - startNsec
                        Log.d(
                            TAG, "sleep=" + sleepTimeUsec + " actual=" + actualSleepNsec / 1000 +
                                    " diff=" + Math.abs(actualSleepNsec / 1000 - sleepTimeUsec) +
                                    " (usec)"
                        )
                    } else {
                        Thread.sleep(sleepTimeUsec / 1000, (sleepTimeUsec % 1000).toInt() * 1000)
                    }
                } catch (ie: InterruptedException) {
                }
                nowUsec = System.nanoTime() / 1000
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            mPrevMonoUsec += frameDelta
            mPrevPresentUsec += frameDelta
        }
    }

    // runs on decode thread
    override fun postRender() {
        callback?.postRender()
        synchronized(pauseResumeLock) {
            if (mIsPausedSignaled) {
                mPrevMonoUsec =0
                mPrevPresentUsec =0
                pauseResumeLock.wait()
                mIsPausedSignaled = false
            }
        }
    }

    override fun loopReset() {
        mLoopReset = true
    }

    fun resume() {
        synchronized(pauseResumeLock){
            pauseResumeLock.notifyAll()
        }
    }

    fun pause() {
        mIsPausedSignaled = true
    }

    companion object {
        private val TAG: String = "SpeedControllCallback"
        private const val CHECK_SLEEP_TIME = false
        private const val ONE_MILLION = 1000000L
    }
}

