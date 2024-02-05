package com.mag.slam3dvideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    init {
        System.loadLibrary("orbvideoslam")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nativeTest();
    }

    external fun nativeTest();
}