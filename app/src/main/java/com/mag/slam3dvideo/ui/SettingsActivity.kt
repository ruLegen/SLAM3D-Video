package com.mag.slam3dvideo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.charleskorn.kaml.Yaml
import com.mag.slam3dvideo.utils.OrbSlamSettings
import com.mag.slam3dvideo.utils.PreferenceHelper
import kotlinx.serialization.decodeFromString
import java.lang.Exception


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.mag.slam3dvideo.R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(com.mag.slam3dvideo.R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var rootKey:String? = null
        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            rootKey = key
            setPreferencesFromResource(com.mag.slam3dvideo.R.xml.root_preferences, rootKey)

            val resetButton: Preference? = findPreference("reset_btn")
            resetButton?.setOnPreferenceClickListener {
                reset()
                return@setOnPreferenceClickListener true
            };

            val exportButton: Preference? = findPreference("export_btn")
            exportButton?.setOnPreferenceClickListener {
                export()
                return@setOnPreferenceClickListener true
            };

            val importButton : Preference?= findPreference("import_btn")
            importButton?.setOnPreferenceClickListener {
                import()
                return@setOnPreferenceClickListener true
            }
        }

        private fun reset() {
           setORB_SLAM_Settings(OrbSlamSettings())
        }

        private fun import(){
            val yaml = "READ FROM FILE"
            // REMOVE %YAML:1.0

            try {
                val serialized = Yaml.default.decodeFromString<OrbSlamSettings>(yaml)
                setORB_SLAM_Settings(serialized)
            }catch (e:Exception){

            }
        }
        private fun export(){
            val preferenceHelper = PreferenceHelper(requireContext())
            val settings = preferenceHelper.getOrbSlamSettings()
            val serialized = Yaml.default.encodeToString(OrbSlamSettings.serializer(),settings)
            val result  = "%YAML:1.0\n".plus(serialized)
            // ask user location
//            val file = File.createTempFile("setting_",".yaml")
//            file.writeText(result)
//            val filePath = file.absolutePath
//            file.delete()
        }
        private fun setORB_SLAM_Settings(slamSettings: OrbSlamSettings) {
            val preferenceHelper = PreferenceHelper(requireContext())
            preferenceHelper.saveOrbSlamSettings(slamSettings)
            setPreferencesFromResource(com.mag.slam3dvideo.R.xml.root_preferences, rootKey)
        }
    }
}