package com.mag.slam3dvideo.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mag.slam3dvideo.R
import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.creation.GltfModelBuilder
import de.javagl.jgltf.model.impl.DefaultMeshModel
import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel
import de.javagl.jgltf.model.impl.DefaultNodeModel
import de.javagl.jgltf.model.impl.DefaultSceneModel
import de.javagl.jgltf.model.io.GltfModelWriter
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UIPanGestureRecognizer
import it.sephiroth.android.library.uigestures.UIRotateGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import java.io.File


class DebugActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        val v= findViewById<View>(R.id.view)
        val b = findViewById<Button>(R.id.btn)
        b.setOnClickListener {
            val gltfModelWriter = GltfModelWriter()

            val scene = DefaultSceneModel().apply {
                val mesh = DefaultMeshModel()
//                val defMeshPrimitive = DefaultMeshPrimitiveModel()
//                mesh.addMeshPrimitiveModel()
                val node = DefaultNodeModel()
//                node.addMeshModel(mesh)
                val nodePosition= FloatArray(16)
                android.opengl.Matrix.setIdentityM(nodePosition,0)
                android.opengl.Matrix.translateM(nodePosition,0,1f,2f,3f)
                node.matrix = nodePosition
                addNode(node)
            }
            val gltfModelBuilder = GltfModelBuilder.create()
            gltfModelBuilder.addSceneModel(scene)

            val gltfModel: GltfModel? = gltfModelBuilder.build()

            val outFile = File.createTempFile("awd","dawd")
            Log.d("DEBUG",outFile.absolutePath);
            gltfModelWriter.write(gltfModel,outFile)
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