package com.mag.slam3dvideo.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mag.slam3dvideo.R
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UIPanGestureRecognizer
import it.sephiroth.android.library.uigestures.UIRotateGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import java.io.InputStreamReader


class DebugActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        val v= findViewById<View>(R.id.view)
        val b = findViewById<Button>(R.id.btn)
        b.setOnClickListener {
            val input = InputStreamReader(assets.open("Models/camera.obj"))
            val obj: Obj = ObjReader.read(input)
            val vert = ObjData.getVertices(obj);
            val faceVertexIndices = ObjData.getFaceVertexIndicesArray(obj);
            val faceTexCoordIndices = ObjData.getFaceTexCoordIndicesArray(obj);
            val faceNormalIndices = ObjData.getFaceNormalIndicesArray(obj);
        }
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