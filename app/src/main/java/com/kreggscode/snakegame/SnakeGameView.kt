package com.kreggscode.snakegame

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class SnakeGameView @JvmOverloads constructor(
    context: Context,
    private val mainActivity: MainActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    // Game constants
    private val GRID_SIZE = 20
    private val INITIAL_SNAKE_LENGTH = 3
    private val FOOD_SCORE = 10

    // Game state
    private var gameThread: Thread? = null
    private var isRunning = false
    private var isPaused = false

    // Snake data
    private val snakeBody = mutableListOf<Point>()
    private var snakeDirection = Direction.RIGHT
    private var nextDirection = Direction.RIGHT

    // Food
    private var foodPosition = Point()

    // Game metrics
    private var score = 0
    private var level = 1
    private var speedDelay = 200L // milliseconds

    // Touch handling
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val swipeThreshold = 50f

    // Rendering
    private var canvas: Canvas? = null
    private val paint = Paint()
    private val backgroundPaint = Paint()
    private val snakeHeadPaint = Paint()
    private val snakeBodyPaint = Paint()
    private val foodPaint = Paint()
    private val gridPaint = Paint()
    private val glowPaint = Paint()

    // Neon Colors
    private val backgroundColor = ContextCompat.getColor(context, R.color.background_dark)
    private val snakeHeadColor = ContextCompat.getColor(context, R.color.snake_head)
    private val snakeBodyColor = ContextCompat.getColor(context, R.color.snake_body)
    private val snakeGlowColor = ContextCompat.getColor(context, R.color.snake_glow)
    private val snakeShadowColor = ContextCompat.getColor(context, R.color.snake_shadow)
    private val foodColor = ContextCompat.getColor(context, R.color.food_primary)
    private val foodInnerColor = ContextCompat.getColor(context, R.color.food_inner)
    private val foodGlowColor = ContextCompat.getColor(context, R.color.food_glow)
    private val gridColor = ContextCompat.getColor(context, R.color.grid_line)
    private val gridGlowColor = ContextCompat.getColor(context, R.color.grid_glow)

    // Game dimensions
    private var cellSize = 0f
    private var gameWidth = 0
    private var gameHeight = 0
    private var offsetX = 0f
    private var offsetY = 0f

    // Particle effects
    private val particles = mutableListOf<Particle>()
    
    // Initialization flag
    private var isInitialized = false

    init {
        holder.addCallback(this)
        isFocusable = true

        setupPaints()
    }

    private fun setupPaints() {
        // Background paint
        backgroundPaint.color = backgroundColor
        backgroundPaint.style = Paint.Style.FILL

        // Snake head paint with enhanced neon glow
        snakeHeadPaint.color = snakeHeadColor
        snakeHeadPaint.style = Paint.Style.FILL
        snakeHeadPaint.setShadowLayer(12f, 0f, 0f, snakeGlowColor)

        // Snake body paint with neon glow
        snakeBodyPaint.color = snakeBodyColor
        snakeBodyPaint.style = Paint.Style.FILL
        snakeBodyPaint.setShadowLayer(8f, 0f, 0f, snakeGlowColor)

        // Food paint with intense neon glow
        foodPaint.color = foodColor
        foodPaint.style = Paint.Style.FILL
        foodPaint.setShadowLayer(20f, 0f, 0f, foodGlowColor)

        // Grid paint with neon effect
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1.5f
        gridPaint.alpha = 80

        // Enhanced glow effect paint
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface is ready, but we'll start the game when the user presses play
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        calculateDimensions(width, height)
        if (!isInitialized) {
            initializeGame()
            isInitialized = true
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
    }

    private fun calculateDimensions(width: Int, height: Int) {
        // Calculate cell size to fit the grid
        val maxGridWidth = width / GRID_SIZE
        val maxGridHeight = height / GRID_SIZE
        cellSize = min(maxGridWidth, maxGridHeight).toFloat()

        // Calculate game area dimensions
        gameWidth = (width / cellSize).toInt()
        gameHeight = (height / cellSize).toInt()

        // Calculate offsets to center the game
        offsetX = (width - gameWidth * cellSize) / 2
        offsetY = (height - gameHeight * cellSize) / 2
    }

    private fun initializeGame() {
        snakeBody.clear()
        particles.clear()

        // Initialize snake at center
        val startX = gameWidth / 2
        val startY = gameHeight / 2

        for (i in 0 until INITIAL_SNAKE_LENGTH) {
            snakeBody.add(Point(startX - i, startY))
        }

        snakeDirection = Direction.RIGHT
        nextDirection = Direction.RIGHT

        generateFood()
        score = 0
        level = 1
        speedDelay = 200L
    }

    private fun generateFood() {
        do {
            foodPosition.x = Random.nextInt(gameWidth)
            foodPosition.y = Random.nextInt(gameHeight)
        } while (snakeBody.contains(foodPosition))
    }

    fun startGame() {
        if (gameThread == null) {
            isRunning = true
            isPaused = false
            gameThread = Thread(this)
            gameThread?.start()
        } else {
            isPaused = false
        }
    }

    fun pauseGame() {
        isPaused = true
    }

    fun restartGame() {
        stopGame()
        initializeGame()
    }

    private fun stopGame() {
        isRunning = false
        isPaused = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        gameThread = null
    }

    override fun run() {
        var lastUpdateTime = System.currentTimeMillis()

        while (isRunning) {
            if (!isPaused) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= speedDelay) {
                    updateGame()
                    lastUpdateTime = currentTime
                }
            }

            drawGame()
            Thread.sleep(16) // ~60 FPS
        }
    }

    private fun updateGame() {
        // Update snake direction
        snakeDirection = nextDirection

        // Calculate new head position
        val head = snakeBody.first()
        val newHead = when (snakeDirection) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        // Check wall collision
        if (newHead.x < 0 || newHead.x >= gameWidth ||
            newHead.y < 0 || newHead.y >= gameHeight) {
            gameOver()
            return
        }

        // Check self collision
        if (snakeBody.contains(newHead)) {
            gameOver()
            return
        }

        // Add new head
        snakeBody.add(0, newHead)

        // Check food collision
        if (newHead == foodPosition) {
            score += FOOD_SCORE
            level = (score / 50) + 1
            speedDelay = maxOf(80L, 200L - (level - 1) * 20L)

            // Create particle effect
            createFoodParticles(foodPosition)

            generateFood()
            mainActivity.onScoreUpdate(score, level)
        } else {
            // Remove tail if no food eaten
            snakeBody.removeAt(snakeBody.size - 1)
        }

        // Update particles
        updateParticles()
    }

    private fun createFoodParticles(position: Point) {
        for (i in 0..12) {
            val angle = (i * 30).toDouble()
            val speed = 3f + Random.nextFloat() * 4f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat()
            val neonColors = arrayOf(
                ContextCompat.getColor(context, R.color.particle_effect),
                ContextCompat.getColor(context, R.color.food_glow),
                ContextCompat.getColor(context, R.color.snake_glow),
                ContextCompat.getColor(context, R.color.primary_light)
            )
            val randomColor = neonColors[Random.nextInt(neonColors.size)]
            particles.add(Particle(position.x.toFloat(), position.y.toFloat(), vx, vy, randomColor, 40))
        }
    }

    private fun updateParticles() {
        particles.removeAll { particle ->
            particle.update()
            particle.isDead()
        }
    }

    private fun gameOver() {
        stopGame()
        mainActivity.onGameOver(score)
    }

    private fun drawGame() {
        canvas = holder.lockCanvas()
        canvas?.let { c ->
            // Clear background
            c.drawColor(backgroundColor)

            // Draw grid (subtle)
            drawGrid(c)

            // Draw food glow effect
            drawFoodGlow(c)

            // Draw snake
            drawSnake(c)

            // Draw food
            drawFood(c)

            // Draw particles
            drawParticles(c)

            // Draw score overlay
            drawScoreOverlay(c)

            holder.unlockCanvasAndPost(c)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (x in 0..gameWidth) {
            val startX = offsetX + x * cellSize
            canvas.drawLine(startX, offsetY, startX, offsetY + gameHeight * cellSize, gridPaint)
        }
        for (y in 0..gameHeight) {
            val startY = offsetY + y * cellSize
            canvas.drawLine(offsetX, startY, offsetX + gameWidth * cellSize, startY, gridPaint)
        }
    }

    private fun drawSnake(canvas: Canvas) {
        snakeBody.forEachIndexed { index, segment ->
            val left = offsetX + segment.x * cellSize
            val top = offsetY + segment.y * cellSize
            val right = left + cellSize
            val bottom = top + cellSize

            val rect = RectF(left + 2, top + 2, right - 2, bottom - 2)

            if (index == 0) {
                // Snake head with enhanced neon gradient and glow
                val headGradient = LinearGradient(
                    left, top, right, bottom,
                    intArrayOf(snakeHeadColor, snakeGlowColor, snakeHeadColor),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                snakeHeadPaint.shader = headGradient

                // Add extra glow around head
                glowPaint.color = snakeGlowColor
                glowPaint.alpha = 120
                canvas.drawRoundRect(RectF(rect.left - 4, rect.top - 4, rect.right + 4, rect.bottom + 4), 12f, 12f, glowPaint)

                canvas.drawRoundRect(rect, 8f, 8f, snakeHeadPaint)

                // Add bright eyes for the snake head
                val eyePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    setShadowLayer(4f, 0f, 0f, Color.YELLOW)
                }
                val eyeSize = cellSize * 0.08f
                canvas.drawCircle(rect.centerX() - cellSize * 0.15f, rect.centerY() - cellSize * 0.1f, eyeSize, eyePaint)
                canvas.drawCircle(rect.centerX() + cellSize * 0.15f, rect.centerY() - cellSize * 0.1f, eyeSize, eyePaint)
            } else {
                // Snake body with neon gradient
                val bodyGradient = LinearGradient(
                    left, top, right, bottom,
                    intArrayOf(snakeBodyColor, ContextCompat.getColor(context, R.color.snake_body_gradient)),
                    null, Shader.TileMode.CLAMP
                )
                snakeBodyPaint.shader = bodyGradient
                canvas.drawRoundRect(rect, 6f, 6f, snakeBodyPaint)
            }
        }
    }

    private fun drawFood(canvas: Canvas) {
        val centerX = offsetX + foodPosition.x * cellSize + cellSize / 2
        val centerY = offsetY + foodPosition.y * cellSize + cellSize / 2
        val radius = cellSize * 0.35f

        // Pulsing effect for the food
        val pulse = (System.currentTimeMillis() % 800) / 800f
        val scale = 0.85f + 0.3f * abs(Math.sin(pulse * Math.PI * 4).toFloat())
        val currentRadius = radius * scale

        // Draw multiple glow layers for intense neon effect
        val glowRadii = arrayOf(currentRadius * 2.5f, currentRadius * 2.0f, currentRadius * 1.6f)

        for (i in glowRadii.indices) {
            glowPaint.color = when (i) {
                0 -> Color.argb(60, Color.red(foodGlowColor), Color.green(foodGlowColor), Color.blue(foodGlowColor))
                1 -> Color.argb(100, Color.red(foodGlowColor), Color.green(foodGlowColor), Color.blue(foodGlowColor))
                else -> Color.argb(150, Color.red(foodGlowColor), Color.green(foodGlowColor), Color.blue(foodGlowColor))
            }
            canvas.drawCircle(centerX, centerY, glowRadii[i], glowPaint)
        }

        // Draw the inner food with gradient
        val foodGradient = RadialGradient(
            centerX, centerY, currentRadius,
            intArrayOf(foodColor, foodInnerColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val innerFoodPaint = Paint().apply {
            shader = foodGradient
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 0f, foodGlowColor)
        }

        canvas.drawCircle(centerX, centerY, currentRadius, innerFoodPaint)

        // Add a bright center highlight
        val highlightPaint = Paint().apply {
            color = Color.WHITE
            alpha = 200
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX - currentRadius * 0.3f, centerY - currentRadius * 0.3f,
                         currentRadius * 0.2f, highlightPaint)
    }

    private fun drawFoodGlow(canvas: Canvas) {
        val centerX = offsetX + foodPosition.x * cellSize + cellSize / 2
        val centerY = offsetY + foodPosition.y * cellSize + cellSize / 2

        glowPaint.color = ContextCompat.getColor(context, R.color.food_glow)
        glowPaint.alpha = 100
        canvas.drawCircle(centerX, centerY, cellSize * 1.5f, glowPaint)
    }

    private fun drawParticles(canvas: Canvas) {
        particles.forEach { particle ->
            val left = offsetX + particle.x * cellSize
            val top = offsetY + particle.y * cellSize
            val right = left + cellSize * 0.3f
            val bottom = top + cellSize * 0.3f

            paint.color = particle.color
            paint.alpha = particle.alpha
            canvas.drawOval(RectF(left, top, right, bottom), paint)
        }
    }

    private fun drawScoreOverlay(canvas: Canvas) {
        // This can be enhanced with more visual elements if needed
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY

                if (abs(deltaX) > swipeThreshold || abs(deltaY) > swipeThreshold) {
                    if (abs(deltaX) > abs(deltaY)) {
                        // Horizontal swipe
                        nextDirection = if (deltaX > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        // Vertical swipe
                        nextDirection = if (deltaY > 0) Direction.DOWN else Direction.UP
                    }

                    // Prevent immediate reverse direction
                    if ((snakeDirection == Direction.UP && nextDirection == Direction.DOWN) ||
                        (snakeDirection == Direction.DOWN && nextDirection == Direction.UP) ||
                        (snakeDirection == Direction.LEFT && nextDirection == Direction.RIGHT) ||
                        (snakeDirection == Direction.RIGHT && nextDirection == Direction.LEFT)) {
                        nextDirection = snakeDirection
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private inner class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var life: Int
    ) {
        var alpha = 255

        fun update() {
            x += vx * 0.1f
            y += vy * 0.1f
            life--
            alpha = (life * 255) / 30
        }

        fun isDead() = life <= 0
    }
}
