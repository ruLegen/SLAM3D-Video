package com.mag.slam3dvideo.utils

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.os.Handler
import android.util.Log
import android.view.Surface
import com.mag.slam3dvideo.utils.gles.EglCore
import com.mag.slam3dvideo.utils.gles.FullFrameRect
import com.mag.slam3dvideo.utils.gles.OffscreenSurface
import com.mag.slam3dvideo.utils.gles.Texture2dProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class TextureSurface(val width: Int, val height: Int, val handler: Handler,val onBufferAvailable:((buffer:ByteBuffer,presentationFrame:Long)->Unit)? = null) : OnFrameAvailableListener {

    var frameCount: Long = 0;
    var totalDurationUsec:Long = 0
    private var pixelBuf: ByteBuffer
    lateinit var mSurface: Surface
    lateinit var mTextureSurface: SurfaceTexture
    private lateinit var mDisplaySurface: OffscreenSurface
    private var mTextureId by Delegates.notNull<Int>()
    private lateinit var mFullFrameBlit: FullFrameRect
    private lateinit var mEglCore: EglCore
    private lateinit var mTexture2dProgram: Texture2dProgram
    private var taskRunner: TaskRunner = TaskRunner()
    init {
            mEglCore = EglCore(null, EglCore.FLAG_RECORDABLE)
            mDisplaySurface = OffscreenSurface(mEglCore, width, height)
            mDisplaySurface.makeCurrent()

            mTexture2dProgram = Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT)

            mFullFrameBlit = FullFrameRect(mTexture2dProgram)
            mTextureId = mFullFrameBlit.createTextureObject()
            mTextureSurface = SurfaceTexture(mTextureId)
            mTextureSurface.setOnFrameAvailableListener(this)
            mSurface = Surface(mTextureSurface)
            pixelBuf = ByteBuffer.allocateDirect(width * height * 4)
            pixelBuf.order(ByteOrder.nativeOrder())
    }

    val matrix = FloatArray(16)
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        handler.post{
            mDisplaySurface.makeCurrent()
            mTextureSurface.updateTexImage()

            var timestamp =  surfaceTexture?.timestamp?:0L
            timestamp /= 1000    // to microseconds
            Log.d("TexSurface","onFrameAvailable $timestamp")
            mTextureSurface.getTransformMatrix(matrix)
            GLES20.glViewport(0, 0, width, height);
            mFullFrameBlit.drawFrame(mTextureId, matrix)
            mDisplaySurface.swapBuffers()
            GLES20.glFinish();
            GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE, pixelBuf)
            onBufferAvailable?.invoke(pixelBuf,timestamp)
        }
    }
}
