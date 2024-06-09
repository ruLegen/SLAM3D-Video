package com.mag.slam3dvideo.videmodels

import android.app.Application
import android.content.Intent
import android.view.View
import com.mag.slam3dvideo.ui.MapViewActivity
import com.mag.slam3dvideo.ui.SettingsActivity
import com.mag.slam3dvideo.ui.components.VideoTimelineView

class VideoViewerViewModel(application: Application) : ObservableViewModel(application) {
    var fileSelected: ((file: String?) -> Unit)? = null

    var mediaSelectClicked: (() -> Unit)? = null

    //    var file:String? = "/storage/emulated/0/DCIM/Camera/PXL_20230318_132255477.mp4"
    var file: String? = "/storage/emulated/0/DCIM/Camera/PXL_20240223_143249538.mp4"

    var timeLineDelegate: VideoTimelineView.VideoTimelineViewDelegate? = null
    var progress: Float = 0f

    init {

    }


    fun clicked(view: View) {
        mediaSelectClicked?.invoke()
    }
    fun settingsClicked(view: View) {
        val appContext = getApplication<Application>()
        val intent = Intent(appContext, SettingsActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        appContext.startActivity(intent)
    }
    fun nextClicked(view: View) {
        val appConext = getApplication<Application>()
        val intent = Intent(appConext, MapViewActivity::class.java)
        intent.putExtra("path", file)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        getApplication<Application>().startActivity(intent);

    }

    fun onMediaSelected(path: String?) {
        file = path
    }
}