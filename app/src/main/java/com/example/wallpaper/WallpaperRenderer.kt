package com.example.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TouchPoint(
    var id: Int = -1,
    var x: Float = 0f,
    var y: Float = 0f,
    var startTime: Long = 0L,
    var lastActiveTime: Long = 0L,
    var active: Boolean = false,
    var pressure: Float = 1f
)

class RippleWave(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    var maxRadius: Float = 500f,
    var alpha: Float = 1f,
    var speed: Float = 400f,
    var color: Int = Color.CYAN,
    var active: Boolean = false
)

class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 3f,
    var color: Int = Color.WHITE,
    var alpha: Float = 1f,
    var phase: Float = 0f,
    var life: Float = 1f,
    var isTrail: Boolean = false
)

class WallpaperRenderer {

    var config: WallpaperConfig = WallpaperConfig()
        set(value) {
            field = value
            reinitializeEngine()
        }

    private var width: Int = 1080
    private var height: Int = 2400

    private var globalTimeSec: Float = 0f

    // Pre-allocated Drawing Objects for zero GC overhead
    private val bgPaint = Paint()
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val wavePath = Path()
    private val tempPath = Path()

    // Multi-touch tracking (supports up to 10 fingers)
    private val maxTouchPoints = 10
    private val touchPoints = Array(maxTouchPoints) { TouchPoint() }

    // Pre-allocated Ripple Pool
    private val maxRipples = 25
    private val ripples = Array(maxRipples) { RippleWave() }
    private var rippleNextIndex = 0

    // Pre-allocated Particle Pool
    private var maxParticles = 200
    private var particles = Array(maxParticles) { Particle() }

    // Listener for tactile haptic feedback triggers
    var onHapticFeedbackNeeded: (() -> Unit)? = null

    // Performance statistics
    var currentFps: Int = 60
        private set
    var activeTouchCount: Int = 0
        private set

    private var lastBgGradientWidth = 0
    private var lastBgGradientHeight = 0

    init {
        reinitializeEngine()
    }

    fun setDimensions(w: Int, h: Int) {
        if (width != w || height != h) {
            width = maxOf(1, w)
            height = maxOf(1, h)
            resetParticlePositions()
        }
    }

    private fun reinitializeEngine() {
        maxParticles = config.particleCount.coerceIn(20, 300)
        if (particles.size != maxParticles) {
            particles = Array(maxParticles) { Particle() }
        }
        resetParticlePositions()
    }

    private fun resetParticlePositions() {
        val preset = config.colorPreset
        val colors = intArrayOf(preset.accentPrimary, preset.accentSecondary, preset.accentTertiary)

        for (i in particles.indices) {
            val p = particles[i]
            p.x = (Math.random() * width).toFloat()
            p.y = (Math.random() * height).toFloat()
            p.vx = ((Math.random() - 0.5) * 60 * config.speedScale).toFloat()
            p.vy = ((Math.random() - 0.5) * 60 * config.speedScale).toFloat()
            p.radius = (2f + Math.random() * 4f).toFloat()
            p.color = colors[i % colors.size]
            p.alpha = (0.3f + Math.random() * 0.7f).toFloat()
            p.phase = (Math.random() * Math.PI * 2).toFloat()
            p.isTrail = false
            p.life = 1f
        }
    }

    fun handleMotionEvent(event: MotionEvent) {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val now = System.currentTimeMillis()

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val pressure = event.getPressure(pointerIndex)

                // Assign to free touch point
                for (tp in touchPoints) {
                    if (!tp.active || tp.id == pointerId) {
                        tp.id = pointerId
                        tp.x = x
                        tp.y = y
                        tp.startTime = now
                        tp.lastActiveTime = now
                        tp.pressure = pressure
                        tp.active = true
                        break
                    }
                }

                // Trigger interactive touch ripple
                triggerRipple(x, y, pressure)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    for (tp in touchPoints) {
                        if (tp.active && tp.id == id) {
                            val dx = x - tp.x
                            val dy = y - tp.y
                            val dist = sqrt(dx * dx + dy * dy)

                            if (dist > 15f && config.showTouchTrails) {
                                spawnTrailParticle(x, y)
                            }

                            tp.x = x
                            tp.y = y
                            tp.lastActiveTime = now
                            break
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerId = event.getPointerId(pointerIndex)
                for (tp in touchPoints) {
                    if (tp.id == pointerId) {
                        tp.active = false
                        break
                    }
                }
            }
        }

        // Count active touches
        activeTouchCount = touchPoints.count { it.active }
    }

