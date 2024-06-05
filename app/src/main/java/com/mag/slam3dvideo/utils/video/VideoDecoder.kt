package com.mag.slam3dvideo.utils.video

import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.mag.slam3dvideo.utils.MoviePlayer
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs


/**
 * The VideoDecoder class decodes video frames from a given source file and renders them on a specified Surface.
 */


/**
 * Creates a new VideoDecoder instance.
 *
 * @param sourceFile     The source file containing the video.
 * @param surface        The Surface on which to render the decoded video frames.
 * @param frameCallback  An optional callback to receive frame rendering events.
 */
class VideoDecoder(
    sourceFile: File,
    surface: Surface?,
    frameCallback: MoviePlayer.FrameCallback? = null
) {
    var mFrameTimeUs: Float
        private set
    var mVideoDuration: Long
        private set
    private val INVALID_SEEK_TIME = -1L
    private var isStarted: Boolean = false
    private var trackIndex: Int
    private val TIMEOUT_USEC = 10000L
    private var extractor: MediaExtractor
    private var decoder: MediaCodec
    private var mSourceFile = sourceFile
    private val TAG = "VideoDecoder"
    private val VERBOSE = false
    private var mVideoHeight: Int
    private var mVideoWidth: Int
    private var frameCallback: MoviePlayer.FrameCallback? =frameCallback

    private var decodeFrameQueue: BlockingQueue<Long> = LinkedBlockingQueue(999)
    private var seekTimeRequest: Long = INVALID_SEEK_TIME

    /**
     * Initialize MediaExtractor
     * Retrieve video info
     */
    init {
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(sourceFile.toString())
            trackIndex = findVideoTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found in $mSourceFile")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            mVideoDuration = format.getLong(MediaFormat.KEY_DURATION)
            mFrameTimeUs = 1000000f / format.getInteger(MediaFormat.KEY_FRAME_RATE)
            if (VERBOSE) {
                Log.d(TAG, "Video size is " + mVideoWidth + "x" + mVideoHeight)
            }
            val mime = format.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime!!)
            val f = getImageFormatFromCodecType(mime);
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            );

            decoder.configure(format, surface, null, 0)

        } finally {
//            extractor?.release()
        }
    }

    private fun getImageFormatFromCodecType(mimeType: String): Int {
        // TODO: Need pick a codec first, then get the codec info, will revisit for future.
        val codecInfo: MediaCodecInfo = getCodecInfoByType(mimeType)!!
        val colorFormat: Int = selectDecoderOutputColorFormat(codecInfo, mimeType)
        return colorFormat
    }

    private fun getCodecInfoByType(mimeType: String): MediaCodecInfo? {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return null
    }

    private fun selectDecoderOutputColorFormat(codecInfo: MediaCodecInfo, mimeType: String): Int {
        val capabilities = codecInfo.getCapabilitiesForType(mimeType)
        for (i in capabilities.colorFormats.indices) {
            val colorFormat = capabilities.colorFormats[i]
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat
            }
        }
        throw RuntimeException("couldn't find a good color format for " + codecInfo.name + " / " + mimeType)
    }

    private fun isRecognizedFormat(colorFormat: Int): Boolean {
        if (VERBOSE) Log.v(TAG, "colorformat: $colorFormat")
        return when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar,
            ImageFormat.YUV_420_888 -> true

            else -> false
        }
    }

    fun start() {
        if (isStarted)
            return;
        decoder.start()
        Thread {
            try {
                isStarted = true
                run(extractor, trackIndex, decoder)
            } finally {
                isStarted = false
            }
        }.apply {
            start()
        }
    }

    fun decodeAtTime(timeUs: Long) {
//        decodeFrameQueue.offer(timeUs)
    }

    private fun run(extractor: MediaExtractor, trackIndex: Int, decoder: MediaCodec) {
        var mIsStopRequested = false

        var inputChunk = 0
        var firstInputTimeNsec: Long = -1
        var outputDone = false
        var inputDone = false
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop")
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested")
                return
            }

            if(seekTimeRequest >= 0){
                val timeToSeek = seekTimeRequest
                seekTimeRequest = INVALID_SEEK_TIME
                if(timeToSeek == 0L)
                {
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    decoder.flush()
                }else
                {
                    val nearestTime = getNearestTime(timeToSeek,extractor)
                    advanceTill(nearestTime,extractor)

                }
            }


            // Feed more data to the decoder.
            if (!inputDone) {
                val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (inputBufIndex >= 0) {
                    if (firstInputTimeNsec == -1L) {
                        firstInputTimeNsec = System.nanoTime()
                    }
                    val inputBuf = decoder.getInputBuffer(inputBufIndex)
                    val chunkSize = extractor.readSampleData(inputBuf!!, 0)
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                        if (VERBOSE) Log.d(TAG, "sent input EOS")
                    } else {
                        if (extractor.sampleTrackIndex != trackIndex) {
                            Log.w(
                                TAG, "WEIRD: got sample from track " +
                                        extractor.sampleTrackIndex + ", expected " + trackIndex
                            )
                        }
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, chunkSize,
                            presentationTimeUs, 0 /*flags*/
                        )
                        if (VERBOSE) {
                            Log.d(
                                TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                        chunkSize
                            )
                        }
                        inputChunk++
                        extractor.advance()
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available")
                }
            }
            if (!outputDone) {
                val bufferInfo = MediaCodec.BufferInfo()
                val decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: $newFormat")
                } else if (decoderStatus < 0) {
                    throw RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus
                    )
                } else { // decoderStatus >= 0
                    if (firstInputTimeNsec != 0L) {
                        // Log the delay from the first buffer of input to the first buffer
                        // of output.
                        val nowNsec = System.nanoTime()
                        Log.d(
                            TAG,
                            "startup lag " + (nowNsec - firstInputTimeNsec) / 1000000.0 + " ms"
                        )
                        firstInputTimeNsec = 0
                    }
                    var doLoop = false
                    if (VERBOSE) Log.d(
                        TAG, "surface decoder given buffer " + decoderStatus +
                                " (size=" + bufferInfo.size + ")"
                    )
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS")
                        if (true) {
                            doLoop = true
                        } else {
                            outputDone = true
                        }
                    }
                    val doRender = bufferInfo.size != 0
                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  We can't control when it
                    // appears on-screen, but we can manage the pace at which we release
                    // the buffers.
                    if (doRender) {
                        frameCallback?.preRender(bufferInfo.presentationTimeUs)
                    }
                    //  val image = decoder.getOutputImage(decoderStatus)
                    //   image?.close()
                    decoder.releaseOutputBuffer(decoderStatus, doRender)
                    if (doRender) {
                        frameCallback?.postRender()
                    }
                    if (doLoop) {
                        Log.d(TAG, "Reached EOS, looping")
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        inputDone = false
                        decoder.flush() // reset decoder state
                        frameCallback?.loopReset()
                    }
                }
            }
        }

    }

    private fun advanceTill(nearestTime: Long, extractor: MediaExtractor) {
        var shouldSeekMore = true
        var outputDone = false
        var inputDone = false
        while (shouldSeekMore) {
            // Feed more data to the decoder.
            if (!inputDone) {
                val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (inputBufIndex >= 0) {
                    val inputBuf = decoder.getInputBuffer(inputBufIndex)
                    val chunkSize = extractor.readSampleData(inputBuf!!, 0)
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                        if (VERBOSE) Log.d(TAG, "sent input EOS")
                    } else {
                        if (extractor.sampleTrackIndex != trackIndex) {
                            Log.w(
                                TAG, "WEIRD: got sample from track " +
                                        extractor.sampleTrackIndex + ", expected " + trackIndex
                            )
                        }
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, chunkSize,
                            presentationTimeUs, 0 /*flags*/
                        )
                        extractor.advance()
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available")
                }
            }
            if (!outputDone) {
                val bufferInfo = MediaCodec.BufferInfo()
                val decoderStatus =
                    decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: $newFormat")
                } else if (decoderStatus < 0) {
                    throw RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus
                    )
                } else { // decoderStatus >= 0
                    var doLoop = false
                    if (VERBOSE) Log.d(
                        TAG, "surface decoder given buffer " + decoderStatus +
                                " (size=" + bufferInfo.size + ")"
                    )
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS")
                        if (true) {
                            doLoop = true
                        } else {
                            outputDone = true
                        }
                    }
                    val isThatTime = bufferInfo.presentationTimeUs == nearestTime
                    if (isThatTime) {
                        Log.d("DECOER", "This time $nearestTime")
                    }
                    shouldSeekMore = bufferInfo.presentationTimeUs < nearestTime && !isThatTime
                    decoder.releaseOutputBuffer(decoderStatus, false)
                    if (doLoop) {
                        Log.d(TAG, "Reached EOS, looping")
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        decoder.flush() // reset decoder state
                        frameCallback?.loopReset()
                    }
                }
            }
        }
        decoder.flush()
    }

    private fun runWithSeek(extractor: MediaExtractor, trackIndex: Int, decoder: MediaCodec) {
        var mIsStopRequested = false
        var inputChunk = 0
        var firstInputTimeNsec: Long = -1
        var outputDone = false
        var inputDone = false
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop")
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested")
                return
            }
            val frameTimeUs = decodeFrameQueue.take()
            if (frameTimeUs > mVideoDuration || frameTimeUs < 0)
                continue
            val nearestTime = getNearestTime(frameTimeUs, extractor)
            extractor.seekTo(nearestTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var shouldSeekMore = true
            while (shouldSeekMore) {
                // Feed more data to the decoder.
                if (!inputDone) {
                    val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                    if (inputBufIndex >= 0) {
                        if (firstInputTimeNsec == -1L) {
                            firstInputTimeNsec = System.nanoTime()
                        }
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        val chunkSize = extractor.readSampleData(inputBuf!!, 0)
                        if (chunkSize < 0) {
                            // End of stream -- send empty frame with EOS flag set.
                            decoder.queueInputBuffer(
                                inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                            if (VERBOSE) Log.d(TAG, "sent input EOS")
                        } else {
                            if (extractor.sampleTrackIndex != trackIndex) {
                                Log.w(
                                    TAG, "WEIRD: got sample from track " +
                                            extractor.sampleTrackIndex + ", expected " + trackIndex
                                )
                            }
                            val presentationTimeUs = extractor.sampleTime
                            decoder.queueInputBuffer(
                                inputBufIndex, 0, chunkSize,
                                presentationTimeUs, 0 /*flags*/
                            )
                            if (VERBOSE) {
                                Log.d(
                                    TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                            chunkSize
                                )
                            }
                            inputChunk++
                            extractor.advance()
                        }
                    } else {
                        if (VERBOSE) Log.d(TAG, "input buffer not available")
                    }
                }
                if (!outputDone) {
                    val bufferInfo = MediaCodec.BufferInfo()
                    val decoderStatus =
                        decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) Log.d(TAG, "no output from decoder available")
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not important for us, since we're using Surface
                        if (VERBOSE) Log.d(TAG, "decoder output buffers changed")
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = decoder.outputFormat
                        if (VERBOSE) Log.d(TAG, "decoder output format changed: $newFormat")
                    } else if (decoderStatus < 0) {
                        throw RuntimeException(
                            "unexpected result from decoder.dequeueOutputBuffer: " +
                                    decoderStatus
                        )
                    } else { // decoderStatus >= 0
                        if (firstInputTimeNsec != 0L) {
                            // Log the delay from the first buffer of input to the first buffer
                            // of output.
                            val nowNsec = System.nanoTime()
                            Log.d(
                                TAG,
                                "startup lag " + (nowNsec - firstInputTimeNsec) / 1000000.0 + " ms"
                            )
                            firstInputTimeNsec = 0
                        }
                        var doLoop = false
                        if (VERBOSE) Log.d(
                            TAG, "surface decoder given buffer " + decoderStatus +
                                    " (size=" + bufferInfo.size + ")"
                        )
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS")
                            if (true) {
                                doLoop = true
                            } else {
                                outputDone = true
                            }
                        }
                        val isThatTime = bufferInfo.presentationTimeUs == nearestTime
                        if (isThatTime) {
                            Log.d("DECOER", "This time $nearestTime")
                        }
                        shouldSeekMore =
                            bufferInfo.presentationTimeUs <= (nearestTime + mFrameTimeUs * 2) && !isThatTime
                        val doRender = bufferInfo.size != 0 && isThatTime
                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  We can't control when it
                        // appears on-screen, but we can manage the pace at which we release
                        // the buffers.
                        if (doRender) {
                            frameCallback?.preRender(bufferInfo.presentationTimeUs)
                        }
                        //  val image = decoder.getOutputImage(decoderStatus)
                        //   image?.close()
                        decoder.releaseOutputBuffer(decoderStatus, doRender)
                        if (doRender) {
                            frameCallback?.postRender()
                        }
                        if (doLoop) {
                            Log.d(TAG, "Reached EOS, looping")
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            inputDone = false
                            decoder.flush() // reset decoder state
                            frameCallback?.loopReset()
                        }
                    }
                }
            }
            //   decoder.flush()
        }
    }

    private fun Long.minDifferenceValue(a: Long, b: Long): Long {
        if (a == b) {
            return Math.min(a, this)
        }
        val diffA = abs(a - this)
        val diffB = abs(b - this)
        if (diffA == diffB) {
            return Math.min(a, b)
        }
        return if (diffA < diffB) a else b
    }

    private fun getNearestTime(time: Long, extractor: MediaExtractor): Long {
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var sampleTime = extractor.sampleTime
        val topTime = time + 2000000
        var isFind = false
        while (!isFind) {
            extractor.advance()
            val s = extractor.sampleTime
            if (s != -1L) {
                sampleTime = time.minDifferenceValue(sampleTime, s)
                isFind = s >= topTime
            } else {
                isFind = true
            }
        }
        return sampleTime
    }


    private fun findVideoTrack(extractor: MediaExtractor): Int {
        // Select the first video track we find, ignore the rest.
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime!!.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track $i ($mime): $format")
                }
                return i
            }
        }
        return -1
    }

    fun seekTo(timeUsec: Long) {
        seekTimeRequest = timeUsec
    }
}