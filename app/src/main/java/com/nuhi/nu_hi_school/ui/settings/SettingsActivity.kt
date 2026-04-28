package com.nuhi.nu_hi_school.ui.settings

import android.app.Activity
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nuhi.nu_hi_school.data.repository.*
import com.nuhi.nu_hi_school.databinding.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSettings()
        loadSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettings() {
        binding.btnTraining.setOnClickListener {
            startActivity(Intent(this, com.nuhi.nu_hi_school.ui.training.TrainingActivity::class.java))
        }

        binding.btnClearTrainingData.setOnClickListener {
            clearTrainingData()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("dark_mode", isChecked)
        }

        binding.switchStylusDetect.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("stylus_detect", isChecked)
        }

        binding.sliderPressureSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.pressureValue.text = "Sensitivity: $progress%"
                saveSetting("pressure_sensitivity", progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("English", "Polish", "German", "French", "Spanish", "Other")
        )
    }

    private fun loadSettings() {
        scope.launch {
            dataStore.data.collect { prefs ->
                binding.switchDarkMode.isChecked = prefs[booleanPreferencesKey("dark_mode")] ?: false
                binding.switchStylusDetect.isChecked = prefs[booleanPreferencesKey("stylus_detect")] ?: true
                binding.sliderPressureSensitivity.progress = prefs[intPreferencesKey("pressure_sensitivity")] ?: 50
                binding.pressureValue.text = "Sensitivity: ${prefs[intPreferencesKey("pressure_sensitivity")] ?: 50}%"
            }
        }
    }

    private fun saveSetting(key: String, value: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(key)] = value
            }
        }
    }

    private fun saveSetting(key: String, value: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[intPreferencesKey(key)] = value
            }
        }
    }

    private fun clearTrainingData() {
        scope.launch {
            TrainingRepository(this@SettingsActivity).clearSamples()
            Toast.makeText(this@SettingsActivity, "Training data cleared!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
