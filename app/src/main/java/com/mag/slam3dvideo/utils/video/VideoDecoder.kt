package com.mag.slam3dvideo.utils.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import java.io.File
import java.nio.ByteBuffer

class VideoDecoder(sourceFile: File, surface: Surface) {
    private var trackIndex: Int
    private val TIMEOUT_USEC = 10000L
    private var extractor: MediaExtractor
    private var decoder: MediaCodec
    private var mSourceFile = sourceFile
    private val TAG = "VideoDecoder"
    private val VERBOSE = false
    private var mVideoHeight: Int
    private var mVideoWidth: Int

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
            if (VERBOSE) {
                Log.d(TAG, "Video size is " + mVideoWidth + "x" + mVideoHeight)
            }
            val mime = format.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime!!)
            decoder.configure(format, surface, null, 0)
            decoder.start()

        } finally {
//            extractor?.release()
        }
    }
    fun start(){
        Thread {
            run(extractor, trackIndex, decoder)
        }.apply {
            start()
        }
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
//                    if (doRender && frameCallback != null) {
//                        frameCallback.preRender(mBufferInfo.presentationTimeUs)
//                    }
                    decoder.releaseOutputBuffer(decoderStatus, doRender)
//                    if (doRender && frameCallback != null) {
//                        frameCallback.postRender()
//                    }
                    if (doLoop) {
                        Log.d(TAG, "Reached EOS, looping")
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        inputDone = false
                        decoder.flush() // reset decoder state
//                        frameCallback!!.loopReset()
                    }
                }
            }
        }

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
}