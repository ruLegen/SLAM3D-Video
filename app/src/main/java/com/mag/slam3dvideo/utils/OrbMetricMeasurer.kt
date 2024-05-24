package com.mag.slam3dvideo.utils

import com.charleskorn.kaml.Yaml
import com.mag.slam3dvideo.orb3.TrackingState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception
import java.util.ArrayList
import kotlin.time.Duration
import kotlin.time.measureTimedValue
object OrbSlamSettingsAsStringSerializer : KSerializer<OrbSlamSettings> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("settings", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: OrbSlamSettings) {
        val string = Yaml.default.encodeToString(OrbSlamSettings.serializer(),value)
        encoder.encodeString(string)
    }
    override fun deserialize(decoder: Decoder): OrbSlamSettings {
        val string = decoder.decodeString()
        return try {
            Yaml.default.decodeFromString(OrbSlamSettings.serializer(), string)
        } catch (ex: Exception) {
            OrbSlamSettings()
        }
    }
}data class OrbFrameResult(val state: TrackingState)
@Serializable
data class OrbFrameMeasurement(val duration: Duration, val state: TrackingState)
@Serializable
data class OrbJsonStruct(@Serializable(with = OrbSlamSettingsAsStringSerializer::class) val settings: OrbSlamSettings,
                         val frameCount: Int,
                         val fps:Float,
                         val measurements:Array<OrbFrameMeasurement>)

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