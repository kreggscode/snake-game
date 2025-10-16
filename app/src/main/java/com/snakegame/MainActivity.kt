package com.snakegame

import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.FrameLayout
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var gameContainer: FrameLayout
    private lateinit var playButton: Button
    private lateinit var settingsButton: Button
    private lateinit var restartButton: Button
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var difficultyText: TextView
    private lateinit var finalScoreText: TextView
    private lateinit var startOverlay: View
    private lateinit var gameOverOverlay: View

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var soundPool: SoundPool
    private lateinit var vibrator: Vibrator

    // Sound IDs
    private var eatSoundId = 0
    private var gameOverSoundId = 0
    private var backgroundMusicId = 0
    private var backgroundMusicStreamId = 0

    // Game state
    private var isGameRunning = false
    private var currentScore = 0
    private var currentLevel = 1
    private var highScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        highScore = sharedPreferences.getInt("high_score", 0)

        // Initialize views
        initializeViews()

        // Initialize sound system
        initializeSoundSystem()

        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        // Create game view
        gameView = SnakeGameView(this, this)
        gameContainer.addView(gameView)

        // Set up button listeners
        setupButtonListeners()

        // Start background music if enabled
        if (sharedPreferences.getBoolean("sound_enabled", true)) {
            playBackgroundMusic()
        }
    }

    private fun initializeViews() {
        gameContainer = findViewById(R.id.gameContainer)
        playButton = findViewById(R.id.playButton)
        settingsButton = findViewById(R.id.settingsButton)
        restartButton = findViewById(R.id.restartButton)
        scoreText = findViewById(R.id.scoreText)
        levelText = findViewById(R.id.levelText)
        difficultyText = findViewById(R.id.difficultyText)
        finalScoreText = findViewById(R.id.finalScoreText)
        startOverlay = findViewById(R.id.startOverlay)
        gameOverOverlay = findViewById(R.id.gameOverOverlay)

        updateDifficultyDisplay()
    }

    private fun initializeSoundSystem() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sound effects (we'll create these files later)
        try {
            eatSoundId = soundPool.load(this, R.raw.eat_sound, 1)
            gameOverSoundId = soundPool.load(this, R.raw.game_over_sound, 1)
            backgroundMusicId = soundPool.load(this, R.raw.background_music, 1)
        } catch (e: Exception) {
            // Handle missing sound files gracefully
        }
    }

    private fun setupButtonListeners() {
        playButton.setOnClickListener {
            if (!isGameRunning) {
                startGame()
            } else {
                pauseGame()
            }
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        restartButton.setOnClickListener {
            restartGame()
        }

        // Start game on screen tap
        startOverlay.setOnClickListener {
            startGame()
        }
    }

    private fun startGame() {
        isGameRunning = true
        gameView.startGame()
        startOverlay.visibility = View.GONE
        gameOverOverlay.visibility = View.GONE
        playButton.text = getString(R.string.pause)
        updateUI()
    }

    private fun pauseGame() {
        isGameRunning = false
        gameView.pauseGame()
        playButton.text = getString(R.string.resume)
    }

    private fun restartGame() {
        gameView.restartGame()
        gameOverOverlay.visibility = View.GONE
        startOverlay.visibility = View.VISIBLE
        playButton.text = getString(R.string.play)
        isGameRunning = false
        currentScore = 0
        currentLevel = 1
        updateUI()
    }

    fun onGameOver(finalScore: Int) {
        isGameRunning = false
        currentScore = finalScore

        // Check for high score
        if (finalScore > highScore) {
            highScore = finalScore
            sharedPreferences.edit().putInt("high_score", highScore).apply()
        }

        // Play game over sound
        if (sharedPreferences.getBoolean("sound_enabled", true)) {
            soundPool.play(gameOverSoundId, 1f, 1f, 0, 0, 1f)
        }

        // Vibrate for game over
        if (sharedPreferences.getBoolean("vibration_enabled", true)) {
            vibratePattern(longArrayOf(0, 100, 100, 100, 100, 200))
        }

        finalScoreText.text = "Final Score: $finalScore\nHigh Score: $highScore"
        gameOverOverlay.visibility = View.VISIBLE
        playButton.text = getString(R.string.play)
    }

    fun onScoreUpdate(score: Int, level: Int) {
        currentScore = score
        currentLevel = level
        updateUI()

        // Play eat sound
        if (sharedPreferences.getBoolean("sound_enabled", true)) {
            soundPool.play(eatSoundId, 0.8f, 0.8f, 0, 0, 1f)
        }

        // Vibrate for eating food
        if (sharedPreferences.getBoolean("vibration_enabled", true)) {
            vibratePattern(longArrayOf(0, 50))
        }
    }

    private fun updateUI() {
        scoreText.text = "Score: $currentScore"
        levelText.text = "Level: $currentLevel"
    }

    private fun playBackgroundMusic() {
        if (backgroundMusicId != 0) {
            backgroundMusicStreamId = soundPool.play(backgroundMusicId, 0.3f, 0.3f, 1, -1, 1f)
        }
    }

    private fun stopBackgroundMusic() {
        if (backgroundMusicStreamId != 0) {
            soundPool.stop(backgroundMusicStreamId)
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume background music if enabled
        if (sharedPreferences.getBoolean("sound_enabled", true) && backgroundMusicStreamId == 0) {
            playBackgroundMusic()
        }
        // Update difficulty display in case it was changed in settings
        updateDifficultyDisplay()
    }

    private fun updateDifficultyDisplay() {
        val difficulty = sharedPreferences.getString("difficulty", "medium") ?: "medium"
        val difficultyTextValue = when (difficulty) {
            "easy" -> "EASY"
            "hard" -> "HARD"
            "expert" -> "EXPERT"
            else -> "MEDIUM"
        }

        val colorRes = when (difficulty) {
            "easy" -> R.color.easy_neon
            "hard" -> R.color.hard_neon
            "expert" -> R.color.expert_neon
            else -> R.color.medium_neon
        }

        difficultyText.text = difficultyTextValue
        difficultyText.setTextColor(resources.getColor(colorRes, null))
        difficultyText.setShadowLayer(8f, 0f, 0f, resources.getColor(colorRes, null))
    }

    override fun onPause() {
        super.onPause()
        // Pause background music
        stopBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}
