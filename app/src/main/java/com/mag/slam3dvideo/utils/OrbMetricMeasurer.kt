package com.mag.slam3dvideo.utils

import android.util.Size
import com.charleskorn.kaml.Yaml
import com.mag.slam3dvideo.orb3.TrackingState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception
import java.util.ArrayList
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Custom JSON settings serializer
 * It created in order to workaround YAML serialization tags in @see OrbSlamSettins
 *
 * @constructor Create empty Orb slam settings as string serializer
 */
object OrbSlamSettingsAsStringSerializer : KSerializer<OrbSlamSettings> {
    @Serializable
    data class OrbSlamSettingsJson(
        val width: Int,
        val height: Int,
        val orbFeatureCount: Int,
        val imageScale: Double,
        val fx: Double,
        val fy: Double,
        val cx: Double,
        val cy: Double,
        val k1: Double,
        val k2: Double
    )

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("settings", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OrbSlamSettings) {
        encoder.encodeSerializableValue(OrbSlamSettingsJson.serializer(),OrbSlamSettingsJson(
            value.newWidth,
            value.newHeight,
            value.nFeatures,
            value.imageScale,
            value.fx,
            value.fy,
            value.cx,
            value.cy,
            value.k1,
            value.k2,
        ))
    }

    override fun deserialize(decoder: Decoder): OrbSlamSettings {
        val string = decoder.decodeString()
        return try {
            Yaml.default.decodeFromString(OrbSlamSettings.serializer(), string)
        } catch (ex: Exception) {
            OrbSlamSettings()
        }
    }
}

data class OrbFrameResult(val state: TrackingState)

@Serializable
data class OrbFrameMeasurement(val duration: Duration, val state: TrackingState)

@Serializable
data class OrbJsonStruct(
    @Serializable(with = OrbSlamSettingsAsStringSerializer::class) val settings: OrbSlamSettings,
    val frameCount: Int,
    val fps: Float,
    val measurements: Array<OrbFrameMeasurement>
)
/**
 * The OrbMetricMeasurer class is responsible for tracking and measuring ORB_SLAM3
 * frame processing metrics and dumping the results to a file.
 */
class OrbMetricMeasurer {
    private val frameMeasurements = ArrayList<OrbFrameMeasurement>()
    private var settings: OrbSlamSettings? = null
    private var videoFrameCount: Int = 0
    private var videoFps: Float = 0f

    /**
     * Tracks video information including ORB SLAM settings, frame count, and frames per second (FPS).
     *
     * @param orbSlamSettings The settings for ORB SLAM.
     * @param frameCount The total number of video frames.
     * @param fps The frames per second of the video.
     */

    fun trackVideoInfo(orbSlamSettings: OrbSlamSettings, frameCount: Int, fps: Float) {
        settings = orbSlamSettings
        videoFrameCount = frameCount
        videoFps = fps
    }

    /**
     * Measures the time taken to process a frame and stores the result.
     *
     * @param action The action representing the frame processing which returns an OrbFrameResult.
     */
    fun measureProcessFrame(action: () -> OrbFrameResult) {
        val (orbResult, elapsed) = measureTimedValue {
            val state = action()
            state
        }
        frameMeasurements.add(OrbFrameMeasurement(elapsed, orbResult.state))
    }

    /**
     * Dumps the collected frame measurements and video information to a specified file in JSON format.
     *
     * @param outFile The file where the JSON data should be written.
     * @return `true` if the data was successfully written to the file, `false` otherwise.
     */
    fun dumpToFile(outFile: File): Boolean {
        if (settings == null || frameMeasurements == null)
            return false
        val json = Json.encodeToString(
            OrbJsonStruct.serializer(),
            OrbJsonStruct(settings!!, videoFrameCount, videoFps, frameMeasurements.toTypedArray())
        )
        outFile.writeText(json)
        return true
    }
}