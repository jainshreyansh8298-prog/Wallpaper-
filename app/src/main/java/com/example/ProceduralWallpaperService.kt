package com.example

import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.wallpaper.WallpaperConfig
import com.example.wallpaper.WallpaperRenderer
import com.example.wallpaper.WallpaperSettingsStore

class ProceduralWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ProceduralEngine()
    }

    inner class ProceduralEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val renderer = WallpaperRenderer()
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var isEngineRunning = false

        private var lastFrameTimeNanos = System.nanoTime()
        private var frameCounter = 0
        private var lastFpsCalcTimeMs = System.currentTimeMillis()
        private var currentFps = 60

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isVisible) return

                val nowNanos = System.nanoTime()
                val deltaSec = ((nowNanos - lastFrameTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
                lastFrameTimeNanos = nowNanos

                // Calculate FPS
                frameCounter++
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastFpsCalcTimeMs >= 1000) {
                    currentFps = frameCounter
                    frameCounter = 0
                    lastFpsCalcTimeMs = nowMs
                }

                // Render Frame
                drawFrame(deltaSec)

                // Schedule next frame based on FPS target
                if (isVisible) {
                    val frameDelayMs = (1000L / renderer.config.targetFps.coerceIn(15, 60)).coerceAtLeast(1L)
                    handler.postDelayed(this, frameDelayMs)
                }
            }
        }

        private val vibrator by lazy {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
        }

        private fun triggerHaptic() {
            try {
                if (vibrator?.hasVibrator() == true) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(8L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(8L)
                    }
                }
            } catch (e: Throwable) {
                // Ignore vibration error on unsupported hardware
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            renderer.onHapticFeedbackNeeded = { triggerHaptic() }

            // Register preference listener
            val prefs = WallpaperSettingsStore.getPrefs(applicationContext)
            prefs.registerOnSharedPreferenceChangeListener(this)

            // Load initial config
            reloadConfig()
        }

        override fun onDestroy() {
            super.onDestroy()
            stopEngine()
            val prefs = WallpaperSettingsStore.getPrefs(applicationContext)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                startEngine()
            } else {
                stopEngine()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startEngine()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer.setDimensions(width, height)
            drawFrame(0.016f)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopEngine()
        }

        override fun onTouchEvent(event: MotionEvent) {
            renderer.handleMotionEvent(event)
            super.onTouchEvent(event)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            reloadConfig()
        }

        private fun reloadConfig() {
            try {
                val config = WallpaperSettingsStore.loadConfig(applicationContext)
                renderer.config = config
            } catch (e: Exception) {
                Log.e("ProceduralWallpaper", "Error loading config", e)
            }
        }

        private fun startEngine() {
            if (!isEngineRunning && isVisible) {
                isEngineRunning = true
                lastFrameTimeNanos = System.nanoTime()
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            }
        }

        private fun stopEngine() {
            isEngineRunning = false
            handler.removeCallbacks(drawRunnable)
            renderer.clearTouches()
        }

        private fun drawFrame(deltaSec: Float) {
            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid) return

            var canvas: Canvas? = null
            try {
                // Hardware accelerated drawing if supported, fallback to standard canvas
                canvas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        holder.lockHardwareCanvas()
                    } catch (e: Throwable) {
                        holder.lockCanvas()
                    }
                } else {
                    holder.lockCanvas()
                }

                if (canvas != null) {
                    renderer.update(deltaSec)
                    renderer.render(canvas, currentFps)
                }
            } catch (e: Exception) {
                Log.e("ProceduralWallpaper", "Error during frame rendering", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("ProceduralWallpaper", "Error unlocking canvas", e)
                    }
                }
            }
        }
    }
}
