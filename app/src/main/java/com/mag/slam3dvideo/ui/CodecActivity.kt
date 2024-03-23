package com.mag.slam3dvideo.ui

import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.utils.MoviePlayer
import com.mag.slam3dvideo.utils.video.VideoDecoder
import java.io.File
import java.io.IOException


class CodecActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    SurfaceHolder.Callback, MoviePlayer.PlayerFeedback {
    var file: String = "/storage/emulated/0/DCIM/Camera/PXL_20240223_143249538.mp4"
    private val TAG: String = "CodecActivity"

    private var mSurfaceView: SurfaceView? = null
    private var mMovieFiles: Array<String> = arrayOf(file)
    private var mSelectedMovie = 0
    private var mShowStopLabel = false
    private var mPlayTask: MoviePlayer.PlayTask? = null
    private var mSurfaceHolderReady = false

    /**
     * Overridable  method to get layout id.  Any provided layout needs to include
     * the same views (or compatible) as active_play_movie_surface
     *
     */
    lateinit var videoDecoder:VideoDecoder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codec)
        mSurfaceView = findViewById(R.id.playMovie_surface)
        mSurfaceView!!.holder.addCallback(this)
        findViewById<Button>(R.id.startStop).apply {
            setOnClickListener{
                clickPlayStop(it)
            }
        }
        // Populate file-selection spinner.
        val spinner = findViewById<View>(R.id.playMovieFile_spinner) as Spinner
        // Need to create one of these fancy ArrayAdapter thingies, and specify the generic layout
        // for the widget itself.
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, mMovieFiles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner.
        spinner.adapter = adapter
        spinner.setOnItemSelectedListener(this)
        updateControls()


    }

    override fun onResume() {
        Log.d(TAG, "PlayMovieSurfaceActivity onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "PlayMovieSurfaceActivity onPause")
        super.onPause()
        // We're not keeping track of the state in static fields, so we need to shut the
        // playback down.  Ideally we'd preserve the state so that the player would continue
        // after a device rotation.
        //
        // We want to be sure that the player won't continue to send frames after we pause,
        // because we're tearing the view down.  So we wait for it to stop here.
        if (mPlayTask != null) {
            stopPlayback()
            mPlayTask!!.waitForStop()
        }
    }

    /*
     * Called when the movie Spinner gets touched.
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val spinner = parent as Spinner
        mSelectedMovie = spinner.selectedItemPosition
        Log.d(TAG, "onItemSelected: " + mSelectedMovie + " '" + mMovieFiles[mSelectedMovie] + "'")
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    /**
     * onClick handler for "play"/"stop" button.
     */
    fun clickPlayStop(@Suppress("unused") unused: View?) {
        if (mShowStopLabel) {
            Log.d(TAG, "stopping movie")
            stopPlayback()
            // Don't update the controls here -- let the task thread do it after the movie has
            // actually stopped.
            mShowStopLabel = false;
            updateControls();
        } else {
            if (mPlayTask != null) {
                Log.w(TAG, "movie already playing")
                return
            }
            Log.d(TAG, "starting movie")
//            val callback = SpeedControlCallback()
            val holder = mSurfaceView!!.holder
            val surface: Surface = holder.surface

            // Don't leave the last frame of the previous video hanging on the screen.
            // Looks weird if the aspect ratio changes.
            clearSurface(surface)
            var player: MoviePlayer? = null
            try {
                player = MoviePlayer(File(mMovieFiles[mSelectedMovie]), surface, null)
            } catch (ioe: IOException) {
                Log.e(TAG, "Unable to play movie", ioe)
                surface.release()
                return
            }
//            val layout: AspectFrameLayout =
//                findViewById<View>(R.id.playMovie_afl) as AspectFrameLayout
            val width: Int = player.getVideoWidth()
            val height: Int = player.getVideoHeight()
//            layout.setAspectRatio(width.toDouble() / height)
            holder.setFixedSize(width, height);
            mPlayTask = MoviePlayer.PlayTask(player, this)
            mShowStopLabel = true
            updateControls()
            mPlayTask!!.execute()
        }
    }

    /**
     * Requests stoppage if a movie is currently playing.
     */
    private fun stopPlayback() {
        if (mPlayTask != null) {
            mPlayTask!!.requestStop()
        }
    }

    // MoviePlayer.PlayerFeedback
    override fun playbackStopped() {
        Log.d(TAG, "playback stopped")
        mShowStopLabel = false
        mPlayTask = null
        updateControls()
    }

    /**
     * Updates the on-screen controls to reflect the current state of the app.
     */
    private fun updateControls() {
//        val play = findViewById<View>(R.id.play_stop_button) as Button
//        if (mShowStopLabel) {
//            play.setText(R.string.stop_button_text)
//        } else {
//            play.setText(R.string.play_button_text)
//        }
//        play.isEnabled = mSurfaceHolderReady
    }

    /**
     * Clears the playback surface to black.
     */
    private fun clearSurface(surface: Surface) {
        // We need to do this with OpenGL ES (*not* Canvas -- the "software render" bits
        // are sticky).  We can't stay connected to the Surface after we're done because
        // that'd prevent the video encoder from attaching.
        //
        // If the Surface is resized to be larger, the new portions will be black, so
        // clearing to something other than black may look weird unless we do the clear
        // post-resize.
//        val eglCore = EglCore()
//        val win = WindowSurface(eglCore, surface, false)
//        win.makeCurrent()
   //     GLES20.glClearColor(0f, 0f, 0f, 0f)
   //     GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//        win.swapBuffers()
//        win.release()
//        eglCore.release()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // There's a short delay between the start of the activity and the initialization
        // of the SurfaceHolder that backs the SurfaceView.  We don't want to try to
        // send a video stream to the SurfaceView before it has initialized, so we disable
        // the "play" button until this callback fires.
        Log.d(TAG, "surfaceCreated")
        mSurfaceHolderReady = true
        Thread{
            videoDecoder = VideoDecoder(File(file), mSurfaceView?.holder?.surface)
        }.apply {
            start()
        }
        updateControls()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}