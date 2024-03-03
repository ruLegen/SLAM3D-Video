package com.mag.slam3dvideo.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and
import kotlin.io.path.Path
import kotlin.io.path.exists


class AssetUtils {
    companion object{
        private val TAG :String="AssetUtils"
        var storage_permissions = arrayOf<String>(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        var storage_permissions_33 = arrayOf<String>(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        fun permissions(): Array<String> {
            val p: Array<String>
            p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storage_permissions_33
            } else {
                storage_permissions
            }
            return p
        }

        fun hasAllMediaPermissions(activity:Activity):Boolean{
            return permissions().all {
                ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }

        }
        /**
         * @return false if request didn't happened
         */
        fun askMediaPermissions(activity: Activity, code:Int): Boolean{
            val permissions = permissions()
            if(hasAllMediaPermissions(activity))
                return  false;
            ActivityCompat.requestPermissions(activity,permissions(),code);
            return  true
        }
        fun fileToMD5(inputStream: InputStream): String? {
            return try {
                val buffer = ByteArray(10240)
                val digest: MessageDigest = MessageDigest.getInstance("MD5")
                var numRead = 0
                while (numRead != -1) {
                    numRead = inputStream!!.read(buffer)
                    if (numRead > 0) digest.update(buffer, 0, numRead)
                }
                val md5Bytes: ByteArray = digest.digest()
                convertHashToString(md5Bytes)
            } catch (e: java.lang.Exception) {
                null
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: java.lang.Exception) {
                    }
                }
            }
        }
        fun fileToMD5(path: String): String? {
            var inputStream = FileInputStream(path)
            val md5 = fileToMD5(inputStream)
            inputStream.close();
            return md5
        }
        private fun convertHashToString(md5Bytes: ByteArray): String? {
            var returnVal = ""
            for (i in md5Bytes.indices) {
                returnVal += Integer.toString((md5Bytes[i] and 0xff.toByte()) + 0x100, 16).substring(1)
            }
            return returnVal.uppercase(Locale.getDefault())
        }


        fun createFileFromInputStream(fileName:String,inputStream: InputStream): File? {
            try {
                if(Path(fileName).exists()){
                    val oldFile = File(fileName)
                    val newFileMd5 = fileToMD5(fileName)
                    val oldFileMd5 = fileToMD5(inputStream)
                    if(newFileMd5 == oldFileMd5)
                        return oldFile;
                    else
                        oldFile.delete()
                    inputStream.reset()
                }
                val f = File(fileName).apply {
                    createNewFile()
                }
                val outputStream: OutputStream = FileOutputStream(f)
                val buffer = ByteArray(8192)
                var length = 0
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                outputStream.close()
                inputStream.close()
                return f
            } catch (e: Exception) {
                Log.d(TAG,e.message!!);
            }
            return null
        }
    }
}