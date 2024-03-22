package com.mag.slam3dvideo.utils.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import java.io.FileInputStream

class VideoFrameRetriever(videoPath: String) {
    var initialized: Boolean =false
        private set
    var durationMs: Long = 0
        private  set
    var capturedFps: Float = 0f
        private set
    var frameCount: Long = 0
        private set

    private val mediaMetadataRetriever = MediaMetadataRetriever()
    private var filePath: String = videoPath
    private var bitmapDecodeParams : BitmapParams
    init {
        bitmapDecodeParams = BitmapParams().apply {
            preferredConfig = Bitmap.Config.ARGB_8888
        }
    }
    fun initialize(): Boolean{
        val inputStream = FileInputStream(filePath)
        mediaMetadataRetriever.setDataSource(inputStream.fd)
        durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1
        capturedFps = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 0f
        frameCount = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLong() ?: -1
        initialized = true
        return true;
    }
    fun getFrame(frameNumber:Int): Bitmap? {
        if(!initialized)
            throw Exception("VideoFrameRetriever not initialized, please call initialize() first")
        if(frameNumber<0 || frameNumber >= frameCount)
            return null;
        return try {
            mediaMetadataRetriever.getFrameAtIndex(frameNumber,bitmapDecodeParams)
        }catch (ex:Exception){
            null
        }
    }
}