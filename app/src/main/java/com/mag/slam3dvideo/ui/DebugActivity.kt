package com.mag.slam3dvideo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mag.slam3dvideo.R
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UIPanGestureRecognizer
import it.sephiroth.android.library.uigestures.UIRotateGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate


class DebugActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        val v= findViewById<View>(R.id.view)
        val delegate= UIGestureRecognizerDelegate();

        val recognizer2= UIRotateGestureRecognizer(this)
        val recognizer3 = UIPanGestureRecognizer(this)
        recognizer3.requireFailureOf=recognizer2

        var angle = 0.0
        recognizer2.actionListener = {
            val rot = it as UIRotateGestureRecognizer
            if(rot.state == UIGestureRecognizer.State.Began)
                angle = 0.0
            angle += rot.rotationInDegrees
            rot.currentLocationX
            Log.d("rotate",angle.toString())
        }
        recognizer3.actionListener = {
            val pan = it as UIPanGestureRecognizer
            Log.d("rotate","(${pan.relativeScrollX};${pan.relativeScrollY})")
        }

        delegate.addGestureRecognizer(recognizer2)
        delegate.addGestureRecognizer(recognizer3)

        v.setGestureDelegate(delegate)

    }
}