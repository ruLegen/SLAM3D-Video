package com.mag.slam3dvideo.videmodels

import android.app.Application
import android.view.View
import android.widget.Button
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import com.mag.slam3dvideo.BR

class VideoViewerViewModel(application: Application) : ObservableViewModel(application){
    @get:Bindable
    var test: Int = 10
        set(value) {
            field = value
            notifyChange(BR.test)
        }



    fun clicked(view:View){
        test++
    }
}