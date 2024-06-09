package com.mag.slam3dvideo.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class PreferenceHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    fun getOrbSlamSettings(): OrbSlamSettings {
        return OrbSlamSettings().apply {
            fx = preferences.getString("camera1_fx", fx.toString())?.toDouble() ?: fx
            fy = preferences.getString("camera1_fy", fy.toString())?.toDouble() ?: fy
            cx = preferences.getString("camera1_cx", cx.toString())?.toDouble() ?: cx
            cy = preferences.getString("camera1_cy", cy.toString())?.toDouble() ?: cy
            k1 = preferences.getString("camera1_k1", k1.toString())?.toDouble() ?: k1
            k2 = preferences.getString("camera1_k2", k2.toString())?.toDouble() ?: k2
            p1 = preferences.getString("camera1_p1", p1.toString())?.toDouble() ?: p1
            p2 = preferences.getString("camera1_p2", p2.toString())?.toDouble() ?: p2
            fps = preferences.getString("camera_fps", fps.toString())?.toInt() ?: fps
            width = preferences.getString("camera_width", width.toString())?.toInt() ?: width
            height = preferences.getString("camera_height", height.toString())?.toInt() ?: height
            newWidth = preferences.getString("camera_new_width", newWidth.toString())?.toInt() ?: newWidth
            newHeight = preferences.getString("camera_new_height", newHeight.toString())?.toInt() ?: newHeight
            imageScale = preferences.getString("camera_image_scale", imageScale.toString())?.toDouble() ?: imageScale
            RGB = preferences.getString("camera_rgb", RGB.toString())?.toInt() ?: RGB
            nFeatures = preferences.getString("orbextractor_n_features", nFeatures.toString())?.toInt() ?: nFeatures
            scaleFactor = preferences.getString("orbextractor_scale_factor", scaleFactor.toString())?.toDouble() ?: scaleFactor
            nLevels = preferences.getString("orbextractor_n_levels", nLevels.toString())?.toInt() ?: nLevels
            iniThFAST = preferences.getString("orbextractor_ini_th_fast", iniThFAST.toString())?.toInt() ?: iniThFAST
            minThFAST = preferences.getString("orbextractor_min_th_fast", minThFAST.toString())?.toInt() ?: minThFAST
        }
    }

    fun saveOrbSlamSettings(settings: OrbSlamSettings) {
        preferences.edit {
            putString("camera1_fx", settings.fx.toString())
            putString("camera1_fy", settings.fy.toString())
            putString("camera1_cx", settings.cx.toString())
            putString("camera1_cy", settings.cy.toString())
            putString("camera1_k1", settings.k1.toString())
            putString("camera1_k2", settings.k2.toString())
            putString("camera1_p1", settings.p1.toString())
            putString("camera1_p2", settings.p2.toString())
            putString("camera_fps", settings.fps.toString())
            putString("camera_width", settings.width.toString())
            putString("camera_height", settings.height.toString())
            putString("camera_new_width", settings.newWidth.toString())
            putString("camera_new_height", settings.newHeight.toString())
            putString("camera_image_scale", settings.imageScale.toString())
            putString("camera_rgb", settings.RGB.toString())
            putString("orbextractor_n_features", settings.nFeatures.toString())
            putString("orbextractor_scale_factor", settings.scaleFactor.toString())
            putString("orbextractor_n_levels", settings.nLevels.toString())
            putString("orbextractor_ini_th_fast", settings.iniThFAST.toString())
            putString("orbextractor_min_th_fast", settings.minThFAST.toString())
        }
    }
}