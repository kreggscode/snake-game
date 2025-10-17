package com.kreggscode.snakegame

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var vibrationSwitch: Switch? = null
    private var vibrationIntensitySeekBar: SeekBar? = null
    private var difficultyRadioGroup: RadioGroup? = null
    private var highScoreText: TextView? = null
    private var gamesPlayedText: TextView? = null
    private var resetButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize preferences first
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Set up toolbar
        try {
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize views with null safety
        initializeViews()

        // Load current settings
        loadSettings()

        // Set up listeners
        setupListeners()
    }

    private fun initializeViews() {
        try {
            vibrationSwitch = findViewById(R.id.vibrationSwitch)
            vibrationIntensitySeekBar = findViewById(R.id.vibrationIntensitySeekBar)
            difficultyRadioGroup = findViewById(R.id.difficultyRadioGroup)
            highScoreText = findViewById(R.id.highScoreText)
            gamesPlayedText = findViewById(R.id.gamesPlayedText)
            resetButton = findViewById(R.id.resetButton)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        try {
            // Load vibration settings
            vibrationSwitch?.isChecked = sharedPreferences.getBoolean("vibration_enabled", true)
            vibrationIntensitySeekBar?.progress = sharedPreferences.getInt("vibration_intensity", 70)
            vibrationIntensitySeekBar?.isEnabled = vibrationSwitch?.isChecked ?: true

            // Load difficulty setting
            val difficulty = sharedPreferences.getString("difficulty", "medium") ?: "medium"
            when (difficulty) {
                "easy" -> findViewById<RadioButton>(R.id.easyRadioButton)?.isChecked = true
                "hard" -> findViewById<RadioButton>(R.id.hardRadioButton)?.isChecked = true
                "expert" -> findViewById<RadioButton>(R.id.expertRadioButton)?.isChecked = true
                else -> findViewById<RadioButton>(R.id.mediumRadioButton)?.isChecked = true
            }

            // Load game statistics
            val highScore = sharedPreferences.getInt("high_score", 0)
            val gamesPlayed = sharedPreferences.getInt("games_played", 0)
            highScoreText?.text = "High Score: $highScore"
            gamesPlayedText?.text = "Games Played: $gamesPlayed"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        try {
            // Vibration switch listener
            vibrationSwitch?.setOnCheckedChangeListener { _, isChecked ->
                vibrationIntensitySeekBar?.isEnabled = isChecked
                sharedPreferences.edit().putBoolean("vibration_enabled", isChecked).apply()
            }

            // Vibration intensity listener
            vibrationIntensitySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        sharedPreferences.edit().putInt("vibration_intensity", progress).apply()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Difficulty radio group listener
            difficultyRadioGroup?.setOnCheckedChangeListener { _, checkedId ->
                val difficulty = when (checkedId) {
                    R.id.easyRadioButton -> "easy"
                    R.id.hardRadioButton -> "hard"
                    R.id.expertRadioButton -> "expert"
                    else -> "medium"
                }
                sharedPreferences.edit().putString("difficulty", difficulty).apply()
            }

            // Reset button listener
            resetButton?.setOnClickListener {
                sharedPreferences.edit()
                    .putInt("high_score", 0)
                    .putInt("games_played", 0)
                    .apply()

                highScoreText?.text = "High Score: 0"
                gamesPlayedText?.text = "Games Played: 0"

                Toast.makeText(this, "High score and statistics reset!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
