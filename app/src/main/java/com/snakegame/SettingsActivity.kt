package com.snakegame

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var soundSwitch: Switch
    private lateinit var vibrationSwitch: Switch
    private lateinit var soundVolumeSeekBar: SeekBar
    private lateinit var vibrationIntensitySeekBar: SeekBar
    private lateinit var difficultyRadioGroup: RadioGroup
    private lateinit var highScoreText: TextView
    private lateinit var gamesPlayedText: TextView
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Initialize preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Initialize views
        initializeViews()

        // Load current settings
        loadSettings()

        // Set up listeners
        setupListeners()
    }

    private fun initializeViews() {
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        soundVolumeSeekBar = findViewById(R.id.soundVolumeSeekBar)
        vibrationIntensitySeekBar = findViewById(R.id.vibrationIntensitySeekBar)
        difficultyRadioGroup = findViewById(R.id.difficultyRadioGroup)
        highScoreText = findViewById(R.id.highScoreText)
        gamesPlayedText = findViewById(R.id.gamesPlayedText)
        resetButton = findViewById(R.id.resetButton)
    }

    private fun loadSettings() {
        // Load sound settings
        soundSwitch.isChecked = sharedPreferences.getBoolean("sound_enabled", true)
        soundVolumeSeekBar.progress = sharedPreferences.getInt("sound_volume", 80)
        soundVolumeSeekBar.isEnabled = soundSwitch.isChecked

        // Load vibration settings
        vibrationSwitch.isChecked = sharedPreferences.getBoolean("vibration_enabled", true)
        vibrationIntensitySeekBar.progress = sharedPreferences.getInt("vibration_intensity", 70)
        vibrationIntensitySeekBar.isEnabled = vibrationSwitch.isChecked

        // Load difficulty setting
        val difficulty = sharedPreferences.getString("difficulty", "medium")
        when (difficulty) {
            "easy" -> findViewById<RadioButton>(R.id.easyRadioButton).isChecked = true
            "hard" -> findViewById<RadioButton>(R.id.hardRadioButton).isChecked = true
            "expert" -> findViewById<RadioButton>(R.id.expertRadioButton).isChecked = true
            else -> findViewById<RadioButton>(R.id.mediumRadioButton).isChecked = true
        }

        // Load game statistics
        val highScore = sharedPreferences.getInt("high_score", 0)
        val gamesPlayed = sharedPreferences.getInt("games_played", 0)
        highScoreText.text = "High Score: $highScore"
        gamesPlayedText.text = "Games Played: $gamesPlayed"
    }

    private fun setupListeners() {
        // Sound switch listener
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            soundVolumeSeekBar.isEnabled = isChecked
            sharedPreferences.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        // Vibration switch listener
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            vibrationIntensitySeekBar.isEnabled = isChecked
            sharedPreferences.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        // Sound volume listener
        soundVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sharedPreferences.edit().putInt("sound_volume", progress).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Vibration intensity listener
        vibrationIntensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sharedPreferences.edit().putInt("vibration_intensity", progress).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Difficulty radio group listener
        difficultyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = when (checkedId) {
                R.id.easyRadioButton -> "easy"
                R.id.hardRadioButton -> "hard"
                R.id.expertRadioButton -> "expert"
                else -> "medium"
            }
            sharedPreferences.edit().putString("difficulty", difficulty).apply()
        }

        // Reset button listener
        resetButton.setOnClickListener {
            sharedPreferences.edit()
                .putInt("high_score", 0)
                .putInt("games_played", 0)
                .apply()

            highScoreText.text = "High Score: 0"
            gamesPlayedText.text = "Games Played: 0"

            Toast.makeText(this, "High score and statistics reset!", Toast.LENGTH_SHORT).show()
        }
    }
}
