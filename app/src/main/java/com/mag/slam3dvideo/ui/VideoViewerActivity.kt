package com.mag.slam3dvideo.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.mag.slam3dvideo.databinding.ActivityVideoViewerBinding
import com.mag.slam3dvideo.ui.components.VideoTimelineView
import com.mag.slam3dvideo.utils.AndroidUtilities
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.utils.CancellationTokenSource
import com.mag.slam3dvideo.utils.FileUtils
import com.mag.slam3dvideo.utils.TaskRunner
import com.mag.slam3dvideo.videmodels.VideoViewerViewModel


class VideoViewerActivity : AppCompatActivity() {
    lateinit var binding: ActivityVideoViewerBinding
    private var taskRunner: TaskRunner = TaskRunner()
    private var tokenSource: CancellationTokenSource = CancellationTokenSource()
    private val lock = Any()
    private var prevProgress: Float = 0F
    @RequiresApi(Build.VERSION_CODES.Q)
    var mediaSelectorLauncher =
        registerForActivityResult<PickVisualMediaRequest, Uri>(ActivityResultContracts.PickVisualMedia()) {
            it?.let {
                val path = FileUtils(baseContext).getPath(it)
                binding.vm?.onMediaSelected(path)
                initVideoPlayer()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this).get<VideoViewerViewModel>()
        vm.mediaSelectClicked = {
            mediaSelectorLauncher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    .build()
            )
        }
        binding = ActivityVideoViewerBinding.inflate(layoutInflater)
        binding.vm = vm
        //ask permissions
        setContentView(binding.root)
        if (!AssetUtils.askMediaPermissions(this, 1)) {
            initVideoPlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.vm?.mediaSelectClicked = null
        tokenSource?.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (AssetUtils.hasAllMediaPermissions(this)) {
            initVideoPlayer()
        } else {
            Toast.makeText(
                this,
                "Cannot start app, no media permissions granted",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private fun initVideoPlayer() {
        val vm = binding.vm
        val player = binding.videoView
        val timeline = binding.timeline
        tokenSource?.cancel()
        val token = tokenSource.token

        timeline.reset()
//        timeline.setDelegate(vm!!.timeLineDelegate)
        timeline.setDelegate(object : VideoTimelineView.VideoTimelineViewDelegate {
            var playerWasActive:Boolean = false
            override fun onLeftProgressChanged(view: VideoTimelineView, progress: Float) {
                val currentTime = (view.videoLength * progress).toInt()
                view.progress = progress
                player.seekTo(currentTime)
            }

            override fun onRightProgressChanged(view: VideoTimelineView, progress: Float) {
                val currentTime = (view.videoLength * progress).toInt()
                view.progress = progress
                player.seekTo(currentTime)
            }

            override fun onSeek(view: VideoTimelineView, p: Float) {
                val currentTime = (view.videoLength * p).toInt()
                player.seekTo(currentTime)
                prevProgress = p
            }

            override fun didStartDragging(view: VideoTimelineView) {
                playerWasActive =  player.isPlaying
                player.pause()
            }

            override fun didStopDragging(view: VideoTimelineView) {
                view.progress = prevProgress
                val currentTime = (view.videoLength * prevProgress).toInt()

                player.seekTo(currentTime)
                if(playerWasActive){
                    player.start()
                    playerWasActive = false
                }

            }

        })
        timeline.setTimeHintView(binding.timeHint)
        timeline.setRoundFrames(false)
        timeline.setVideoPath(vm?.file)
        timeline.progress = vm?.progress ?: 0f
        val mc = android.widget.MediaController(this@VideoViewerActivity)
        player.setMediaController(mc)
        mc.setAnchorView(player)
        player.setOnPreparedListener {
            it.setOnSeekCompleteListener {
                timeline.progress = it.currentPosition / it.duration.toFloat()
            }
        }
        player.setVideoPath(vm?.file)
        player.requestFocus(0);
        taskRunner.executeAsync({
            val handler = Handler(Looper.getMainLooper())
            while (true) {
                if (!token.isCancelRequested) {
                    break
                }
                handler.post {
                    if (player.isPlaying) {
                        try {
                            prevProgress = player.currentPosition / player.duration.toFloat()
                            timeline.progress = prevProgress
                        } catch (_: Exception) {
                        }
                    }
                }
                try {
                    Thread.sleep(50)
                } catch (_: Exception) {

                }
            }
        })
    }
}
