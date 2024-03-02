package com.mag.slam3dvideo.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.adapters.VideoViewBindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.databinding.ActivityVideoViewerBinding
import com.mag.slam3dvideo.utils.AssetUtils
import com.mag.slam3dvideo.videmodels.VideoViewerViewModel

class VideoViewerActivity : AppCompatActivity() {
    lateinit var binding: ActivityVideoViewerBinding
    val file:String = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this).get<VideoViewerViewModel>()
        binding = ActivityVideoViewerBinding.inflate(layoutInflater)
        binding.vm = vm
        //ask permissions
        setContentView(binding.root)
        if(!AssetUtils.askMediaPermissions(this,1)){
            initTimeLine()
        }
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
        binding.timeline.setVideoPath(file)
    }
}
