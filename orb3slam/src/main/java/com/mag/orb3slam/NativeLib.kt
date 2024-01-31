package com.mag.orb3slam

class NativeLib {

    /**
     * A native method that is implemented by the 'orb3slam' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'orb3slam' library on application startup.
        init {
            System.loadLibrary("orb3slam")
        }
    }
}