    private fun triggerRipple(x: Float, y: Float, pressure: Float) {
        val ripple = ripples[rippleNextIndex]
        ripple.x = x
        ripple.y = y
        ripple.radius = 0f
        ripple.maxRadius = (350f + pressure * 250f) * config.rippleIntensity
        ripple.alpha = 1f
        ripple.speed = 500f * config.speedScale
        ripple.color = when ((0..2).random()) {
            0 -> config.colorPreset.accentPrimary
            1 -> config.colorPreset.accentSecondary
            else -> config.colorPreset.accentTertiary
        }
        ripple.active = true

        rippleNextIndex = (rippleNextIndex + 1) % maxRipples

        if (config.enableHaptics) {
            onHapticFeedbackNeeded?.invoke()
        }
    }

    private fun spawnTrailParticle(x: Float, y: Float) {
        // Find inactive or oldest particle to convert into trail particle
        var p = particles.find { !pIsActive(it) }
        if (p == null) {
            p = particles[(0 until particles.size).random()]
        }
        p.x = x + ((Math.random() - 0.5) * 20).toFloat()
        p.y = y + ((Math.random() - 0.5) * 20).toFloat()
        p.vx = ((Math.random() - 0.5) * 30).toFloat()
        p.vy = ((Math.random() - 0.5) * 30).toFloat()
        p.radius = (3f + Math.random() * 5f).toFloat()
        p.color = config.colorPreset.accentSecondary
        p.alpha = 1f
        p.life = 1f
        p.isTrail = true
    }

    private fun pIsActive(p: Particle): Boolean {
        return !p.isTrail || p.life > 0.05f
    }

    fun update(deltaSec: Float) {
        val timeStep = deltaSec * config.speedScale
        globalTimeSec += timeStep

        // Update Ripples
        for (r in ripples) {
            if (r.active) {
                r.radius += r.speed * deltaSec
                r.alpha = 1f - (r.radius / r.maxRadius).coerceIn(0f, 1f)
                if (r.radius >= r.maxRadius || r.alpha <= 0.01f) {
                    r.active = false
                }
            }
        }

        // Update Particles physics
        for (p in particles) {
            if (p.isTrail) {
                p.life -= deltaSec * 1.5f
                p.alpha = p.life.coerceIn(0f, 1f)
            } else {
                p.phase += deltaSec * 1.5f
            }

            p.x += p.vx * deltaSec
            p.y += p.vy * deltaSec

            // Multi-touch gravitational interaction
            for (tp in touchPoints) {
                if (tp.active) {
                    val dx = tp.x - p.x
                    val dy = tp.y - p.y
                    val distSq = dx * dx + dy * dy
                    val touchRadius = 350f * config.touchSensitivity

                    if (distSq < touchRadius * touchRadius && distSq > 1f) {
                        val dist = sqrt(distSq)
                        val force = (1f - dist / touchRadius) * 250f * config.touchSensitivity
                        val dirX = dx / dist
                        val dirY = dy / dist

                        // Attraction or repulsion based on effect
                        if (config.effectMode == EffectMode.NEON_PARTICLES) {
                            p.vx += dirX * force * deltaSec
                            p.vy += dirY * force * deltaSec
                        } else {
                            p.vx -= dirX * force * 1.5f * deltaSec
                            p.vy -= dirY * force * 1.5f * deltaSec
                        }
                    }
                }
            }

            // Physics behavior: Zero Gravity vs Standard Gravity
            if (config.zeroGravityMode) {
                // Zero-Gravity: No velocity friction/damping, weightless harmonic float
                p.vx += sin(p.phase * 0.5f) * 1.5f * deltaSec
                p.vy += cos(p.phase * 0.5f) * 1.5f * deltaSec

                // Smooth screen edge wrapping for infinite zero-g space
                if (p.x < 0) p.x = width.toFloat()
                else if (p.x > width) p.x = 0f

                if (p.y < 0) p.y = height.toFloat()
                else if (p.y > height) p.y = 0f
            } else {
                // Standard Gravity: Velocity Damping & Edge Bouncing
                p.vx *= 0.98f
                p.vy *= 0.98f

                // Bounce on boundaries
                if (p.x < 0) {
                    p.x = 0f
                    p.vx = abs(p.vx)
                } else if (p.x > width) {
                    p.x = width.toFloat()
                    p.vx = -abs(p.vx)
                }

                if (p.y < 0) {
                    p.y = 0f
                    p.vy = abs(p.vy)
                } else if (p.y > height) {
                    p.y = height.toFloat()
                    p.vy = -abs(p.vy)
                }
            }
        }
    }

