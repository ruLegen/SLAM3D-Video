package com.mag.slam3dvideo.utils

import android.util.Log
import android.util.Size
import com.mag.slam3dvideo.orb3.TrackingState
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.ArrayList
import kotlin.time.Duration
import kotlin.time.measureTimedValue

data class OrbFrameResult(val state: TrackingState)
@Serializable
data class OrbFrameMeasurement(val duration: Duration, val state: TrackingState)
@Serializable
data class OrbJsonStruct(val settings: OrbSlamSettings,val frameCount: Int,val fps:Float,val measurements:Array<OrbFrameMeasurement>)

class OrbMetricMeasurer {
    private val frameMeasurements = ArrayList<OrbFrameMeasurement>()
    private var settings:OrbSlamSettings? = null
    private var videoFrameCount:Int = 0
    private var videoFps:Float = 0f
    fun trackVideoInfo(orbSlamSettings: OrbSlamSettings,frameCount:Int,fps:Float){
        settings = orbSlamSettings
        videoFrameCount = frameCount
        videoFps = fps
    }
    fun measureProcessFrame(action:()->OrbFrameResult){
        val (orbResult,elapsed) = measureTimedValue{
            val state = action()
            state
        }
        frameMeasurements.add(OrbFrameMeasurement(elapsed, orbResult.state))
    }

    fun dumpToFile(outFile:File):Boolean{
        if(settings == null || frameMeasurements == null)
            return false
        val json = Json.encodeToString(OrbJsonStruct.serializer(),OrbJsonStruct(settings!!, videoFrameCount,videoFps,frameMeasurements.toTypedArray()))
        outFile.writeText(json)
        return true
    }
}