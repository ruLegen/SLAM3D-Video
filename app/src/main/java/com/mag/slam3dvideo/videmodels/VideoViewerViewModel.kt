package com.mag.slam3dvideo.videmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import com.mag.slam3dvideo.BR
import com.mag.slam3dvideo.ui.components.VideoTimelineView

class VideoViewerViewModel(application: Application) : ObservableViewModel(application){
    var fileSelected:((file:String?) -> Unit)? = null

    var mediaSelectClicked: (() -> Unit)? = null

    var file:String? = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
    var timeLineDelegate: VideoTimelineView.VideoTimelineViewDelegate? = null
    var progress: Float = 0f

    init {

    }


    fun clicked(view:View){
        mediaSelectClicked?.invoke()
    }

    fun onMediaSelected(path: String?){
        file = path
    }
}