    fun render(canvas: Canvas, measuredFps: Int = 60) {
        this.currentFps = measuredFps

        // 1. Draw Background Gradient
        drawBackground(canvas)

        // 2. Render Selected Effect Mode
        when (config.effectMode) {
            EffectMode.QUANTUM_GRID -> renderQuantumGrid(canvas)
            EffectMode.NEON_PARTICLES -> renderNeonParticles(canvas)
            EffectMode.AURORA_FLOW -> renderAuroraFlow(canvas)
            EffectMode.COSMIC_PLASMA -> renderCosmicPlasma(canvas)
        }

        // 3. Render Ripples
        renderRipples(canvas)

        // 4. Render Active Touch Glows
        renderTouchGlows(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val preset = config.colorPreset
        if (width != lastBgGradientWidth || height != lastBgGradientHeight) {
            lastBgGradientWidth = width
            lastBgGradientHeight = height
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(preset.bgPrimary, preset.bgSecondary, preset.bgPrimary),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    private fun renderQuantumGrid(canvas: Canvas) {
        val gridStep = (60f * (width / 1080f).coerceAtLeast(0.6f)).coerceIn(40f, 90f)
        val cols = (width / gridStep).toInt() + 2
        val rows = (height / gridStep).toInt() + 2

        val preset = config.colorPreset
        gridPaint.color = preset.accentPrimary

        val time = globalTimeSec

        // Draw distorted grid lines
        for (r in 0 until rows) {
            val yBase = r * gridStep
            wavePath.reset()

            for (c in 0 until cols) {
                val xBase = c * gridStep

                var offsetX = 0f
                var offsetY = 0f

                // Mathematical wave distortion
                offsetY += sin(xBase * 0.015f + time * 2f) * 12f

                // Calculate ripple displacement
                for (ripple in ripples) {
                    if (ripple.active) {
                        val dx = xBase - ripple.x
                        val dy = yBase - ripple.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val rippleDist = abs(dist - ripple.radius)

                        if (rippleDist < 120f) {
                            val factor = (1f - rippleDist / 120f) * ripple.alpha
                            val wave = sin(rippleDist * 0.08f) * factor * 35f
                            offsetX += (dx / (dist + 0.1f)) * wave
                            offsetY += (dy / (dist + 0.1f)) * wave
                        }
                    }
                }

                val finalX = xBase + offsetX
                val finalY = yBase + offsetY

                if (c == 0) {
                    wavePath.moveTo(finalX, finalY)
                } else {
                    wavePath.lineTo(finalX, finalY)
                }
            }

            gridPaint.alpha = (40 + (sin(r * 0.3f + time) * 20).toInt()).coerceIn(15, 120)
            canvas.drawPath(wavePath, gridPaint)
        }

        // Render Particle Nodes over Grid
        renderNeonParticles(canvas)
    }

    private fun renderNeonParticles(canvas: Canvas) {
        val preset = config.colorPreset

        // Draw Web Connections between close particles
        if (config.enableWebConnections) {
            val maxConnectDistSq = 140f * 140f
            linePaint.color = preset.accentPrimary

            val count = particles.size
            for (i in 0 until count) {
                val p1 = particles[i]
                if (!pIsActive(p1)) continue

                for (j in i + 1 until count) {
                    val p2 = particles[j]
                    if (!pIsActive(p2)) continue

                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    val distSq = dx * dx + dy * dy

                    if (distSq < maxConnectDistSq) {
                        val alphaFactor = 1f - sqrt(distSq) / 140f
                        linePaint.alpha = (alphaFactor * p1.alpha * p2.alpha * 120).toInt().coerceIn(0, 180)
                        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
                    }
                }
            }
        }

        // Draw Individual Particle Nodes
        for (p in particles) {
            if (!pIsActive(p)) continue
            particlePaint.color = p.color
            val pulseRadius = p.radius + sin(p.phase) * 1.2f
            particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, pulseRadius, particlePaint)
        }
    }

    private fun renderAuroraFlow(canvas: Canvas) {
        val preset = config.colorPreset
        val colors = intArrayOf(preset.accentPrimary, preset.accentSecondary, preset.accentTertiary)
        val waveCount = 4

        for (w in 0 until waveCount) {
            wavePath.reset()
            val yOffset = height * (0.25f + w * 0.18f)
            val baseColor = colors[w % colors.size]

            wavePath.moveTo(0f, height.toFloat())
            wavePath.lineTo(0f, yOffset)

            val step = 30f
            var x = 0f
            while (x <= width + step) {
                val time = globalTimeSec * (0.8f + w * 0.3f)
                val freq1 = 0.003f + w * 0.001f
                val freq2 = 0.007f

                var y = yOffset + sin(x * freq1 + time) * 80f + cos(x * freq2 - time * 0.7f) * 40f

                // Apply touch disturbance
                for (tp in touchPoints) {
                    if (tp.active) {
                        val dx = x - tp.x
                        val dist = abs(dx)
                        if (dist < 250f) {
                            val factor = 1f - dist / 250f
                            y += sin(dist * 0.02f + time * 3f) * factor * 60f
                        }
                    }
                }

                wavePath.lineTo(x, y)
                x += step
            }

            wavePath.lineTo(width.toFloat(), height.toFloat())
            wavePath.close()

            gridPaint.style = Paint.Style.FILL
            gridPaint.color = baseColor
            gridPaint.alpha = (35 + w * 15).coerceIn(10, 100)
            canvas.drawPath(wavePath, gridPaint)
        }

        // Draw foreground particle stars
        renderNeonParticles(canvas)
    }

    private fun renderCosmicPlasma(canvas: Canvas) {
        val preset = config.colorPreset
        val time = globalTimeSec

        // Draw radial plasma pulses
        val pulseCount = 3
        for (i in 0 until pulseCount) {
            val cx = width * (0.3f + 0.4f * sin(time * 0.5f + i))
            val cy = height * (0.3f + 0.4f * cos(time * 0.4f + i * 2))
            val radius = (300f + sin(time + i) * 100f) * (width / 1080f).coerceAtLeast(0.8f)

            val color = when (i) {
                0 -> preset.accentPrimary
                1 -> preset.accentSecondary
                else -> preset.accentTertiary
            }

            val shader = RadialGradient(
                cx, cy, radius.coerceAtLeast(10f),
                intArrayOf(color, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            gridPaint.style = Paint.Style.FILL
            gridPaint.shader = shader
            gridPaint.alpha = 70
            canvas.drawCircle(cx, cy, radius, gridPaint)
            gridPaint.shader = null
        }

        renderNeonParticles(canvas)
    }

    private fun renderRipples(canvas: Canvas) {
        for (r in ripples) {
            if (r.active) {
                ripplePaint.color = r.color
                ripplePaint.alpha = (r.alpha * 220).toInt().coerceIn(0, 255)
                ripplePaint.strokeWidth = 3f + (1f - r.alpha) * 4f
                canvas.drawCircle(r.x, r.y, r.radius, ripplePaint)
            }
        }
    }

    private fun renderTouchGlows(canvas: Canvas) {
        val preset = config.colorPreset
        for (tp in touchPoints) {
            if (tp.active) {
                val glowRadius = 70f * tp.pressure
                particlePaint.color = preset.accentPrimary
                particlePaint.alpha = 140
                canvas.drawCircle(tp.x, tp.y, glowRadius, particlePaint)

                particlePaint.color = Color.WHITE
                particlePaint.alpha = 220
                canvas.drawCircle(tp.x, tp.y, glowRadius * 0.3f, particlePaint)
            }
        }
    }

    fun clearTouches() {
        for (tp in touchPoints) {
            tp.active = false
        }
        activeTouchCount = 0
    }
}
