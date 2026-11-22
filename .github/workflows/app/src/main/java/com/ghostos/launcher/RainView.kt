package com.ghostos.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.PowerManager
import android.view.View

class RainView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyphs = "0123456789ABCDEFXYZ<>{}[]#\$%=+*/|".toCharArray()
    private val rnd = java.util.Random()
    private var cell = 0f
    private var cellW = 0f
    private var cols = 0
    private var rows = 0
    private var head = FloatArray(0)
    private var speed = FloatArray(0)
    private var trail = IntArray(0)
    private var tick = 0
    private var running = false
    private var staticMode = false

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            step()
            invalidate()
            postDelayed(this, 50)
        }
    }

    init {
        paint.typeface = Typeface.MONOSPACE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cell = context.dp(14).toFloat()
        cellW = cell * 0.7f
        paint.textSize = cell
        cols = (w / cellW).toInt()
        rows = (h / cell).toInt() + 1
        head = FloatArray(cols) { -rnd.nextInt(rows.coerceAtLeast(1)).toFloat() }
        speed = FloatArray(cols) { 0.2f + rnd.nextFloat() * 0.5f }
        trail = IntArray(cols) { 8 + rnd.nextInt(14) }
        if (staticMode) {
            repeat(60) { step() }
            invalidate()
        }
    }

    private fun step() {
        tick++
        for (c in 0 until cols) {
            head[c] += speed[c]
            if (head[c] - trail[c] > rows) {
                head[c] = -rnd.nextInt(10).toFloat()
                speed[c] = 0.2f + rnd.nextFloat() * 0.5f
                trail[c] = 8 + rnd.nextInt(14)
            }
        }
    }

    private fun powerSave(): Boolean {
        return try {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
        } catch (e: Exception) {
            false
        }
    }

    fun start() {
        if (running) return
        if (powerSave()) {
            // battery saver: draw one static frame, no animation
            staticMode = true
            if (cols > 0) {
                repeat(60) { step() }
            }
            invalidate()
        } else {
            staticMode = false
            running = true
            post(frame)
        }
    }

    fun stop() {
        running = false
        removeCallbacks(frame)
    }

    override fun onDraw(canvas: Canvas) {
        if (cols == 0) return
        for (c in 0 until cols) {
            val x = c * cellW
            val t = trail[c]
            for (k in 0 until t) {
                val row = head[c].toInt() - k
                if (row < 0 || row >= rows) continue
                val idx = (c * 31 + row * 17 + tick / 5 + k * 3) % glyphs.size
                if (k == 0) {
                    paint.color = Palette.bright
                    paint.alpha = 210
                } else {
                    paint.color = Palette.fg
                    paint.alpha = (150f * (1f - k.toFloat() / t)).toInt().coerceAtLeast(14)
                }
                canvas.drawText(glyphs, idx, 1, x, (row + 1) * cell, paint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
