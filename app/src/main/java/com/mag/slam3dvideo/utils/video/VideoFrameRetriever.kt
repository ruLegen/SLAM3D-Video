package com.mag.slam3dvideo.utils.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import java.io.FileInputStream

public class VideoFrameRetriever(videoPath: String) {
    var initialized: Boolean =false
        private set
    private val mediaMetadataRetriever = MediaMetadataRetriever()
    private var filePath: String = videoPath
    private var durationMs: Long = 0
    private var capturedFps: Float = 0f
    private var frameCount: Long = 0
    private var bitmapDecodeParams : BitmapParams

    init {
        bitmapDecodeParams = BitmapParams().apply {
            preferredConfig = Bitmap.Config.ARGB_8888
        }
    }
    fun initialize(): Boolean{
        return try {
            val inputStream = FileInputStream(filePath)
            mediaMetadataRetriever.setDataSource(inputStream.fd)
            durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1
            capturedFps = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 0f
            frameCount = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLong() ?: -1
            initialized = true
            true;
        }catch (ex: Exception) {
            false
        }
    }
    fun getFrame(frameNumber:Int): Bitmap? {
        if(frameNumber<0 || frameNumber >= frameCount || !initialized)
            return null;
        return try {
            mediaMetadataRetriever.getFrameAtIndex(frameNumber,bitmapDecodeParams)
        }catch (ex:Exception){
            null
        }
    }
}