package com.mag.slam3dvideo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.orb3.ORB3
import com.mag.slam3dvideo.utils.AssetUtils


data class AssetFiles(val vocabFile: String, val configFile: String)

class MainActivity : AppCompatActivity() {
    private val TAG: String? = "awdawd"
    private lateinit var mSurfaveView: SurfaceView
    private lateinit var mButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSurfaveView = findViewById(R.id.surfaceView);
        mButton = findViewById(R.id.save)
        mButton.setOnClickListener {
            val files = saveAssetsToLocalFiles()
            val orb = ORB3(files.vocabFile, files.configFile);
            Toast.makeText(this@MainActivity,"read ok",Toast.LENGTH_LONG).show()
        }
        //  Log.d(TAG,orb.ptr.toString())
    }

    fun saveAssetsToLocalFiles(): AssetFiles {
        val vocabAssetName = "Vocabulary/ORBvoc.bin"
        val cameraParamsAssetName = "Calibration/PARAconfig.yaml"
        val assets = getAssets();
        val vocabInputStream = assets.open(vocabAssetName)
        val calibInputStream = assets.open(cameraParamsAssetName)
        val baseDir = filesDir.toString()
        val outVocab = baseDir + "/voc.bin"
        val outConfig = baseDir + "/config.yaml"
        AssetUtils.createFileFromInputStream(filesDir.toString() + "/voc.bin", vocabInputStream)
        AssetUtils.createFileFromInputStream(filesDir.toString() + "/config.yaml", calibInputStream)

        return AssetFiles(outVocab, outConfig)
    }

}