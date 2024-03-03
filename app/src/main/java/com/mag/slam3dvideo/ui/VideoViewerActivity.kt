package com.mag.slam3dvideo.ui

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.PathUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.mag.slam3dvideo.databinding.ActivityVideoViewerBinding
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.utils.FileUtils
import com.mag.slam3dvideo.videmodels.VideoViewerViewModel
import java.security.Permission


class VideoViewerActivity : AppCompatActivity() {
    lateinit var binding: ActivityVideoViewerBinding
    @RequiresApi(Build.VERSION_CODES.Q)
    var mediaSelectorLauncher = registerForActivityResult<PickVisualMediaRequest, Uri>(ActivityResultContracts.PickVisualMedia()){
        it?.let{
            val path = FileUtils(baseContext).getPath(it)
            binding.vm?.onMediaSelected(path)
            initTimeLine()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this).get<VideoViewerViewModel>()
        vm.mediaSelectClicked = {
            mediaSelectorLauncher.launch(PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                .build()
            )
        }
        binding = ActivityVideoViewerBinding.inflate(layoutInflater)
        binding.vm = vm
        //ask permissions
        setContentView(binding.root)
        if(!AssetUtils.askMediaPermissions(this,1)){
            initTimeLine()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.vm?.mediaSelectClicked = null
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(AssetUtils.hasAllMediaPermissions(this)){
            initTimeLine()
        }else{
            Toast.makeText(this,"Cannot start app, no media permissions granted",Toast.LENGTH_LONG).show();
        }
    }
    private fun initTimeLine() {
        val vm = binding.vm
        val timeline = binding.timeline
        timeline.reset()
        timeline.setDelegate(binding.vm!!.timeLineDelegate)
        timeline.setTimeHintView(binding.timeHint)
        timeline.setRoundFrames(false)
        timeline.setVideoPath(vm?.file)
        timeline.progress = vm?.progress ?: 0f
    }
}
