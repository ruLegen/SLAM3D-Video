package com.mag.slam3dvideo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import com.mag.slam3dvideo.R
import com.mag.slam3dvideo.orb3.ORB3
import com.mag.slam3dvideo.utils.AssetUtils


data class AssetFiles(val vocabFile:String, val configFile:String)

class MainActivity : AppCompatActivity() {
    private val TAG: String? = "awdawd"
    private lateinit var mSurfaveView : SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSurfaveView = findViewById(R.id.surfaceView);

      //  val files = saveAssetsToLocalFiles()

      //  val orb = ORB3(files.vocabFile,files.configFile);
      //  Log.d(TAG,orb.ptr.toString())
    }

    fun saveAssetsToLocalFiles() : AssetFiles {
        val vocabAssetName = "Vocabulary/ORBvoc.txt"
        val cameraParamsAssetName = "Calibration/PARAconfig.yaml"
        val assets = getAssets();
        val vocabInputStream = assets.open(vocabAssetName)
        val calibInputStream = assets.open(cameraParamsAssetName)
        val baseDir = filesDir.toString()
        val outVocab = baseDir + "/voc.txt.tar"
        val outConfig = baseDir + "/config.yaml"
        AssetUtils.createFileFromInputStream(filesDir.toString() + "/voc.txt.tar",vocabInputStream)
        AssetUtils.createFileFromInputStream(filesDir.toString() + "/config.yaml",calibInputStream)

        return AssetFiles(outVocab,outConfig)
    }

}