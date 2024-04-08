package com.mag.slam3dvideo

import android.app.Application
import com.mag.slam3dvideo.resources.AssetMeshes

class SlamVideoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AssetMeshes.init(applicationContext)
    }
}